package com.carrotsearch.progresso;

import org.assertj.core.api.Assertions;
import org.junit.Test;

import com.carrotsearch.randomizedtesting.RandomizedTest;

public class TaskStatsTest extends RandomizedTest {
  @Test
  public void taskBreakdown() {
    GenericTask t1 = Tasks.newGenericTask("task_a");
    GenericTask t3 = t1.newGenericSubtask("subtask_1");
    GenericTask t2 = Tasks.newGenericTask("task_b");
    GenericTask t4 = t2.newGenericSubtask("subtask_2");

    try (Tracker tt1 = t1.start()) {
      try (Tracker tt3 = t3.start()) {
      }
    }

    t2.start().close();
    t4.start().close();

    Assertions.assertThat(TaskStats.breakdown(t1, t1, t1))
      .containsOnlyOnce("task_a")
      .containsOnlyOnce("subtask_1");

    Assertions.assertThat(TaskStats.breakdown(t1, t2))
      .containsOnlyOnce("task_a")
      .containsOnlyOnce("task_b");

    Assertions.assertThat(TaskStats.breakdown(t1, t4))
      .containsOnlyOnce("task_a")
      .containsOnlyOnce("subtask_1")
      .containsOnlyOnce("subtask_2")
      .doesNotContain("task_b");
    
    Assertions.assertThat(TaskStats.breakdown(t1, t2, t3, t4))
      .containsOnlyOnce("task_a")
      .containsOnlyOnce("subtask_1")
      .containsOnlyOnce("subtask_2")
      .containsOnlyOnce("task_b");    
  }
}
