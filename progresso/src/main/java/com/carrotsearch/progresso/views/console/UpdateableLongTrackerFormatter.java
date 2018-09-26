package com.carrotsearch.progresso.views.console;

import java.util.IdentityHashMap;

import com.carrotsearch.progresso.LongTracker;
import com.carrotsearch.progresso.Task;
import com.carrotsearch.progresso.Tracker;
import com.carrotsearch.progresso.util.LineFormatter;
import com.carrotsearch.progresso.util.UnitFormatter;
import com.carrotsearch.progresso.util.LineFormatter.Alignment;
import com.carrotsearch.progresso.util.LineFormatter.Trim;

public class UpdateableLongTrackerFormatter extends AbstractTrackerFormatter<Tracker> {
  private final IdentityHashMap<Tracker, RateCalculator> rateCalculators = new IdentityHashMap<>();

  @Override
  public boolean supports(int lineWidth, Tracker tracker) {
    return tracker instanceof LongTracker;
  }

  @Override
  protected void doFormat(LineFormatter lf, int lineWidth, Task<?> task, Tracker tracker) {
    LongTracker longTracker = (LongTracker) tracker; 

    UnitFormatter unit;
    if (longTracker.task() instanceof WithUnit) {
      unit = ((WithUnit) longTracker.task()).unit();
    } else {
      unit = UnitFormatter.DECIMAL;
    }

    appendTaskName(lf, task);

    final long at = longTracker.at();
    final String value = unit.format(at);
    final int columns = lf.columns(value);
    lf.cell(columns, columns, Alignment.RIGHT, Trim.RIGHT, LineFormatter.PRIORITY_OPTIONAL, value);

    if (!task.isDone()) {
      RateCalculator rateCalculator = 
          rateCalculators.computeIfAbsent(tracker, (key) -> new RateCalculator());
      double bytesPerSec = rateCalculator.tick(System.currentTimeMillis(), at);
      String speedRatio = " @" + unit.format((long) bytesPerSec) + "/s";
      appendOptional(lf, speedRatio);
    }
    appendTime(lf, task, tracker);
  }
}
