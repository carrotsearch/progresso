package com.carrotsearch.progresso.views.console;

import java.util.IdentityHashMap;

import com.carrotsearch.progresso.ByteTracker;
import com.carrotsearch.progresso.Task;
import com.carrotsearch.progresso.Task.Status;
import com.carrotsearch.progresso.Tracker;
import com.carrotsearch.progresso.util.LineFormatter;
import com.carrotsearch.progresso.util.UnitFormatter;
import com.carrotsearch.progresso.util.LineFormatter.Alignment;
import com.carrotsearch.progresso.util.LineFormatter.Trim;

public class UpdateableByteTrackerFormatter extends AbstractTrackerFormatter<Tracker> {
  private final IdentityHashMap<Tracker, RateCalculator> rateCalculators = new IdentityHashMap<>();

  @Override
  public boolean supports(int lineWidth, Tracker tracker) {
    return tracker instanceof ByteTracker;
  }

  @Override
  protected void doFormat(LineFormatter lf, int lineWidth, Task<?> task, Tracker tracker) {
    appendTaskName(lf, task);
    if (task.getStatus() != Status.DONE) {
      appendByteProgress(lf, lineWidth, (ByteTracker) tracker);
    }
    appendTime(lf, task, tracker);
  }

  protected void appendByteProgress(LineFormatter lf, int lineWidth, ByteTracker tracker) {
    if (!rateCalculators.containsKey(tracker)) {
      rateCalculators.put(tracker, new RateCalculator());
    }

    final long at = tracker.at();
    
    String value = UnitFormatter.BYTES.format(tracker.at());
    int columns = lf.columns(value);
    lf.cell(columns, columns, Alignment.RIGHT, Trim.RIGHT, LineFormatter.PRIORITY_OPTIONAL, value);

    double bytesPerSec = rateCalculators.get(tracker).tick(System.currentTimeMillis(), at);
    String speedRatio = " @" + UnitFormatter.BYTES.format((long) bytesPerSec) + "/s";
    appendOptional(lf, speedRatio);
  }
}
