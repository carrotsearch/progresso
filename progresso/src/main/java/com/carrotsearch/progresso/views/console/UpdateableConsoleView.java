package com.carrotsearch.progresso.views.console;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import com.carrotsearch.progresso.ProgressView;
import com.carrotsearch.progresso.Task;
import com.carrotsearch.progresso.Task.Status;
import com.carrotsearch.progresso.Tracker;
import com.carrotsearch.progresso.util.ColumnCounter;
import com.carrotsearch.progresso.util.LineFormatter;
import com.carrotsearch.progresso.util.LineFormatter.Alignment;

/**
 * A progress view which assumes the terminal supports "backtracking" (carriage return) 
 * properly.
 */
public class UpdateableConsoleView implements ProgressView {
  private static final long DEFAULT_UPDATE_INTERVAL = TimeUnit.SECONDS.toMillis(1); 

  private final ArrayList<? extends TrackerFormatter> formatters;
  private final ConsoleWriter consoleWriter;
  private long nextUpdate = 0L;

  private final TaskStatusRecovery statusUpdater = new TaskStatusRecovery();

  private Task<?> activeTask = null;
  private long modHash = 0L;

  private final ArrayDeque<Task<?>> doneTasks = new ArrayDeque<>();
  private final ArrayDeque<Task<?>> startedTasks = new ArrayDeque<>();

  private final Set<Task<?>> topTasks;

  public UpdateableConsoleView(ConsoleWriter out,
                               Collection<Task<?>> topTasks, 
                               List<? extends TrackerFormatter> formatters) {
    this.topTasks = new HashSet<>(topTasks);
    this.formatters = new ArrayList<>(formatters);
    this.consoleWriter = out;
  }

  public UpdateableConsoleView(ConsoleWriter out, Collection<Task<?>> topTasks) {
    this(out, topTasks, defaultFormatters());
  }

  public UpdateableConsoleView(ConsoleWriter out) {
    this(out, Collections.emptyList());
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
          modHash = ~activeTask.getTracker().modHash();
        }
      } else {
        i.remove();
      }
    }
  }

  private void emitCompleted(Task<?> t) {
    try {
      // Skipped tasks don't emit anything.
      if (t.getStatus() == Status.SKIPPED) {
        consoleWriter.updateLine("");
        return;
      }

      // Proceed to a new line on top-level tasks.
      if (topTasks.isEmpty() || topTasks.contains(t)) {
        consoleWriter.printLine(formatView(t));
      } else {
        consoleWriter.updateLine(formatView(t));
      }
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  private void taskUpdate(Task<?> t) {
    // When there's no change and we haven't reached
    // the update interval, skip the update.
    long modHash = t.getTracker().modHash();
    if (this.modHash == modHash &&
        now() < nextUpdate) {
      return;
    }

    this.modHash = modHash;
    nextUpdate = now() + DEFAULT_UPDATE_INTERVAL;

    try {
      consoleWriter.updateLine(formatView(t));
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  private String formatView(Task<?> task) {
    final Status taskStatus = task.getStatus();
    if (taskStatus != Status.STARTED &&
        taskStatus != Status.DONE &&
        taskStatus != Status.SKIPPED) {
      throw new AssertionError();
    }

    final ColumnCounter cc = ColumnCounter.DEFAULT;
    final LineFormatter lf = new LineFormatter(cc);

    if (!topTasks.isEmpty()) {
      String top = Long.toString(topTasks.size());
      long current = topTasks.stream()
          .filter((t) -> (t == task || task.isChildOf(t) || t.isDone()))
          .count();
      int width = 2 * cc.columns(top) + 1 + 1;
      lf.cell(width, width, Alignment.RIGHT, current + "/" + top + " "); 
    }

    // Leave space for cursor. Certain terminals make an automatic next line 
    // feed if cursor doesn't fit.
    int lineWidth = consoleWriter.lineWidth() - 1;

    final Tracker tracker = task.getTracker();
    for (TrackerFormatter formatter : formatters) {
      if (formatter.supports(lineWidth, tracker)) {
        formatter.format(lf, lineWidth, task, tracker);
        break;
      }
    }

    return lf.format(lineWidth);
  }

  private long now() {
    return System.currentTimeMillis();
  }

  public static List<AbstractTrackerFormatter<?>> defaultFormatters() {
    return Arrays.asList(
        new UpdateablePathTrackerFormatter(),
        new UpdateableByteRangeTrackerFormatter(),
        new UpdateableCompletedRatioTrackerFormatter(),
        new UpdateableLongTrackerFormatter(),
        new UpdateableGenericTrackerFormatter());
  }  
}