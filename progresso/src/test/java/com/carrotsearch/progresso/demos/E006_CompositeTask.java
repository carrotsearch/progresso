
package com.carrotsearch.progresso.demos;

import java.util.Collections;

import org.junit.Test;

import com.carrotsearch.progresso.CompositeTask;
import com.carrotsearch.progresso.GenericTask;
import com.carrotsearch.progresso.RangeTask;
import com.carrotsearch.progresso.RangeTracker;
import com.carrotsearch.progresso.TaskStats;
import com.carrotsearch.progresso.Tasks;
import com.carrotsearch.progresso.Tracker;
import com.carrotsearch.progresso.views.console.ConsoleAware;
import com.carrotsearch.progresso.views.console.UpdateableConsoleView;

public class E006_CompositeTask extends AbstractExampleTest {
  @Test
  public void composite() throws Exception {
    CompositeTask composite = new CompositeTask("Composite");
    RangeTask range1 = composite.attach(Tasks.newRangeTask("Range0-10,w1"));
    RangeTask range2 = composite.attach(Tasks.newRangeTask("Range0-10,w2"), 5);
    GenericTask generic = composite.attach(Tasks.newGenericTask("Generic"), 5);

    UpdateableConsoleView view = new UpdateableConsoleView(ConsoleAware.writer());
    view.update(Collections.singleton(composite));

    try (Tracker ctracker = composite.start()) {
      int max = 10;
      try (RangeTracker t = range1.start(0, max + 1)) {
        for (int i = 0; i < max; i++) {
          Thread.sleep(250);
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
