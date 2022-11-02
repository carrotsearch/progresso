package com.carrotsearch.progresso;

import com.carrotsearch.progresso.demos.AbstractExampleTest;
import com.carrotsearch.progresso.views.console.ConsoleAware;
import java.util.Arrays;
import org.junit.Test;

public class CompositeTaskTest extends AbstractExampleTest {
  @Test
  public void attachAfterViewCreated() throws Exception {
    CompositeTask composite = new CompositeTask("Composite");
    RangeTask range1 = composite.attach(Tasks.newRangeTask("Range0-10,w1"));
    RangeTask range2 = composite.attach(Tasks.newRangeTask("Range0-10,w2"), 5);
    GenericTask generic = composite.attach(Tasks.newGenericTask("Generic"), 5);

    try (Progress p = new Progress(ConsoleAware.newConsoleProgressView())) {
      p.attach(composite);

      try (CompositeTask.WeightedTracker ctracker = composite.start()) {
        int max = 10;
        try (RangeTracker t = range1.start(0, max + 1)) {
          for (int i = 0; i < max; i++) {
            Thread.sleep(250);
            t.at(i);
          }
        }

        assert ctracker.completedRatio() > 0;

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

  @Test
  public void weightsCorrectAfterAttachGroup() throws Exception {
    CompositeTask composite = new CompositeTask("Composite");
    RangeTask range1 = Tasks.newRangeTask("Range0-10,w1");
    RangeTask range2 = Tasks.newRangeTask("Range0-10,w2");
    GenericTask generic = Tasks.newGenericTask("Generic");
    composite.attach(Arrays.asList(range1, range2, generic));

    try (Progress p = new Progress(ConsoleAware.newConsoleProgressView())) {
      p.attach(composite);

      try (CompositeTask.WeightedTracker ctracker = composite.start()) {
        int max = 10;
        try (RangeTracker t = range1.start(0, max + 1)) {
          for (int i = 0; i < max; i++) {
            t.at(i);
          }
        }
        assert ctracker.completedRatio() > 0;

        generic.start().close();

        try (RangeTracker t = range2.start(0, max + 1)) {
          for (int i = 0; i < max; i++) {
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
