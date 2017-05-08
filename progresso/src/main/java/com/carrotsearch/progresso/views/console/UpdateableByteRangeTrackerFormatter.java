package com.carrotsearch.progresso.views.console;

import java.util.IdentityHashMap;
import java.util.Locale;

import com.carrotsearch.progresso.ByteRangeTracker;
import com.carrotsearch.progresso.CompletedRatio;
import com.carrotsearch.progresso.LongTracker;
import com.carrotsearch.progresso.Task;
import com.carrotsearch.progresso.Task.Status;
import com.carrotsearch.progresso.Tracker;
import com.carrotsearch.progresso.util.LineFormatter;
import com.carrotsearch.progresso.util.UnitFormatter;
import com.carrotsearch.progresso.util.LineFormatter.Alignment;

public class UpdateableByteRangeTrackerFormatter extends AbstractTrackerFormatter<Tracker> {
  private final IdentityHashMap<Tracker, RateCalculator> rateCalculators = new IdentityHashMap<>();

  @Override
  public boolean supports(int lineWidth, Tracker tracker) {
    return tracker instanceof ByteRangeTracker;
  }

  @Override
  protected void doFormat(LineFormatter lf, int lineWidth, Task<?> task, Tracker tracker) {
    appendTaskName(lf, task);
    CompletedRatio ratioTracker = (CompletedRatio) tracker;
    if (task.getStatus() != Status.DONE) {
      String dots = overlayBytesPerSecond(lineWidth, tracker, ratioTracker);
      appendOptional(lf, dots);
    }
    appendPercent(lf, task, ratioTracker);
    appendTime(lf, task, tracker);
  }

  protected String overlayBytesPerSecond(int lineWidth, Tracker tracker, CompletedRatio ratioTracker) {
    String dots = dots(lineWidth, ratioTracker);
    if (!rateCalculators.containsKey(tracker)) {
      rateCalculators.put(tracker, new RateCalculator());
    }
    double bytesPerSec = rateCalculators.get(tracker).tick(System.currentTimeMillis(), ((LongTracker) tracker).at());

    String speedRatio = UnitFormatter.BYTES.format((long) bytesPerSec) + "/s"; 
    StringBuilder b = new StringBuilder(dots);
    int at;
    if (ratioTracker.completedRatio() > 0.5) {
      at = 1;
    } else {
      at = b.length() - speedRatio.length() - 1;
    }
    b.replace(at, at + speedRatio.length(), speedRatio);
    dots = b.toString();
    return dots;
  }

  private static void appendPercent(LineFormatter lf, Task<?> task, CompletedRatio tracker) {
    if (task.getStatus() == Status.DONE) {
      lf.cell(" done");
    } else {
      double p = 100.0d * tracker.completedRatio();
      lf.cell(5, 5, Alignment.RIGHT, String.format(Locale.ROOT, "%3.0f%%", p));
    }
  }
}
