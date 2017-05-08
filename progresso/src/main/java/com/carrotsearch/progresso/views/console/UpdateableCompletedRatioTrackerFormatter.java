package com.carrotsearch.progresso.views.console;

import java.util.Locale;

import com.carrotsearch.progresso.CompletedRatio;
import com.carrotsearch.progresso.Task;
import com.carrotsearch.progresso.Task.Status;
import com.carrotsearch.progresso.Tracker;
import com.carrotsearch.progresso.util.LineFormatter;
import com.carrotsearch.progresso.util.LineFormatter.Alignment;

public class UpdateableCompletedRatioTrackerFormatter extends AbstractTrackerFormatter<Tracker> {
  @Override
  public boolean supports(int lineWidth, Tracker tracker) {
    return tracker instanceof CompletedRatio;
  }
  
  @Override
  protected void doFormat(LineFormatter lf, int lineWidth, Task<?> task, Tracker tracker) {
    appendTaskName(lf, task);
    CompletedRatio ratioTracker = (CompletedRatio) tracker;
    if (task.getStatus() != Status.DONE) {
      appendOptional(lf, dots(lineWidth, ratioTracker));
    }
    appendPercent(lf, task, ratioTracker);
    appendTime(lf, task, tracker);
  }

  private static void appendPercent(LineFormatter lf, Task<?> task, CompletedRatio tracker) {
    if (task.isDone()) {
      lf.cell(" done");
    } else {
      double p = 100.0d * tracker.completedRatio();
      lf.cell(5, 5, Alignment.RIGHT, String.format(Locale.ROOT, "%3.0f%%", p));
    }
  }
}
