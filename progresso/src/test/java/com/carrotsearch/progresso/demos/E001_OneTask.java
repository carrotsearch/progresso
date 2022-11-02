package com.carrotsearch.progresso.demos;

import com.carrotsearch.progresso.Progress;
import com.carrotsearch.progresso.RangeTask;
import com.carrotsearch.progresso.RangeTracker;
import com.carrotsearch.progresso.Tasks;
import org.junit.Test;

public class E001_OneTask extends AbstractExampleTest {
  @Test
  public void subtaskForkedFromParent() {
    try (Progress progress = defaultProgress()) {
      runTask(progress);
    }
  }

  @Test
  public void subtaskAttachedToParent() {
    try (Progress progress = defaultProgress()) {
      // Early init of the task (for example during configuration collection).
      RangeTask task = Tasks.newByteRangeTask("Bytes");

      // Attaching to parent.
      progress.attach(task);

      // Then just run the task.
      runTask(task);
    }
  }

  private void runTask(RangeTask task) {
    int max = 1024 * 1024 * 10;
    try (RangeTracker t = task.start(0, max)) {
      for (int i = 0; i <= max; i += max / 200) {
        sleep(10);
        t.at(i);
      }
    }
  }

  private void runTask(Tasks parent) {
    int max = 1024 * 1024 * 10;
    try (RangeTracker t = parent.newByteRangeSubtask("Bytes").start(0, max)) {
      for (int i = 0; i < max; i += max / 200) {
        sleep(25);
        t.at(i);
      }
    }
  }
}
