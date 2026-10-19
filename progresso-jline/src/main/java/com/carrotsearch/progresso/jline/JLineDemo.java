package com.carrotsearch.progresso.jline;

import com.carrotsearch.progresso.CompositeTask;
import com.carrotsearch.progresso.GenericTask;
import com.carrotsearch.progresso.LongTracker;
import com.carrotsearch.progresso.Progress;
import com.carrotsearch.progresso.RangeTask;
import com.carrotsearch.progresso.RangeTracker;
import com.carrotsearch.progresso.Tasks;
import com.carrotsearch.progresso.Tracker;
import com.carrotsearch.progresso.views.console.ConsoleAware;
import java.util.List;
import java.util.Locale;
import java.util.function.IntConsumer;
import org.jline.terminal.Terminal;
import org.jline.utils.InfoCmp.Capability;

/** Several tasks running at once on a {@link JLineConsoleView}, with log lines in between. */
public final class JLineDemo {
  private JLineDemo() {}

  static void main() throws Exception {
    try (JLineConsoleView view = JLineConsoleView.open()) {
      Terminal terminal = view.terminal();
      view.printLine(
          String.format(
              Locale.ROOT,
              "Terminal: %s (%s), %dx%d, colors: %s, width supplier: %s, progress bar: %s",
              terminal.getName(),
              terminal.getType(),
              terminal.getSize().getColumns(),
              terminal.getSize().getRows(),
              terminal.getNumericCapability(Capability.max_colors),
              ConsoleAware.consoleWidthSupplier()
                  .map(s -> s.getClass().getSimpleName())
                  .orElse("default"),
              ConsoleAware.progressBarStyle().name().toLowerCase(Locale.ROOT)));

      try (Progress progress = new Progress(view)) {
        RangeTracker download = progress.newByteRangeSubtask("Download").start(0, 40_000_000L);
        RangeTracker index = progress.newRangeSubtask("Index documents").start(0, 2_000);
        LongTracker count = progress.newLongSubtask("Count things").start(0);

        List<Thread> workers =
            List.of(
                worker(download, 120, 5, i -> at(download, 40_000_000L, i, 120)),
                worker(index, 60, 2, i -> at(index, 2_000L, i, 60)),
                worker(count, 150, 1, i -> count.incrementBy(1_000 + (i % 13) * 300)));

        try (Tracker t = progress.newGenericSubtask("Warm-up (no progress tracking)").start()) {
          Thread.sleep(2_500);
        }

        Thread.sleep(1_000);
        view.printLine("[log] A line printed while the tasks run: it stays above the live block.");

        for (Thread worker : workers) {
          worker.join();
        }

        // Nested tasks: the parent's own progress and the running child are both live.
        RangeTask build = progress.newByteRangeSubtask("Build index");
        try (RangeTracker whole = build.start(0, 3_000_000L)) {
          String[] phases = {"Parse", "Tokenize", "Write"};
          for (int phase = 0; phase < phases.length; phase++) {
            try (RangeTracker part = build.newRangeSubtask(phases[phase]).start(0, 400)) {
              for (int i = 0; i < 400; i++) {
                Thread.sleep(3);
                part.at(i);
                whole.at(Math.min(2_999_999L, 1_000_000L * phase + 2_500L * i));
              }
            }
          }
        }

        // A composite task aggregates its weighted children into one completion ratio.
        CompositeTask pipeline = new CompositeTask("Pipeline");
        RangeTask fetch = pipeline.attach(Tasks.newRangeTask("Fetch"), 1);
        RangeTask transform = pipeline.attach(Tasks.newRangeTask("Transform"), 3);
        GenericTask publish = pipeline.attach(Tasks.newGenericTask("Publish"), 1);
        progress.attach(pipeline);
        try (Tracker whole = pipeline.start()) {
          for (RangeTask stage : List.of(fetch, transform)) {
            try (RangeTracker t = stage.start(0, 100)) {
              for (int i = 0; i < 100; i++) {
                Thread.sleep(stage == transform ? 20 : 8);
                t.at(i);
              }
            }
          }
          try (Tracker t = publish.start()) {
            Thread.sleep(600);
          }
        }

        progress.newRangeSubtask("Skipped on purpose").skip();
        try (RangeTracker t = progress.newRangeSubtask("Final pass").start(0, 50)) {
          for (int i = 0; i < 50; i++) {
            Thread.sleep(40);
            t.at(i);
          }
        }
      }
    }
  }

  /**
   * Sets the tracker to step {@code i} of {@code steps}; the range is half-open, so never to {@code
   * to}.
   */
  private static void at(RangeTracker tracker, long to, int i, int steps) {
    tracker.at(Math.min(to - 1, to * i / steps));
  }

  /** Runs {@code steps} calls of {@code step} on a daemon thread, then closes the tracker. */
  private static Thread worker(Tracker tracker, int steps, int stepMillis, IntConsumer step) {
    Thread thread =
        Thread.ofPlatform()
            .daemon()
            .start(
                () -> {
                  try (tracker) {
                    for (int i = 1; i <= steps; i++) {
                      Thread.sleep(stepMillis * (1 + (i % 7)));
                      step.accept(i);
                    }
                  } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                  }
                });
    return thread;
  }
}
