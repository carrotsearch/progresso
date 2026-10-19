package com.carrotsearch.progresso;

import static com.carrotsearch.progresso.views.console.AbstractConsoleView.defaultFormatters;

import com.carrotsearch.progresso.views.console.ConsoleAware;
import com.carrotsearch.progresso.views.console.ConsoleWriter;
import com.carrotsearch.progresso.views.console.ProgressBarStyle;
import com.carrotsearch.progresso.views.console.UpdateableConsoleView;
import java.util.Collections;
import java.util.List;

public class Demo {
  private static final int STEPS = 80;
  private static final long STEP_MILLIS = 50;

  static void main() throws Exception {
    ConsoleWriter out = ConsoleAware.writer();
    List<ProgressBarStyle> supported = ConsoleAware.supportedProgressBarStyles();
    out.printLine(
        "Console charset: "
            + ConsoleAware.outputCharset().name()
            + ", supported progress bar styles: "
            + supported);

    for (ProgressBarStyle style : ProgressBarStyle.values()) {
      if (!supported.contains(style)) {
        out.printLine("Style " + style.name() + ": not supported in this environment, skipped.");
        continue;
      }

      UpdateableConsoleView view =
          new UpdateableConsoleView(out, Collections.emptyList(), defaultFormatters(style));

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
