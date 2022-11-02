package com.carrotsearch.progresso.demos;

import com.carrotsearch.progresso.Progress;
import com.carrotsearch.progresso.RangeTracker;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.Test;

public class E004_ParallelTasks extends AbstractExampleTest {
  @Test
  public void parallelTasks() throws Exception {
    try (Progress p = defaultProgress()) {
      List<Callable<Void>> tasks = new ArrayList<>();
      for (int i = 0; i < 5; i++) {
        String taskName = "Reading file: " + i;
        int max = (i + 1) * 1024 * 1024;
        tasks.add(
            () -> {
              try (RangeTracker tracker = p.newByteRangeSubtask(taskName).start(0, max)) {
                for (int j = 0; j < max; j += randomIntBetween(0, 10 * 1024)) {
                  tracker.at(j);
                  sleep(randomIntBetween(1, 5));
                }
              }
              return null;
            });
      }

      ExecutorService service = Executors.newFixedThreadPool(tasks.size());
      for (Future<Void> f : service.invokeAll(tasks)) {
        f.get();
      }
      service.shutdown();
      service.awaitTermination(1, TimeUnit.MINUTES);
    }
  }
}
