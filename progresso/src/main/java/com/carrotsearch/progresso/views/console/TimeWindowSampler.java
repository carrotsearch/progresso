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

  private final ArrayDeque<Sample> samples = new ArrayDeque<>();
  private final long timeWindow = TimeUnit.SECONDS.toMillis(5);
  private final long MIN_SAMPLE_INTERVAL = 250;

  public synchronized WindowStats tick(long tsNow, T value) {
    long forgetLine = tsNow - timeWindow;
    while (!samples.isEmpty() && samples.peekFirst().time < forgetLine) {
      samples.removeFirst();
    }

    // Prevent rapid sample bursts, replace the last sample only at most
    // each window of sample interval.
    if (samples.size() > 1 &&
        samples.peekLast().time + MIN_SAMPLE_INTERVAL >= tsNow) {
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
