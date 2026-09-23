package com.carrotsearch.progresso.views.console;

import com.carrotsearch.progresso.CompletedRatio;
import com.carrotsearch.progresso.LongTracker;
import com.carrotsearch.progresso.Task;
import com.carrotsearch.progresso.Tracker;
import java.time.Duration;
import java.util.IdentityHashMap;
import java.util.concurrent.TimeUnit;

class TrackerRateCalculator {
  private static class TrackerState {
    long at;
    double completedRatio;

    public TrackerState(Tracker tracker) {
      if (tracker instanceof LongTracker) {
        this.at = ((LongTracker) tracker).at();
      }
      if (tracker instanceof CompletedRatio) {
        this.completedRatio = ((CompletedRatio) tracker).completedRatio();
      }
    }
  }

  static class TrackerStats {
    private Double completionEta;
    private Long itemsPerSec;

    public TrackerStats(Long itemsPerSec, Double completionEta) {
      this.itemsPerSec = itemsPerSec;
      this.completionEta = completionEta;
    }

    public boolean hasItemsPerSec() {
      return itemsPerSec != null;
    }

    public long itemsPerSec() {
      return itemsPerSec;
    }

    public boolean hasCompletionEta() {
      return completionEta != null;
    }

    public double completionEta() {
      return completionEta;
    }
  }

  /** Sampling window for ETA and throughput estimation, see {@link #etaWindowMillis()}. */
  private static final long ETA_WINDOW_MILLIS = etaWindowMillis();

  private final IdentityHashMap<Tracker, TimeWindowSampler<TrackerState>> samplers =
      new IdentityHashMap<>();

  /**
   * @return The sampling window for the given task: the task's own setting ({@link
   *     Task#etaWindow(Duration)}) if present, otherwise the process-wide default.
   */
  static long etaWindowMillis(Task<?> task) {
    Duration window = task.etaWindow();
    return window != null ? window.toMillis() : ETA_WINDOW_MILLIS;
  }

  /**
   * @return The sampling window in milliseconds from {@link ConsoleAware#ETA_WINDOW_PROPERTY}, or
   *     {@link TimeWindowSampler#DEFAULT_TIME_WINDOW_MILLIS} if the property is not set.
   */
  static long etaWindowMillis() {
    String value = System.getProperty(ConsoleAware.ETA_WINDOW_PROPERTY);
    if (value == null) {
      return TimeWindowSampler.DEFAULT_TIME_WINDOW_MILLIS;
    }
    long millis = Long.parseLong(value.trim());
    if (millis < 0) {
      throw new IllegalArgumentException(
          ConsoleAware.ETA_WINDOW_PROPERTY + " must not be negative: " + value);
    }
    return millis;
  }

  public TrackerStats update(Tracker tracker) {
    if (tracker instanceof LongTracker || tracker instanceof CompletedRatio) {
      TimeWindowSampler<TrackerState> sampler =
          samplers.computeIfAbsent(
              tracker, (key) -> new TimeWindowSampler<>(etaWindowMillis(key.task())));
      TimeWindowSampler<TrackerState>.WindowStats windowStats =
          sampler.tick(TimeUnit.NANOSECONDS.toMillis(System.nanoTime()), new TrackerState(tracker));

      if (windowStats != null) {
        Long itemsPerSec =
            (tracker instanceof LongTracker) ? computeItemsPerSec(windowStats) : null;
        Double completionEta =
            (tracker instanceof CompletedRatio) ? computeCompletionEta(windowStats) : null;
        return new TrackerStats(itemsPerSec, completionEta);
      }
    }
    return new TrackerStats(null, null);
  }

  private Long computeItemsPerSec(TimeWindowSampler<TrackerState>.WindowStats windowStats) {
    TimeWindowSampler<TrackerState>.Sample first = windowStats.first;
    TimeWindowSampler<TrackerState>.Sample last = windowStats.last;

    if (first.value.at > last.value.at) {
      throw new RuntimeException(
          "Non-monotonic at() in samples: " + first.value.at + " " + last.value.at);
    }

    long delta = last.time - first.time;
    if (delta > 0) {
      return (last.value.at - first.value.at) * 1000 / delta;
    } else {
      return null;
    }
  }

  private Double computeCompletionEta(TimeWindowSampler<TrackerState>.WindowStats windowStats) {
    TimeWindowSampler<TrackerState>.Sample first = windowStats.first;
    TimeWindowSampler<TrackerState>.Sample last = windowStats.last;

    if (first.value.completedRatio > last.value.completedRatio) {
      throw new RuntimeException(
          "Non-monotonic completion ratio in samples: "
              + first.value.completedRatio
              + " "
              + last.value.completedRatio);
    }

    long delta = last.time - first.time;
    double increment = last.value.completedRatio - first.value.completedRatio;
    double remaining = 1 - last.value.completedRatio;
    if (delta > 0 && increment > 0 && remaining > 0) {
      return delta * remaining / increment;
    } else {
      return null;
    }
  }
}
