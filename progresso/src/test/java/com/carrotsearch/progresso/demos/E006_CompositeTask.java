
package com.carrotsearch.progresso.demos;

import com.carrotsearch.progresso.*;
import com.carrotsearch.progresso.views.console.ConsoleAware;
import org.junit.Test;

public class E006_CompositeTask extends AbstractExampleTest {
  @Test
  public void composite() throws Exception {
    CompositeTask composite = new CompositeTask("Composite");
    // Default weight (1)
    RangeTask range1 = composite.newRangeSubtask("Range0-10,w1");
    // Explicit weights (5)
    RangeTask range2 = composite.attach(Tasks.newRangeTask("Range0-10,w2"), 5);
    GenericTask generic = composite.attach(Tasks.newGenericTask("Generic"), 5);

    try (Progress p = new Progress(ConsoleAware.newConsoleProgressView())) {
      p.attach(composite);

      try (Tracker ctracker = composite.start()) {
        int max = 10;
        try (RangeTracker t = range1.start(0, max + 1)) {
          for (int i = 0; i < max; i++) {
            Thread.sleep(650);
            t.at(i);
          }
        }

        generic.start().close();

        try (RangeTracker t = range2.start(0, max + 1)) {
          for (int i = 0; i < max; i++) {
            Thread.sleep(250);
            t.at(i);
          }
        }

        try (Tracker ignored = range1.newGenericSubtask("Range1-generic").start()) {
          // nothing.
        }
      }
    }

    System.out.println(TaskStats.breakdown(composite));
  }
}
