package com.carrotsearch.progresso.views.console;

import com.carrotsearch.progresso.Progress;
import com.carrotsearch.progresso.ProgressView;
import com.carrotsearch.progresso.RangeTask;
import com.carrotsearch.progresso.RangeTracker;
import com.carrotsearch.progresso.TaskStats;
import com.carrotsearch.randomizedtesting.RandomizedTest;
import org.junit.Test;

import java.util.Arrays;

public class SkippedTaskTest extends RandomizedTest {
  @Test
  public void plainConsoleView() throws InterruptedException {
    runTest(new PlainConsoleView(ConsoleAware.writer()));
  }

  @Test
  public void quietConsoleView() throws InterruptedException {
    runTest(new QuietConsoleView(ConsoleAware.writer()));
  }

  @Test
  public void updateableConsoleView() throws InterruptedException {
    runTest(new UpdateableConsoleView(ConsoleAware.writer()));
  }

  void runTest(ProgressView view) throws InterruptedException {
    try (Progress p = new Progress(view)) {
      RangeTask t1 = new RangeTask("t1");
      RangeTask t2 = new RangeTask("t2");
      RangeTask t3 = new RangeTask("t3");
      p.attach(Arrays.asList(t1, t2, t3));

      try (RangeTracker t = t1.start(0, 10)) {
        Thread.sleep(100);
        t.at(9);
      }

      t2.skip();

      try (RangeTracker t = t3.start(0, 10)) {
        Thread.sleep(100);
        t.at(9);
      }

      System.out.println(TaskStats.breakdown(p.tasks()));
    }
  }
}
