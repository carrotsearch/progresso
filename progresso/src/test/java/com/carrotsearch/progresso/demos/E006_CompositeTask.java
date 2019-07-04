
package com.carrotsearch.progresso.demos;

import com.carrotsearch.progresso.*;
import com.carrotsearch.progresso.views.console.ConsoleAware;
import org.junit.Test;

import java.util.Collections;

public class E006_CompositeTask extends AbstractExampleTest {
  @Test
  public void composite() throws Exception {
    CompositeTask composite = new CompositeTask("Composite");
    RangeTask range1 = composite.attach(Tasks.newRangeTask("Range0-10,w1"));
    RangeTask range2 = composite.attach(Tasks.newRangeTask("Range0-10,w2"), 5);
    GenericTask generic = composite.attach(Tasks.newGenericTask("Generic"), 5);

    ProgressView view = ConsoleAware.newConsoleProgressView();
    view.update(Collections.singleton(composite));

    try (Tracker ctracker = composite.start()) {
      int max = 10;
      try (RangeTracker t = range1.start(0, max + 1)) {
        for (int i = 0; i < max; i++) {
          Thread.sleep(1250);
          t.at(i);
          view.update(Collections.singleton(composite));
        }
      }
      
      generic.start().close();

      try (RangeTracker t = range2.start(0, max + 1)) {
        for (int i = 0; i < max; i++) {
          Thread.sleep(250);
          t.at(i);
          view.update(Collections.singleton(composite));
        }
      }      
    }

    view.update(Collections.singleton(composite));

    System.out.println(TaskStats.breakdown(composite));
  }
}
