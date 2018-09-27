package com.carrotsearch.progresso.views.console;

import java.util.IdentityHashMap;
import java.util.Locale;

import com.carrotsearch.progresso.CompletedRatio;
import com.carrotsearch.progresso.LongTracker;
import com.carrotsearch.progresso.Task;
import com.carrotsearch.progresso.Task.Status;
import com.carrotsearch.progresso.Tracker;
import com.carrotsearch.progresso.util.LineFormatter;
import com.carrotsearch.progresso.util.LineFormatter.Alignment;
import com.carrotsearch.progresso.util.UnitFormatter;

public class UpdateableCompletedRatioTrackerFormatter extends AbstractTrackerFormatter<Tracker> {
  private final IdentityHashMap<Tracker, RateCalculator> rateCalculators = new IdentityHashMap<>();

  @Override
  public void taskStarted(Task<?> task) {
    if (task.getTracker() instanceof LongTracker) {
      LongTracker tracker = (LongTracker) task.getTracker();
      RateCalculator rateCalculator = rateCalculators.computeIfAbsent(tracker, (key) -> new RateCalculator());
      rateCalculator.tick(System.currentTimeMillis(), tracker.at());
    }
  }

  @Override
  public boolean supports(int lineWidth, Tracker tracker) {
    return tracker instanceof CompletedRatio;
  }
  
  @Override
  protected void doFormat(LineFormatter lf, int lineWidth, Task<?> task, Tracker tracker) {
    appendTaskName(lf, task);

    final double completedRatio = ((CompletedRatio) tracker).completedRatio();
    
    if (task.getStatus() != Status.DONE) {
      String dots = dots(lineWidth, completedRatio);
      
      if (tracker.task() instanceof WithUnit &&
          tracker instanceof LongTracker) {
        UnitFormatter unit = ((WithUnit) tracker.task()).unit();
        dots = overlayUnitsPerSecond(dots, (LongTracker) tracker, unit);
      }
      appendOptional(lf, dots);
    }
    appendPercent(lf, task, completedRatio);
    appendTime(lf, task, tracker);
  }

  protected String overlayUnitsPerSecond(String dots, LongTracker tracker, UnitFormatter unit) {
    RateCalculator rateCalculator = 
        rateCalculators.computeIfAbsent(tracker, (key) -> new RateCalculator());

    String itemsPerSec = unit.format((long) rateCalculator.tick(System.currentTimeMillis(), tracker.at()));
    if (itemsPerSec != null) {
      String speedRatio = itemsPerSec + "/s"; 
  
      StringBuilder b = new StringBuilder(dots);
      int at;
      if (((CompletedRatio) tracker).completedRatio() > 0.5) {
        at = 1;
      } else {
        at = b.length() - speedRatio.length() - 1;
      }
      b.replace(at, at + speedRatio.length(), speedRatio);
      dots = b.toString();
    }

    return dots;
  }

  private static void appendPercent(LineFormatter lf, Task<?> task, double completedRatio) {
    if (task.isDone()) {
      lf.cell(" done");
    } else {
      double p = 100.0d * completedRatio;
      lf.cell(5, 5, Alignment.RIGHT, String.format(Locale.ROOT, "%3.0f%%", p));
    }
  }
}
