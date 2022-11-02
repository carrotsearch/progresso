package com.carrotsearch.progresso.views.console;

import com.carrotsearch.progresso.Task;
import com.carrotsearch.progresso.Tracker;
import com.carrotsearch.progresso.util.LineFormatter;

public class UpdateableGenericTrackerFormatter extends AbstractTrackerFormatter<Tracker> {
  @Override
  public void taskStarted(Task<?> task) {}

  @Override
  public boolean supports(int lineWidth, Tracker tracker) {
    return true;
  }

  @Override
  protected void doFormat(LineFormatter lf, int lineWidth, Task<?> task, Tracker tracker) {
    appendTaskName(lf, task);
    if (task.isDone()) {
      lf.cell(" done");
    }
    appendTime(lf, task, tracker, null);
  }
}
