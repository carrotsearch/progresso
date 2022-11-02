package com.carrotsearch.progresso;

import com.carrotsearch.progresso.util.Units;
import java.time.Instant;
import java.util.Locale;

/** An immutable snapshot of task properties. */
public class TaskData {
  private final Task<?> task;
  private final Tracker tracker;
  private final long startTime;
  private final long elapsed;
  private final Task.Status status;
  private final Instant startInstant;

  public TaskData(Task<?> task) {
    this.task = task;
    this.tracker = task.hasTracker() ? task.getTracker() : null;
    this.status = task.getStatus();
    if (this.tracker != null) {
      this.startTime = tracker.startTime();
      this.startInstant = tracker.startInstant();
      this.elapsed = tracker.elapsedMillis();
    } else {
      this.startTime = 0;
      this.startInstant = null;
      this.elapsed = 0;
    }
  }

  public Task<?> getTask() {
    return task;
  }

  public Tracker getTracker() {
    return tracker;
  }

  public boolean hasTracker() {
    return tracker != null;
  }

  public Instant startInstant() {
    assert hasTracker();
    return startInstant;
  }

  public long startTimeMillis() {
    assert hasTracker();
    return startTime;
  }

  public long elapsedMillis() {
    assert hasTracker();
    return elapsed;
  }

  public boolean isDone() {
    return status == Task.Status.DONE || status == Task.Status.SKIPPED;
  }

  public String taskName() {
    return task.hasName() ? task.getName() : "<unnamed>";
  }

  public String taskTime() {
    if (hasTracker()) {
      String val = Units.DURATION.format(elapsedMillis());
      return isDone() ? val : "⇥ " + val;
    } else {
      String val;
      switch (status) {
        case NEW:
          val = "Never started";
          break;
        case SKIPPED:
          val = "Skipped";
          break;
        default:
          val = status.name();
          break;
      }
      return "[" + val + "]";
    }
  }

  public String taskTimeFraction(long total) {
    if (hasTracker() && total > 0) {
      String val;
      long elapsed = elapsedMillis();
      double percent = 100.0d * elapsed / total;
      if (percent > 100.0d) {
        val = ">100%?";
      } else {
        val = String.format(Locale.ROOT, "%.1f%%", percent);
      }
      return isDone() ? val : "⇥ " + val;
    } else {
      return "";
    }
  }

  public String taskTimeT0(long t0) {
    if (hasTracker()) {
      return Units.DURATION_COMPACT.format(startTimeMillis() - t0);
    } else {
      return "";
    }
  }
}
