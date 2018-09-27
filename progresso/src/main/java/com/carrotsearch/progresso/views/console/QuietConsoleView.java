package com.carrotsearch.progresso.views.console;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.carrotsearch.progresso.ProgressView;
import com.carrotsearch.progresso.Task;
import com.carrotsearch.progresso.Task.Status;
import com.carrotsearch.progresso.Tracker;
import com.carrotsearch.progresso.util.LineFormatter;
import com.carrotsearch.progresso.util.LineFormatter.Alignment;

/**
 * A "quiet" console view that only reports completed top-level tasks.
 */
public class QuietConsoleView implements ProgressView {
  private final static String LF = System.getProperty("line.separator");

  private final ArrayList<? extends TrackerFormatter> formatters;

  private final TaskStatusRecovery statusUpdater = new TaskStatusRecovery();
  private final ArrayDeque<Task<?>> doneTasks = new ArrayDeque<>();
  private final ArrayDeque<Task<?>> startedTasks = new ArrayDeque<>();

  private final Writer out;
  private final int lineWidth;

  private final Set<Task<?>> topTasks;
  
  public QuietConsoleView(ConsoleWriter out) {
    this(out, Collections.emptyList());
  }

  public QuietConsoleView(ConsoleWriter out, Collection<Task<?>> topTasks) {
    this(out, out.lineWidth() - 1, topTasks);
  }

  public QuietConsoleView(Writer out,
                          int lineWidth,
                          Collection<Task<?>> topTasks) {
    this(out, lineWidth, topTasks, defaultFormatters());
  }

  public QuietConsoleView(Writer out,
                          int lineWidth,
                          Collection<Task<?>> topTasks,
                          List<? extends TrackerFormatter> formatters) {
    this.lineWidth = lineWidth;
    this.topTasks = new HashSet<>(topTasks);
    this.out = out;
    this.formatters = new ArrayList<>(formatters);
  }

  @Override
  public void update(Set<Task<?>> tasks) {
    statusUpdater.update(tasks,
        (t) -> { startedTasks.addLast(t); formatters.forEach(fmt -> fmt.taskStarted(t)); },
        (t) -> doneTasks.addLast(t));

    // Flush any pending completed tasks.
    while (!doneTasks.isEmpty()) {
      Task<?> task = doneTasks.removeFirst();
      if (topTasks.isEmpty() || topTasks.contains(task)) {
        emitCompleted(task);
      }
    }
  }

  private void emitCompleted(Task<?> t) {
    // Skipped tasks don't emit anything.
    if (t.getStatus() == Status.SKIPPED) {
      return;
    }

    out(updateView(t));
  }

  private void out(String s) {
    try {
      out.write(s);
      out.flush();
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  private static EnumSet<Status> UPDATE_ALLOWED = EnumSet.of(Status.DONE);

  private String updateView(Task<?> task) {
    final Status taskStatus = task.getStatus();
    if (!UPDATE_ALLOWED.contains(taskStatus)) {
      throw new AssertionError("Invalid task status: " + taskStatus);
    }

    final Tracker tracker = task.getTracker();
    final LineFormatter lf = new LineFormatter();

    if (!topTasks.isEmpty()) {
      String top = Long.toString(topTasks.size());
      long current = topTasks.stream()
          .filter((t) -> (t == task || task.isChildOf(t) || t.isDone()))
          .count();
      int width = 2 * lf.columns(top) + 1 + 1;
      lf.cell(width, width, Alignment.RIGHT, current + "/" + top + " "); 
    }

    for (TrackerFormatter formatter : formatters) {
      if (formatter.supports(lineWidth, tracker)) {
        formatter.format(lf, lineWidth, task, tracker);
        break;
      }
    }

    return lf.format(lineWidth) + LF;
  }

  public static List<AbstractTrackerFormatter<?>> defaultFormatters() {
    return Arrays.asList(
        new UpdateablePathTrackerFormatter(),
        new UpdateableCompletedRatioTrackerFormatter(),
        new UpdateableLongTrackerFormatter(),
        new UpdateableGenericTrackerFormatter());
  }  
}