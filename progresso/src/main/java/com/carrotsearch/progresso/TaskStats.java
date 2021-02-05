package com.carrotsearch.progresso;

import com.carrotsearch.progresso.util.TabularOutput;
import com.carrotsearch.progresso.util.Units;

import java.io.StringWriter;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

public final class TaskStats {
  public static final String TOTAL_TIME = "Total time:";
  private static final Comparator<TaskData> BY_START_TIME =
      (a, b) -> {
        int c = Boolean.compare(b.hasTracker(), a.hasTracker());
        if (c != 0) return c;

        if (a.hasTracker() && b.hasTracker()) {
          c = a.startInstant().compareTo(b.startInstant());
          if (c != 0) return c;

          c = Long.compare(a.elapsedMillis(), b.elapsedMillis());
          if (c != 0) return c;
        }

        return a.taskName().compareTo(b.taskName());
      };

  private TaskStats() {}

  public static String summary(Set<Task<?>> tasks) {
    return String.format(Locale.ROOT, "Done. %s", totalTimeString(tasks));
  }

  private static String totalTimeString(Set<Task<?>> tasks) {
    if (tasks.isEmpty()) {
      throw new IllegalArgumentException("Task set must not be empty.");
    }

    long start = Long.MAX_VALUE;
    long end = 0;

    boolean done = true;
    for (Task<?> t : tasks) {
      if (t.hasTracker()) {
        Tracker tracker = t.getTracker();
        start = Math.min(tracker.startTime(), start);
        end = Math.max(tracker.startTime() + tracker.elapsedMillis(), end);
        if (!t.isDone()) {
          done = false;
        }
      }
    }

    if (!done || start > end) {
      return TOTAL_TIME + " (not done yet).";
    }

    return TOTAL_TIME + " " + Units.DURATION.format((end - start)) + ".";
  }

  public static long elapsedTimeMs(Collection<Task<?>> tasks) {
    long start = Long.MAX_VALUE;
    long end = 0;

    for (Task<?> t : tasks) {
      if (t.hasTracker()) {
        Tracker tracker = t.getTracker();
        start = Math.min(tracker.startTime(), start);
        end = Math.max(tracker.startTime() + tracker.elapsedMillis(), end);
      }
    }

    if (end >= start) {
      return end - start;
    } else {
      return 0;
    }
  }

  public static String breakdown(Collection<? extends Task<?>> tasks) {
    return breakdown(tasks.toArray(new Task<?>[tasks.size()]));
  }

  public static String breakdown(Task<?>... taskList) {
    return breakdown(BY_START_TIME, taskList);
  }

  public static String breakdown(Comparator<TaskData> order, Task<?>... taskList) {
    List<TaskData> tasks = uniqueTasks(order, taskList);

    long total = 0;
    long t0 = Long.MAX_VALUE;
    for (TaskData td : tasks) {
      if (td.hasTracker()) {
        t0 = Math.min(t0, td.startTimeMillis());
        if (td.hasTracker()) {
          total += td.elapsedMillis();
        }
      }
    }

    TabularOutput tabular =
        TabularOutput.to(new StringWriter())
            .columnSeparator("  ")
            .noAutoFlush()
            .addColumn("[Task]", TabularOutput.ColumnSpec::alignLeft)
            .addColumn("[Time]", TabularOutput.ColumnSpec::alignRight)
            .addColumn("[%]", TabularOutput.ColumnSpec::alignRight)
            .addColumn("[+T₀]", TabularOutput.ColumnSpec::alignRight)
            .build();

    for (TaskData td : tasks) {
      breakdownTask(tabular, 0, td, order, total, t0);
    }

    return tabular.flush().getWriter().toString();
  }

  private static List<TaskData> uniqueTasks(Comparator<TaskData> order, Task<?>[] taskList) {
    HashSet<Task<?>> all = new HashSet<>(Arrays.asList(taskList));
    List<TaskData> tasks =
        Arrays.stream(taskList)
            .filter(
                (t) -> {
                  for (Task<?> parent = t.getParent();
                      parent != null;
                      parent = parent.getParent()) {
                    if (all.contains(parent)) {
                      return false;
                    }
                  }
                  return true;
                })
            .collect(Collectors.toCollection(LinkedHashSet::new))
            .stream()
            .map(TaskData::new)
            .sorted(order)
            .collect(Collectors.toList());
    return tasks;
  }

  private static void breakdownTask(
      TabularOutput tabular,
      int indent,
      TaskData td,
      Comparator<TaskData> taskOrdering,
      long total,
      long t0) {
    String padding = repeat(indent, "  ");

    tabular
        .append(
            padding + td.taskName(), td.taskTime(), td.taskTimeFraction(total), td.taskTimeT0(t0))
        .nextRow();

    for (Attribute a : td.task.attributes()) {
      tabular
          .append(String.format(Locale.ROOT, "%s @ %s: %s", padding, a.key, a.value), "", "", "")
          .nextRow();
    }

    td.task.subtasks().stream()
        .map(TaskData::new)
        .sorted(taskOrdering)
        .forEachOrdered(
            (subtask) -> {
              breakdownTask(tabular, indent + 1, subtask, taskOrdering, total, t0);
            });
  }

  private static String repeat(int indent, String s) {
    if (indent == 0) {
      return "";
    }

    if (indent == 1) {
      return s;
    }

    StringBuilder b = new StringBuilder();
    for (; indent > 0; indent--) {
      b.append(s);
    }
    return b.toString();
  }
}
