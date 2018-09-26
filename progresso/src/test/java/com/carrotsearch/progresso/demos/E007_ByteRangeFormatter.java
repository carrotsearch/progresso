
package com.carrotsearch.progresso.demos;

import java.util.Collections;

import org.junit.Test;

import com.carrotsearch.progresso.RangeTracker;
import com.carrotsearch.progresso.Tasks;
import com.carrotsearch.progresso.views.console.ConsoleAware;
import com.carrotsearch.progresso.views.console.UpdateableConsoleView;

public class E007_ByteRangeFormatter extends AbstractExampleTest {
  @Test
  public void subtaskForkedFromParent() throws Exception {
    UpdateableConsoleView view = new UpdateableConsoleView(ConsoleAware.writer());
    
    long max = 10 * (1024 * 1024);
    long duration = 3 * 1000;
    long steps = 40;
    try (RangeTracker t = Tasks.newByteRangeTask("Downloading foo").start(0, max + 1)) {
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
