package com.carrotsearch.progresso.views.console;

import com.carrotsearch.progresso.Task;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * A progress view which assumes the terminal supports "backtracking" (carriage return) properly.
 */
public class UpdateableConsoleView extends AbstractConsoleView {
  private static final long DEFAULT_UPDATE_INTERVAL = TimeUnit.SECONDS.toMillis(1);

  private long nextUpdate = 0L;
  private long modHash = 0L;

  private final ConsoleWriter consoleWriter;

  public UpdateableConsoleView(ConsoleWriter out, Collection<Task<?>> topTasks) {
    this(out, topTasks, defaultFormatters());
  }

  public UpdateableConsoleView(ConsoleWriter out) {
    this(out, Collections.emptyList());
  }

  public UpdateableConsoleView(
      ConsoleWriter out,
      Collection<Task<?>> topTasks,
      List<? extends TrackerFormatter> formatters) {
    super(topTasks, formatters);
    this.consoleWriter = out;
  }

  protected void setActiveTask(Task<?> task) {
    super.setActiveTask(task);
    if (task == null) {
      modHash = 0L;
    } else {
      modHash = ~task.getTracker().modHash();
    }
  }

  protected void taskCompletedUpdate(Task<?> t) {
    try {
      // Proceed to a new line on top-level tasks.
      if (topTasks.isEmpty() || topTasks.contains(t)) {
        consoleWriter.printLine(formatStatusLine(t));
      } else {
        consoleWriter.updateLine(formatStatusLine(t));
      }
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  protected void taskUpdate(Task<?> t) {
    // When there's no change to task status, and we haven't reached
    // the update interval, skip the update.
    long modHash = t.getTracker().modHash();
    if (this.modHash == modHash && nowMillis() < nextUpdate) {
      return;
    }

    this.modHash = modHash;
    nextUpdate = nowMillis() + DEFAULT_UPDATE_INTERVAL;

    try {
      consoleWriter.updateLine(formatStatusLine(t));
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  private String formatStatusLine(Task<?> t) {
    // Leave space for cursor. Certain terminals make an automatic next line
    // feed if cursor doesn't fit.
    int lineWidth = consoleWriter.lineWidth() - 1;
    return formatStatusLine(t, lineWidth);
  }
}
