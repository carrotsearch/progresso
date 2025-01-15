package com.carrotsearch.progresso;

import com.carrotsearch.progresso.util.TabularOutput;
import com.carrotsearch.progresso.util.Units;
import java.io.StringWriter;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class TaskStats {
  public static final String TOTAL_TIME = "Total time:";
  public static final Comparator<TaskData> BY_START_TIME;

  static {
    BY_START_TIME =
        Comparator.<TaskData, Instant>comparing(
                v -> v.hasTracker() ? v.startInstant() : v.getTask().getLastStatusChangeTime())
            .thenComparingLong(v -> v.hasTracker() ? v.elapsedMillis() : 0)
            .thenComparing(TaskData::taskName);
  }

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

    if (!done || (start != Long.MAX_VALUE && start > end)) {
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

  public static class BreakDownBuilder {
    private List<Task<?>> tasks = new ArrayList<>();
    private Comparator<TaskData> comparator;
    private Integer maxColumnWidth = null;

    public BreakDownBuilder addTasks(Stream<? extends Task<?>> tasks) {
      tasks.forEach(this.tasks::add);
      return this;
    }

    public BreakDownBuilder addTasks(Collection<? extends Task<?>> tasks) {
      return this.addTasks(tasks.stream());
    }

    public BreakDownBuilder addTasks(Task<?>... tasks) {
      return this.addTasks(Stream.of(tasks));
    }

    public BreakDownBuilder maxColumnWidth(int maxColumnWidth) {
      this.maxColumnWidth = maxColumnWidth;
      return this;
    }

    public BreakDownBuilder withComparator(Comparator<TaskData> comparator) {
      if (this.comparator != null) {
        throw new RuntimeException("Only one comparator is allowed.");
      }
      this.comparator = comparator;
      return this;
    }

    @Override
    public String toString() {
      var order = Objects.requireNonNullElse(comparator, BY_START_TIME);
      List<TaskData> tasks = uniqueTasks(order, this.tasks.toArray(Task[]::new));

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
              .addColumn(
                  "[Task]",
                  spec -> {
                    if (maxColumnWidth != null) {
                      spec.maxWidth(maxColumnWidth);
                    }
                    spec.alignLeft();
                  })
              .addColumn("[Time]", TabularOutput.ColumnSpec::alignRight)
              .addColumn("[%]", TabularOutput.ColumnSpec::alignRight)
              .addColumn("[+T₀]", TabularOutput.ColumnSpec::alignRight)
              .build();

      for (TaskData td : tasks) {
        breakdownTask(tabular, 0, td, order, total, t0);
      }

      return tabular.flush().getWriter().toString();
    }
  }

  public static BreakDownBuilder breakdownBuilder() {
    return new BreakDownBuilder();
  }

  public static String breakdown(Collection<? extends Task<?>> tasks) {
    return breakdownBuilder().addTasks(tasks).toString();
  }

  public static String breakdown(Task<?>... taskList) {
    return breakdownBuilder().addTasks(taskList).toString();
  }

  public static String breakdown(Comparator<TaskData> order, Task<?>... taskList) {
    return breakdownBuilder().withComparator(order).addTasks(taskList).toString();
  }

  public static List<TaskData> uniqueTasks(Comparator<TaskData> order, Task<?>[] taskList) {
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

    for (Attribute a : td.getTask().attributes()) {
      tabular
          .append(String.format(Locale.ROOT, "%s @ %s: %s", padding, a.key, a.value), "", "", "")
          .nextRow();
    }

    td.getTask().subtasks().stream()
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
