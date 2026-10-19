package com.carrotsearch.progresso.demos;

import com.carrotsearch.progresso.Progress;
import com.carrotsearch.progresso.RangeTracker;
import com.carrotsearch.progresso.views.console.AbstractConsoleView;
import com.carrotsearch.progresso.views.console.ConsoleAware;
import com.carrotsearch.progresso.views.console.ProgressBarStyle;
import com.carrotsearch.progresso.views.console.UpdateableConsoleView;
import java.util.Collections;
import org.junit.Test;

/**
 * Renders a task in each {@link ProgressBarStyle}, then one more with the automatically detected
 * style. Views are driven by {@link Progress}, which polls them twice a second, so each task runs
 * long enough to show a handful of redraws.
 */
public class E012_ProgressBarStyles extends AbstractExampleTest {
  private static final int STEPS = 80;
  private static final long STEP_MILLIS = 50;

  @Test
  public void allStyles() throws Exception {
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

  @Test
  public void autodetected() throws Exception {
    System.out.println(
        "Detected: "
            + ConsoleAware.progressBarStyle()
            + ", charset "
            + ConsoleAware.outputCharset());

    try (Progress progress = defaultProgress();
        RangeTracker t = progress.newRangeSubtask("Autodetected").start(0, STEPS)) {
      runSteps(t);
    }
  }

  private static void runSteps(RangeTracker t) {
    for (int i = 0; i < STEPS; i++) {
      sleep(STEP_MILLIS);
      t.at(i);
    }
  }
}
