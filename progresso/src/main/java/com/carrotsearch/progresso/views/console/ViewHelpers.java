package com.carrotsearch.progresso.views.console;

import com.carrotsearch.progresso.CompletedRatio;
import com.carrotsearch.progresso.Task;
import com.carrotsearch.progresso.Tracker;
import com.carrotsearch.progresso.util.LineFormatter;
import com.carrotsearch.progresso.util.Units;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

public final class ViewHelpers {
  /**
   * This is the minimum width of the time column; it's typically the last column and we want it
   * aligned properly.
   */
  private static final int TIME_COL_WIDTH = 9;

  private static final long ETA_ESTIMATE_DELAY = TimeUnit.SECONDS.toMillis(2);

  public static void appendTaskName(LineFormatter lf, final Task<?> task) {
    StringBuilder name = new StringBuilder();
    for (Task<?> t = task; t != null; t = t.getParent()) {
      if (t.hasName()) {
        if (name.length() > 0) {
          name.insert(0, " > ");
        }
        name.insert(0, t.getName());
      }
    }

    if (name.length() == 0) {
      name.append("(unnamed task)");
    }

    lf.cell(
        10,
        Integer.MAX_VALUE,
        LineFormatter.Alignment.LEFT,
        LineFormatter.Trim.RIGHT,
        LineFormatter.PRIORITY_HIGH,
        name.toString());
    lf.cell(" ");
  }

  public static void appendTime(
      LineFormatter lf, Task<?> task, Optional<TrackerRateCalculator.TrackerStats> trackerStats) {

    String value = "";

    if (task.hasTracker()) {
      Tracker tracker = task.getTracker();
      final long duration = tracker.elapsedMillis();
      if (task.isDone()) {
        value = Units.DURATION_COMPACT.format(duration);
      } else {
        // Don't display anything for the initial small period.
        if (duration > ETA_ESTIMATE_DELAY) {
          value = Units.DURATION_COMPACT.format(tracker.elapsedMillis());

          if (trackerStats.isPresent()) {
            TrackerRateCalculator.TrackerStats stats = trackerStats.get();
            if (stats.hasCompletionEta()) {
              long etaMillis = Math.round(stats.completionEta());
              value = "~" + Units.DURATION_COMPACT.format(etaMillis);
            }
          } else if (tracker instanceof CompletedRatio) {
            final CompletedRatio ratio = (CompletedRatio) tracker;
            final double current = Units.clamp(ratio.completedRatio(), 0, 1);

            if (current > 0) {
              long etaMillis = Math.round((duration * (1 - current)) / (current - 0));
              value = "~" + Units.DURATION_COMPACT.format(etaMillis);
            }
          }
        }
      }
    } else {
      // no tracker, ignore.
    }

    lf.cell(
        TIME_COL_WIDTH,
        TIME_COL_WIDTH,
        LineFormatter.Alignment.RIGHT,
        LineFormatter.Trim.RIGHT,
        LineFormatter.PRIORITY_DEFAULT,
        value);
  }
}
