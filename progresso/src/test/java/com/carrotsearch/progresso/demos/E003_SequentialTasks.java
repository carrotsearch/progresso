
package com.carrotsearch.progresso.demos;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Test;

import com.carrotsearch.progresso.ByteRangeTask;
import com.carrotsearch.progresso.ByteRangeTracker;
import com.carrotsearch.progresso.Progress;
import com.carrotsearch.progresso.Tasks;

public class E003_SequentialTasks extends AbstractExampleTest {
  @Test
  public void sequentialTasksOutOfOrder() {
    List<ByteRangeTask> tasks = Arrays.asList(
        Tasks.newByteRangeTask("Reading file1.bin"),
        Tasks.newByteRangeTask("Reading file2.bin"),
        Tasks.newByteRangeTask("Reading file3.bin"));

    try (Progress p = defaultProgress()) {
      p.attach(tasks);

      Collections.reverse(tasks);
      for (ByteRangeTask task : tasks) {
        int max = 1024 * 1024;
        try (ByteRangeTracker tracker = task.start(0, max)) {
          for (int i = 0; i < max; i += randomIntBetween(0, 10 * 1024)) {
            tracker.at(i);
            sleep(randomIntBetween(1, 50));
          }
        }
      }
    }
  }
}
