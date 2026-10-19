package com.carrotsearch.progresso.demos;

import com.carrotsearch.progresso.Progress;
import com.carrotsearch.progresso.RangeTracker;
import com.carrotsearch.progresso.Tasks;
import com.carrotsearch.progresso.views.console.AbstractConsoleView;
import com.carrotsearch.progresso.views.console.ConsoleAware;
import com.carrotsearch.progresso.views.console.ProgressBarStyle;
import com.carrotsearch.progresso.views.console.UpdateableConsoleView;
import java.util.Collections;
import org.junit.Test;

public class E012_ProgressBarStyles extends AbstractExampleTest {
  @Test
  public void allStyles() throws Exception {
    for (ProgressBarStyle style : ProgressBarStyle.values()) {
      UpdateableConsoleView view =
          new UpdateableConsoleView(
              ConsoleAware.writer(),
              Collections.emptyList(),
              AbstractConsoleView.defaultFormatters(style));

      int steps = 60;
      try (RangeTracker t = Tasks.newRangeTask("Style " + style.name()).start(0, steps)) {
        for (int i = 0; i < steps; i++) {
          sleep(1000 / steps);
          t.at(i);
          view.update(Collections.singleton(t.task()));
        }
        t.close();
        view.update(Collections.singleton(t.task()));
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
    int steps = 60;
    try (Progress progress = defaultProgress();
        RangeTracker t = progress.newRangeSubtask("Autodetected").start(0, steps)) {
      for (int i = 0; i < steps; i++) {
        sleep(1000 / steps);
        t.at(i);
      }
    }
  }
}
