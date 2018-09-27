package com.carrotsearch.progresso.views.console;

import java.util.ArrayDeque;
import java.util.concurrent.TimeUnit;

final class RateCalculator {
  private final static class Sample {
    final long time;
    final long value;
    
    public Sample(long time, long value) {
      this.time = time;
      this.value = value;
    }
    
    @Override
    public String toString() {
      return time + ":" + value;
    }
  }

  private final ArrayDeque<Sample> samples = new ArrayDeque<>();
  private final long timeWindow = TimeUnit.SECONDS.toMillis(5);
  private final long MIN_SAMPLE_INTERVAL = 250;

  public synchronized double tick(long now, long at) {
    long forgetLine = now - timeWindow;
    while (!samples.isEmpty() && samples.peekFirst().time < forgetLine) {
      samples.removeFirst();
    }

    if (samples.size() > 1 && 
        samples.peekLast().time + MIN_SAMPLE_INTERVAL >= now) {
      // Prevent rapid sample bursts, replace the last sample.
      samples.removeLast();
    }
    samples.addLast(new Sample(now, at));

    if (samples.size() >= 2) {
      Sample first = samples.peekFirst();
      Sample last = samples.peekLast();
      long delta = last.time - first.time;

      if (first.value > last.value) {
        throw new RuntimeException(first + " " + last);
      }

      if (delta > 0) {
        return (last.value - first.value) * 1000 / delta;
      }
    }

    return 0L;
  }
}
