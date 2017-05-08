package com.carrotsearch.progresso.views.console;

import java.util.concurrent.TimeUnit;

import com.carrotsearch.progresso.CompletedRatio;
import com.carrotsearch.progresso.RangeTracker;
import com.carrotsearch.progresso.Task;
import com.carrotsearch.progresso.Tracker;
import com.carrotsearch.progresso.util.LineFormatter;
import com.carrotsearch.progresso.util.LineFormatter.Alignment;
import com.carrotsearch.progresso.util.LineFormatter.Trim;
import com.carrotsearch.progresso.util.MinMax;
import com.carrotsearch.progresso.util.UnitFormatter;

public abstract class AbstractTrackerFormatter<T> implements TrackerFormatter {
  /**
   * This is the minimum width of the time column; it's typically the last column
   * and we want it aligned properly.
   */
  private static final int TIME_COL_WIDTH = 9;
  private static final long ETA_ESTIMATE_DELAY = TimeUnit.SECONDS.toMillis(2);

  /* */
  @Override
  public abstract boolean supports(int lineWidth, Tracker tracker);

  /* */
  @SuppressWarnings("unchecked")
  @Override
  public final void format(LineFormatter lf, int lineWidth, Task<?> task, Tracker tracker) {
    doFormat(lf, lineWidth, task, (T) tracker);
  }

  protected abstract void doFormat(LineFormatter lf, int lineWidth, Task<?> task, T tracker);

  protected void appendTaskName(LineFormatter lf, final Task<?> task) {
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

    lf.cell(10, Integer.MAX_VALUE, Alignment.LEFT, Trim.RIGHT, LineFormatter.PRIORITY_HIGH, name.toString());
    lf.cell(" ");
  }
  
  protected void appendOptional(LineFormatter lf, String value) {
    int cols = lf.columns(value);
    lf.cell(cols, cols, Alignment.LEFT, Trim.LEFT, LineFormatter.PRIORITY_OPTIONAL, value);
  }

  protected void appendTime(LineFormatter lf, Task<?> task, Tracker tracker) {
    final long duration = tracker.elapsedMillis();

    String value = "";
    
    if (task.isDone() && task.hasTracker()) {
      value = UnitFormatter.DURATION_COMPACT.format(tracker.elapsedMillis());
    } else {
      // Don't display anything for the initial small period.
      if (duration > ETA_ESTIMATE_DELAY) {
        value = UnitFormatter.DURATION_COMPACT.format(tracker.elapsedMillis());

        if (tracker instanceof RangeTracker) {
          final RangeTracker rangeTracker = (RangeTracker) tracker;
          final long current = rangeTracker.at();
          final MinMax mm = rangeTracker.minMax;

          if (current > mm.minInclusive && current < mm.maxExclusive) {
            long etaMillis = (long) (((double) (mm.maxExclusive - mm.minInclusive)) * duration / (current - mm.minInclusive) - duration);
            value = "~" + UnitFormatter.DURATION_COMPACT.format(etaMillis);
          }
        }      
      }
    }

    lf.cell(TIME_COL_WIDTH, TIME_COL_WIDTH, Alignment.RIGHT, Trim.RIGHT, LineFormatter.PRIORITY_DEFAULT, value);
  }

  protected static String dots(int lineWidth, CompletedRatio tracker) {
    final int progressBarWidth;
    if (lineWidth <= 80) {
      progressBarWidth = 4 * 5 + 1;
    } else if (lineWidth <= 120) {
      progressBarWidth = 4 * 7 + 1;
    } else {
      progressBarWidth = 4 * 9 + 1;
    }

    final int tickModulo = (progressBarWidth - 1) / 4;

    final StringBuilder out = new StringBuilder();
    final int atColumn = (int) (tracker.completedRatio() * (progressBarWidth - 1));
    for (int i = 0; i < progressBarWidth; i++) {
      char chr = '?';

      if (i == 0) {
        chr = '[';
      } else if (i == progressBarWidth - 1) {
        chr = ']';
      } else if (i == atColumn) {
        chr = '>';
      } else {
        if (i % tickModulo == 0) {
          chr = ':';
        } else {
          if (i < atColumn) {
            chr = '=';  
          } else {
            chr = ' ';
          }
        }
      }

      out.append(chr);
    }

    return out.toString();
  }
}