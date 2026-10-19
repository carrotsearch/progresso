package com.carrotsearch.progresso;

import com.carrotsearch.progresso.views.console.AbstractConsoleView;
import com.carrotsearch.progresso.views.console.ConsoleAware;
import com.carrotsearch.progresso.views.console.ProgressBarStyle;
import com.carrotsearch.progresso.views.console.UpdateableConsoleView;
import java.util.Collections;

public class Demo {
  private static final int STEPS = 80;
  private static final long STEP_MILLIS = 50;

  static void main() throws InterruptedException {
    for (ProgressBarStyle style : ProgressBarStyle.values()) {
      UpdateableConsoleView view =
          new UpdateableConsoleView(
              ConsoleAware.writer(),
              Collections.emptyList(),
              AbstractConsoleView.defaultFormatters(style));

      try (Progress progress = new Progress(view);
          RangeTracker t = progress.newRangeSubtask("Style " + style.name()).start(0, STEPS)) {
        runSteps(t);
      }
    }
  }

  private static void runSteps(RangeTracker t) throws InterruptedException {
    for (int i = 0; i < STEPS; i++) {
      Thread.sleep(STEP_MILLIS);
      t.at(i);
    }
  }
}
