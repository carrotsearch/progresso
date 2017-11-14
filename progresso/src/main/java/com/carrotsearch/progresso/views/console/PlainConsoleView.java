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
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import com.carrotsearch.progresso.ProgressView;
import com.carrotsearch.progresso.Task;
import com.carrotsearch.progresso.Task.Status;
import com.carrotsearch.progresso.Tracker;
import com.carrotsearch.progresso.util.LineFormatter;
import com.carrotsearch.progresso.util.LineFormatter.Alignment;

/**
 * A very basic progress view.
 */
public class PlainConsoleView implements ProgressView {
  private final static String LF = System.getProperty("line.separator");

  private final ArrayList<? extends TrackerFormatter> formatters;

  private static final long DELAY_INITIAL  = TimeUnit.SECONDS.toMillis(3); 
  private static final long DELAY_PROGRESS = TimeUnit.SECONDS.toMillis(2); 

  private final TaskStatusRecovery statusUpdater = new TaskStatusRecovery();
  private final ArrayDeque<Task<?>> doneTasks = new ArrayDeque<>();
  private final ArrayDeque<Task<?>> startedTasks = new ArrayDeque<>();

  private final Writer out;
  private final int lineWidth;

  private Task<?> activeTask = null;

  private long nextUpdate = 0L;

  private final Set<Task<?>> topTasks;

  public PlainConsoleView(ConsoleWriter out) {
    this(out, Collections.emptyList());
  }
  
  public PlainConsoleView(ConsoleWriter out,
      Collection<Task<?>> topTasks) {
    this(out, out.lineWidth(), topTasks);
  }

  public PlainConsoleView(Writer out,
                          int lineWidth,
                          Collection<Task<?>> topTasks) {
    this(out, lineWidth, topTasks, defaultFormatters());
  }

  public PlainConsoleView(Writer out,
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
        (t) -> startedTasks.addLast(t),
        (t) -> doneTasks.addLast(t));

    // If active task has completed, finalize its progress.
    if (activeTask != null && doneTasks.remove(activeTask)) {
      emitCompleted(activeTask);
      activeTask = null;
      nextUpdate = 0L;
    }

    // If no active task is running, flush any pending completed tasks.
    if (activeTask == null) {
      while (!doneTasks.isEmpty()) {
        emitCompleted(doneTasks.removeFirst());
      }
    }

    // Pick or update the active task.
    pickNewActive();

    // Update the active task, if any.
    if (activeTask != null) {
      taskUpdate(activeTask);
    }
  }

  private void pickNewActive() {
    for (Iterator<Task<?>> i = startedTasks.iterator(); i.hasNext();) {
      Task<?> t = i.next();
      
      if (t.getStatus() == Status.STARTED) {
        if (activeTask != t && (activeTask == null || t.isChildOf(activeTask))) {
          activeTask = t;
          nextUpdate = now() + DELAY_INITIAL;
        }
      } else {
        i.remove();
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

  private void taskUpdate(Task<?> t) {
    if (now() < nextUpdate) {
      return;
    }

    nextUpdate = now() + DELAY_PROGRESS;
    out(updateView(t));
  }
  
  private static EnumSet<Status> UPDATE_ALLOWED = EnumSet.of(
      Status.STARTED, Status.DONE);

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

  private long now() {
    return System.currentTimeMillis();
  }
  
  private static List<AbstractTrackerFormatter<?>> defaultFormatters() {
    return Arrays.asList(
        new UpdateablePathTrackerFormatter(),
        new UpdateableByteRangeTrackerFormatter(),
        new UpdateableByteTrackerFormatter(),
        new UpdateableCompletedRatioTrackerFormatter(),
        new UpdateableLongTrackerFormatter(),
        new UpdateableGenericTrackerFormatter());
  }  
}