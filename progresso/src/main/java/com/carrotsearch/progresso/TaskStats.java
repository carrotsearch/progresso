package com.carrotsearch.progresso;

import com.carrotsearch.progresso.util.Units;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Formatter;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

public final class TaskStats {
  public static final String TOTAL_TIME = "Total time:";

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

  private static final Comparator<? super Task<?>> BY_START_TIME =
      (a, b) -> {
        int c = Boolean.compare(b.hasTracker(), a.hasTracker());
        if (c != 0) {
          return c;
        } else {
          if (a.hasTracker()) {
            c = Long.compare(a.getTracker().startTime(), b.getTracker().startTime());
            if (c != 0) {
              return c;
            }
          }

          c = a.getName().compareTo(b.getName());
        }
        return c;
      };

  public static String breakdown(Task<?>... taskList) {
    return breakdown(BY_START_TIME, taskList);
  }

  public static String breakdown(Comparator<? super Task<?>> taskOrdering, Task<?>... taskList) {
    HashSet<Task<?>> all = new HashSet<>(Arrays.asList(taskList));
    LinkedHashSet<Task<?>> tasks =
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
            .sorted(taskOrdering)
            .collect(Collectors.toCollection(LinkedHashSet::new));

    List<String[]> lines = new ArrayList<String[]>();
    lines.add(new String[] {"[Task]", "[Time]", "[%]", "[+T₀]"});

    long total = 0;
    long t0 = Long.MAX_VALUE;
    for (Task<?> task : tasks) {
      if (task.hasTracker()) {
        t0 = Math.min(t0, task.getTracker().startTime());
        if (task.isDone()) {
          total += task.getTracker().elapsedMillis();
        }
      }
    }

    for (Task<?> task : tasks) {
      breakdownTask(lines, 0, task, taskOrdering, total, t0);
    }

    int[] widths = new int[lines.get(0).length];
    for (String[] line : lines) {
      for (int c = 0; c < line.length; c++) {
        widths[c] = Math.max(line[c].length(), widths[c]);
      }
    }

    StringBuilder b = new StringBuilder();
    try (Formatter fmt = new Formatter(b, Locale.ROOT)) {
      String pattern =
          "%-" + widths[0] + "s  " + "%" + widths[1] + "s  " + "%" + widths[2] + "s  " + "%"
              + widths[3] + "s\n";
      for (String[] line : lines) {
        fmt.format(pattern, (Object[]) line);
      }
    }
    return b.toString();
  }

  private static void breakdownTask(
      List<String[]> lines,
      int indent,
      Task<?> task,
      Comparator<? super Task<?>> taskOrdering,
      long total,
      long t0) {
    String padding = repeat(indent, "  ");

    lines.add(
        new String[] {
          padding + taskName(task),
          taskTime(task),
          taskTimeFraction(task, total),
          taskTimeT0(task, t0)
        });

    for (Attribute a : task.attributes()) {
      lines.add(
          new String[] {
            String.format(Locale.ROOT, "%s @ %s: %s", padding, a.key, a.value), "", "", ""
          });
    }

    task.subtasks().stream()
        .sorted(taskOrdering)
        .forEachOrdered(
            (subtask) -> {
              breakdownTask(lines, indent + 1, subtask, taskOrdering, total, t0);
            });
  }

  private static String taskTimeT0(Task<?> task, long t0) {
    if (task.hasTracker()) {
      return Units.DURATION_COMPACT.format(task.getTracker().startTime() - t0);
    } else {
      return "";
    }
  }

  private static String taskTimeFraction(Task<?> task, long total) {
    if (task.isDone() && task.hasTracker() && total > 0) {
      long elapsed = task.getTracker().elapsedMillis();
      double percent = 100.0d * elapsed / total;
      if (percent > 100.0d) {
        return ">100%?";
      } else {
        return String.format(Locale.ROOT, "%4.1f%%", percent);
      }
    } else {
      return "";
    }
  }

  private static String taskTime(Task<?> task) {
    if (task.isDone() && task.hasTracker()) {
      return Units.DURATION.format(task.getTracker().elapsedMillis());
    } else {
      return "[" + task.getStatus().name() + "]";
    }
  }

  private static String taskName(Task<?> task) {
    return task.hasName() ? task.getName() : "<unnamed>";
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
