
package com.carrotsearch.progresso.demos;

import com.carrotsearch.progresso.LongTracker;
import com.carrotsearch.progresso.ProgressView;
import com.carrotsearch.progresso.Tasks;
import com.carrotsearch.progresso.views.console.ConsoleAware;
import org.junit.Test;

import java.util.Collections;

public class E008_ByteFormatter extends AbstractExampleTest {
  @Test
  public void subtaskForkedFromParent() throws Exception {
    ProgressView view = ConsoleAware.newConsoleProgressView();

    long max = 10 * (1024 * 1024);
    long duration = 3 * 1000;
    long steps = 40;
    try (LongTracker t = Tasks.newByteTask("Downloading foo").start(0)) {
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
