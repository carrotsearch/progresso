package com.carrotsearch.progresso.demos;

import com.carrotsearch.progresso.Progress;
import com.carrotsearch.progresso.RangeTracker;
import com.carrotsearch.progresso.util.Units;
import org.junit.Ignore;
import org.junit.Test;

public class E011_VariableSpeed extends AbstractExampleTest {
  @Test
  @Ignore
  public void variableProcessingSpeed() throws Exception {
    long max = 1024 * 1024 * 3;
    long current = 0;
    long step = 1024 * 5;
    try (Progress p = defaultProgress();
        RangeTracker t = p.newByteRangeSubtask("Reading file").start(0, max + 1)) {

      while (current < max) {
        current = Units.clamp(current + step, 0, max);
        t.at(current);

        // increase the 'reading' speed.
        Thread.sleep(500);
        if (current > (long) (max * 0.9)) {
          step = 1024 * 5;
        } else {
          step = (long) (step * 1.2);
        }
      }
    }
  }
}
