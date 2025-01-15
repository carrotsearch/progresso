package com.carrotsearch.progresso;

import com.carrotsearch.randomizedtesting.RandomizedContext;
import com.carrotsearch.randomizedtesting.RandomizedTest;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.assertj.core.api.Assertions;
import org.junit.Test;

public class TaskStatsTest extends RandomizedTest {
  @Test
  public void taskTimSortBreak() {
    List<GenericTask> tasks =
        IntStream.range(0, 100)
            .mapToObj(i -> Tasks.newGenericTask("Task " + i))
            .collect(Collectors.toList());

    for (GenericTask t : tasks.subList(0, 50)) {
      t.start();
    }

    for (int i = 0; i < 100; i++) {
      Collections.shuffle(tasks, RandomizedContext.current().getRandom());
      TaskStats.breakdown(tasks);
    }

    System.out.println(TaskStats.breakdown(tasks));
  }

  @Test
  public void taskTruncateNames() {
    GenericTask t1 = Tasks.newGenericTask("task_a " + "~".repeat(200) + "X");

    try (Tracker tracker = t1.start()) {
      tracker.attribute(
          "description " + "%".repeat(150), "%s", "<val>" + " ".repeat(100) + "</val>");
    }

    String breakdown = TaskStats.breakdownBuilder().addTasks(t1).maxColumnWidth(80).toString();

    Assertions.assertThat(breakdown)
        .contains("task_a ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~...~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~X")
        .contains(
            " @ description %%%%%%%%%%%%%%%%%%%%%%%...                                </val>");
  }

  @Test
  public void taskBreakdown() {
    GenericTask t1 = Tasks.newGenericTask("task_a");
    GenericTask t3 = t1.newGenericSubtask("subtask_1");
    GenericTask t2 = Tasks.newGenericTask("task_b");
    GenericTask t4 = t2.newGenericSubtask("subtask_2");

    try (Tracker tt1 = t1.start()) {
      try (Tracker tt3 = t3.start()) {}
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

  @Test
  public void taskBreakdownOrdering() throws Exception {
    GenericTask t1 = Tasks.newGenericTask("task_a");
    GenericTask s1 = t1.newGenericSubtask("subtask_1");
    GenericTask t2 = Tasks.newGenericTask("task_b");
    GenericTask s2 = t2.newGenericSubtask("subtask_2");
    GenericTask s3 = t2.newGenericSubtask("subtask_3");
    s3.attribute("att1", "%s", "att1 value");
    s3.attribute("att2 long attribute", "%s", "att2 value");

    Tracker tr2 = t2.start();
    Thread.sleep(10);

    Tracker tr1 = t1.start();
    s1.start().close();

    Thread.sleep(10);
    s3.start().close();
    Thread.sleep(10);
    s2.start().close();

    tr2.close();
    Thread.sleep(10);

    tr1.close();

    String breakdown = TaskStats.breakdown(t1, t2);
    System.out.println(breakdown);

    Assertions.assertThat(breakdown.indexOf("task_b")).isLessThan(breakdown.indexOf("task_a"));

    Assertions.assertThat(breakdown.indexOf("subtask_3"))
        .isLessThan(breakdown.indexOf("subtask_2"));
  }
}
