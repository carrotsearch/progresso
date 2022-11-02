package com.carrotsearch.progresso.demos;

import com.carrotsearch.progresso.LongTracker;
import com.carrotsearch.progresso.Tasks;
import com.carrotsearch.progresso.util.Units;
import com.carrotsearch.progresso.views.console.ConsoleAware;
import com.carrotsearch.progresso.views.console.UpdateableConsoleView;
import java.util.Collections;
import org.junit.Test;

public class E009_NamedItemFormatter extends AbstractExampleTest {
  @Test
  public void countCompact() throws Exception {
    UpdateableConsoleView view = new UpdateableConsoleView(ConsoleAware.writer());

    long max = 10 * (1024 * 1024);
    long duration = 3 * 1000;
    long steps = 40;
    try (LongTracker t = Tasks.newLongTask("Counting lemmings", Units.DECIMAL_COMPACT).start(0)) {
      for (int i = 0; i < steps; i++) {
        sleep(duration / steps);
        t.incrementBy(max / steps);

        view.update(Collections.singleton(t.task()));
      }
      t.close();

      view.update(Collections.singleton(t.task()));
    }
  }

  @Test
  public void countDefault() throws Exception {
    UpdateableConsoleView view = new UpdateableConsoleView(ConsoleAware.writer());

    long max = 10 * (1024 * 1024);
    long duration = 3 * 1000;
    long steps = 40;
    try (LongTracker t = Tasks.newLongTask("Counting lemmings").start(0)) {
      for (int i = 0; i < steps; i++) {
        sleep(duration / steps);
        t.incrementBy(max / steps);

        view.update(Collections.singleton(t.task()));
      }
      t.close();

      view.update(Collections.singleton(t.task()));
    }
  }
}
