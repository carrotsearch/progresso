package com.carrotsearch.progresso.views.console;

import com.carrotsearch.progresso.Task;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/** A "quiet" console view that only reports completed tasks, no partial progress reports. */
public class QuietConsoleView extends AbstractConsoleView {
  private final Writer out;
  private final int lineWidth;

  public QuietConsoleView(ConsoleWriter out) {
    this(out, Collections.emptyList());
  }

  public QuietConsoleView(ConsoleWriter out, Collection<Task<?>> topTasks) {
    this(out, out.lineWidth() - 1, topTasks);
  }

  public QuietConsoleView(Writer out, int lineWidth, Collection<Task<?>> topTasks) {
    this(out, lineWidth, topTasks, defaultFormatters());
  }

  public QuietConsoleView(
      Writer out,
      int lineWidth,
      Collection<Task<?>> topTasks,
      List<? extends TrackerFormatter> formatters) {
    super(topTasks, formatters);
    this.lineWidth = lineWidth;
    this.out = out;
  }

  @Override
  protected void taskCompletedUpdate(Task<?> t) {
    if (topTasks.isEmpty() || topTasks.contains(t)) {
      if (t.isDone()) {
        printLine(formatStatusLine(t));
      }
    }
  }

  @Override
  protected void taskUpdate(Task<?> t) {
    // Do nothing for partial updates.
  }

  private void printLine(String s) {
    try {
      out.write(s);
      out.write(LF);
      out.flush();
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  private String formatStatusLine(Task<?> t) {
    return formatStatusLine(t, lineWidth);
  }
}
