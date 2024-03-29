package com.carrotsearch.progresso;

import com.carrotsearch.progresso.Task.Status;
import java.io.Closeable;
import java.time.Instant;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

public abstract class Tracker implements Closeable {
  static final Supplier<Instant> CLOCK = Instant::now;
  final Task<?> task;

  private final Instant startTime = CLOCK.get();
  private Instant endTime;

  // Use monotonic clock times for duration.
  private final long startNanos = System.nanoTime();
  private long endNanos;

  public Tracker(Task<?> task) {
    this.task = Objects.requireNonNull(task);
  }

  public final void close() {
    if (endTime == null) {
      endNanos = System.nanoTime();
      endTime = CLOCK.get();
      task.setStatus(Status.DONE);
    }
    close0();
  }

  protected void close0() {}

  public long startTime() {
    return startTime.toEpochMilli();
  }

  public long elapsedMillis() {
    long endOrNow = (endTime != null ? this.endNanos : System.nanoTime());
    return TimeUnit.NANOSECONDS.toMillis(endOrNow - startNanos);
  }

  public Instant startInstant() {
    return startTime;
  }

  /** Trackers must indicate changed state via alternating modification counter/ hash. */
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
