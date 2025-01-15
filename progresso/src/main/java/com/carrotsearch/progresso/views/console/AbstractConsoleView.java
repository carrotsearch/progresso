package com.carrotsearch.progresso.views.console;

import com.carrotsearch.progresso.ProgressView;
import com.carrotsearch.progresso.Task;
import com.carrotsearch.progresso.Task.Status;
import com.carrotsearch.progresso.Tracker;
import com.carrotsearch.progresso.util.ColumnCounter;
import com.carrotsearch.progresso.util.LineFormatter;
import com.carrotsearch.progresso.util.LineFormatter.Alignment;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/** A very basic progress view. */
public abstract class AbstractConsoleView implements ProgressView {
  protected static final String LF = System.lineSeparator();

  private final ArrayList<? extends TrackerFormatter> formatters;

  private final TaskStatusRecovery statusUpdater = new TaskStatusRecovery();
  private final ArrayDeque<Task<?>> doneTasks = new ArrayDeque<>();
  private final ArrayDeque<Task<?>> startedTasks = new ArrayDeque<>();

  private final TrackerRateCalculator trackerRateCalculator = new TrackerRateCalculator();

  protected Task<?> activeTask;
  protected final Set<Task<?>> topTasks;

  protected AbstractConsoleView(
      Collection<? extends Task<?>> topTasks, List<? extends TrackerFormatter> formatters) {
    this.topTasks = new HashSet<>(Objects.requireNonNull(topTasks));
    this.formatters = new ArrayList<>(Objects.requireNonNull(formatters));
  }

  @Override
  public final void update(Set<Task<?>> tasks) {
    statusUpdater.update(
        tasks,
        (t) -> {
          startedTasks.addLast(t);
          formatters.forEach(
              fmt -> {
                trackerRateCalculator.update(t.getTracker());
                fmt.taskStarted(t);
              });
        },
        doneTasks::addLast);

    // If active task has completed, finalize its progress.
    if (activeTask != null && doneTasks.remove(activeTask)) {
      taskCompletedUpdate(activeTask);
      setActiveTask(null);
    }

    // If no active task is running, flush any pending completed tasks.
    if (activeTask == null) {
      while (!doneTasks.isEmpty()) {
        taskCompletedUpdate(doneTasks.removeFirst());
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
    for (Iterator<Task<?>> i = startedTasks.iterator(); i.hasNext(); ) {
      Task<?> t = i.next();

      if (t.getStatus() == Status.STARTED) {
        if (activeTask != t && (activeTask == null || t.isChildOf(activeTask))) {
          setActiveTask(t);
        }
      } else {
        i.remove();
      }
    }
  }

  protected void setActiveTask(Task<?> task) {
    activeTask = task;
  }

  protected abstract void taskCompletedUpdate(Task<?> t);

  protected abstract void taskUpdate(Task<?> t);

  protected final String formatStatusLine(Task<?> task, int lineWidth) {
    final Status taskStatus = task.getStatus();
    if (taskStatus != Status.STARTED && taskStatus != Status.DONE && taskStatus != Status.SKIPPED) {
      throw new AssertionError("Invalid task status: " + taskStatus);
    }

    final ColumnCounter cc = ColumnCounter.DEFAULT;
    final LineFormatter lf = new LineFormatter(cc);

    if (!topTasks.isEmpty()) {
      String top = Long.toString(topTasks.size());
      long current =
          topTasks.stream().filter((t) -> (t == task || task.isChildOf(t) || t.isDone())).count();
      int width = 2 * lf.getColumnCounter().columns(top) + 1 + 1;
      lf.cell(width, width, Alignment.RIGHT, current + "/" + top + " ");
    }

    // Add task name.
    ViewHelpers.appendTaskName(lf, task);

    Optional<TrackerRateCalculator.TrackerStats> trackerStats;
    if (task.hasTracker()) {
      Tracker tracker = task.getTracker();
      for (TrackerFormatter formatter : formatters) {
        if (formatter.supports(lineWidth, tracker)) {
          formatter.format(lf, lineWidth, task, tracker);
          break;
        }
      }
      trackerStats = Optional.of(trackerRateCalculator.update(tracker));
    } else {
      trackerStats = Optional.empty();
    }

    // Add status.
    switch (taskStatus) {
      case DONE:
        lf.cell(" done");
        break;
      case SKIPPED:
        lf.cell(" skipped");
        break;
      default:
        ViewHelpers.appendTime(lf, task, trackerStats);
        break;
    }

    return lf.format(lineWidth);
  }

  protected final long nowMillis() {
    return TimeUnit.NANOSECONDS.toMillis(System.nanoTime());
  }

  public static List<AbstractTrackerFormatter<?>> defaultFormatters() {
    return Arrays.asList(
        new UpdateablePathTrackerFormatter(),
        new UpdateableCompletedRatioTrackerFormatter(),
        new UpdateableLongTrackerFormatter(),
        new UpdateableGenericTrackerFormatter());
  }
}
