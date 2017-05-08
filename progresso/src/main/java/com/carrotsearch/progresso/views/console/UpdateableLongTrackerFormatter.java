package com.carrotsearch.progresso.views.console;

import java.util.Locale;

import com.carrotsearch.progresso.LongTracker;
import com.carrotsearch.progresso.Task;
import com.carrotsearch.progresso.Tracker;
import com.carrotsearch.progresso.util.LineFormatter;

public class UpdateableLongTrackerFormatter extends AbstractTrackerFormatter<Tracker> {
  @Override
  public boolean supports(int lineWidth, Tracker tracker) {
    return tracker instanceof LongTracker;
  }
  
  @Override
  protected void doFormat(LineFormatter lf, int lineWidth, Task<?> task, Tracker tracker) {
    appendTaskName(lf, task);
    
    if (task.isDone()) {
      lf.cell(" done");
    } else {
      appendOptional(lf, String.format(Locale.ROOT, "%,d", ((LongTracker) tracker).at()));
    }
    appendTime(lf, task, tracker);
  }
}
