package com.carrotsearch.progresso.demos;

import com.carrotsearch.progresso.Progress;
import com.carrotsearch.progresso.RangeTask;
import com.carrotsearch.progresso.RangeTracker;
import com.carrotsearch.progresso.Tasks;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.Test;

public class E003_SequentialTasks extends AbstractExampleTest {
  @Test
  public void sequentialTasksOutOfOrder() {
    List<? extends RangeTask> tasks =
        Arrays.asList(
            Tasks.newByteRangeTask("Reading file1.bin"),
            Tasks.newByteRangeTask("Reading file2.bin"),
            Tasks.newByteRangeTask("Reading file3.bin"));

    try (Progress p = defaultProgress()) {
      p.attach(tasks);

      Collections.reverse(tasks);
      for (RangeTask task : tasks) {
        int max = 1024 * 1024;
        try (RangeTracker tracker = task.start(0, max)) {
          for (int i = 0; i < max; i += randomIntBetween(0, 10 * 1024)) {
            tracker.at(i);
            sleep(randomIntBetween(1, 50));
          }
        }
      }
    }
  }
}
