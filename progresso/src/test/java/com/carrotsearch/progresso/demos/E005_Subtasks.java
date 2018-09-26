
package com.carrotsearch.progresso.demos;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import com.carrotsearch.progresso.Progress;
import com.carrotsearch.progresso.RangeTask;
import com.carrotsearch.progresso.RangeTracker;
import com.carrotsearch.progresso.Task;
import com.carrotsearch.progresso.TaskStats;
import com.carrotsearch.progresso.Tasks;
import com.carrotsearch.progresso.views.console.ConsoleAware;
import com.carrotsearch.progresso.views.console.PlainConsoleView;

public class E005_Subtasks extends AbstractExampleTest {
  @Test
  public void dynamicSubtask() throws Exception {
    RangeTask t1 = Tasks.newByteRangeTask("task1");
    RangeTask t2 = Tasks.newByteRangeTask("task2");

    List<Task<?>> topLevel = Arrays.asList(t1, t2);

    try (Progress p = new Progress(new PlainConsoleView(
         ConsoleAware.writer(), DEFAULT_WIDTH, topLevel))) {
      p.attach(topLevel);

      runTask(t1);
      runTask(t2);

      System.out.println(TaskStats.breakdown(
          t1, 
          t2, 
          Tasks.newGenericTask(),
          Tasks.newGenericTask().start().task()));
    }
  }

  private void runTask(RangeTask t) {
    int max = 100;
    try (RangeTracker tracker = t.start(0, max)) {
      tracker.at(5);

      try (RangeTracker sub = t.newByteRangeSubtask("Subtask1 of " + t.getName()).start(0, 100)) {
        sub.at(10);
        sleep(6000);
        sub.at(99);
        sub.attribute("foo", "bar");
        sub.attribute("foo", "%s", "bar");
      }

      try (RangeTracker sub = t.newByteRangeSubtask("Subtask2 of " + t.getName()).start(0, 100)) {
        sub.at(10);
        sleep(500);
        sub.at(99);
      }

      for (int i = 5; i < max; i++) {
        tracker.at(i);
      }
    }
  }
}
