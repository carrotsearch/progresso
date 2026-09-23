package com.carrotsearch.progresso.views.console;

import java.util.ArrayDeque;
import java.util.concurrent.TimeUnit;

public final class TimeWindowSampler<T> {
  public final class Sample {
    final long time;
    final T value;

    public Sample(long time, T value) {
      this.time = time;
      this.value = value;
    }

    @Override
    public String toString() {
      return time + ":" + value;
    }
  }

  public final class WindowStats {
    public final Sample first;
    public final Sample last;

    public WindowStats(Sample first, Sample last) {
      this.first = first;
      this.last = last;
    }
  }

  /** The default sampling window. */
  public static final long DEFAULT_TIME_WINDOW_MILLIS = TimeUnit.SECONDS.toMillis(5);

  private final ArrayDeque<Sample> samples = new ArrayDeque<>();
  private final long timeWindow;
  private final long MIN_SAMPLE_INTERVAL = 250;

  public TimeWindowSampler() {
    this(DEFAULT_TIME_WINDOW_MILLIS);
  }

  /**
   * @param timeWindowMillis Length of the sampling window in milliseconds. Zero means an unlimited
   *     window: only the first and the most recent sample are retained.
   */
  public TimeWindowSampler(long timeWindowMillis) {
    if (timeWindowMillis < 0) {
      throw new IllegalArgumentException("Time window must not be negative: " + timeWindowMillis);
    }
    this.timeWindow = timeWindowMillis;
  }

  public synchronized WindowStats tick(long tsNow, T value) {
    if (timeWindow > 0) {
      long forgetLine = tsNow - timeWindow;
      while (!samples.isEmpty() && samples.peekFirst().time < forgetLine) {
        samples.removeFirst();
      }
    } else if (samples.size() > 2) {
      // Unlimited window: keep the first sample and the most recent one only.
      Sample first = samples.pollFirst();
      Sample last = samples.pollLast();
      samples.clear();
      samples.addLast(first);
      samples.addLast(last);
    }

    // Prevent rapid sample bursts, replace the last sample only at most
    // each window of sample interval.
    if (samples.size() > 1 && samples.peekLast().time + MIN_SAMPLE_INTERVAL >= tsNow) {
      samples.removeLast();
    }
    samples.addLast(new Sample(tsNow, value));

    if (samples.size() >= 2) {
      Sample first = samples.peekFirst();
      Sample last = samples.peekLast();
      return new WindowStats(first, last);
    } else {
      return null;
    }
  }
}
