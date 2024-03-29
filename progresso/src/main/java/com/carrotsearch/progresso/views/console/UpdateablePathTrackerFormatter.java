package com.carrotsearch.progresso.views.console;

import com.carrotsearch.progresso.PathScanningTask.PathTracker;
import com.carrotsearch.progresso.Task;
import com.carrotsearch.progresso.Task.Status;
import com.carrotsearch.progresso.Tracker;
import com.carrotsearch.progresso.util.LineFormatter;
import com.carrotsearch.progresso.util.LineFormatter.Alignment;
import com.carrotsearch.progresso.util.LineFormatter.Trim;
import java.nio.file.Path;
import java.util.Locale;

public class UpdateablePathTrackerFormatter extends AbstractTrackerFormatter<PathTracker> {

  @Override
  public void taskStarted(Task<?> task) {}

  @Override
  public boolean supports(int lineWidth, Tracker tracker) {
    return tracker instanceof PathTracker;
  }

  @Override
  protected void doFormat(LineFormatter lf, int lineWidth, Task<?> task, PathTracker tracker) {
    if (task.getStatus() != Status.DONE) {
      Path current = tracker.at();
      if (current != null) {
        lf.cell(5, 40, Alignment.LEFT, Trim.MIDDLE, current.toString());
        lf.cell(" ");
      }
      lf.cell(String.format(Locale.ROOT, "%,d", tracker.count()));
    }
  }
}
