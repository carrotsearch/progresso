
package com.carrotsearch.progresso.demos;

import java.util.Collections;

import org.junit.Test;

import com.carrotsearch.progresso.LongTracker;
import com.carrotsearch.progresso.Tasks;
import com.carrotsearch.progresso.util.UnitFormatter;
import com.carrotsearch.progresso.views.console.ConsoleAware;
import com.carrotsearch.progresso.views.console.UpdateableConsoleView;

public class E009_NamedItemFormatter extends AbstractExampleTest {
  @Test
  public void subtaskForkedFromParent() throws Exception {
    UpdateableConsoleView view = new UpdateableConsoleView(ConsoleAware.writer());

    long max = 10 * (1024 * 1024);
    long duration = 3 * 1000;
    long steps = 40;
    try (LongTracker t = Tasks.newLongTask("Counting lemmings", UnitFormatter.DECIMAL_COMPACT).start(0)) {
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
