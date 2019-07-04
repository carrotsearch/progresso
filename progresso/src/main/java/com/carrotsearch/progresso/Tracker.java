package com.carrotsearch.progresso;

import java.io.Closeable;
import java.time.Clock;
import java.util.Objects;

import com.carrotsearch.progresso.Task.Status;

public abstract class Tracker implements Closeable {
  final static Clock CLOCK = Clock.systemDefaultZone();
  final Task<?> task;

  private final long startTime = CLOCK.millis();
  private Long endTime;

  public Tracker(Task<?> task) {
    this.task = Objects.requireNonNull(task);
  }

  public final void close() {
    if (endTime == null) {
      endTime = CLOCK.millis();
      task.setStatus(Status.DONE);
    }
    close0();
  }

  protected void close0() {
  }

  public long startTime() {
    return startTime;
  }
  
  public long elapsedMillis() {
    if (endTime == null) {
      return CLOCK.millis() - startTime;
    } else {
      return endTime - startTime;
    }
  }

  /**
   * Trackers must indicate changed state via alternating 
   * modification counter/ hash.  
   */
  public abstract long modHash();

  public Task<?> task() {
    return task;
  }

  public final Tracker attribute(String description, String argsFormat, Object... args) {
    task().attribute(description, argsFormat, args);
    return this;
  }

  protected final void ensureOpen() {
    if (endTime != null) {
      throw new RuntimeException("Tracker closed: " + task());
    }
  }
}