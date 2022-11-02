package com.carrotsearch.progresso.views.console;

import com.carrotsearch.progresso.Task;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

/** A very basic progress view emitting incremental output, line by line. */
public class PlainConsoleView extends AbstractConsoleView {
  private static final long DELAY_INITIAL = TimeUnit.SECONDS.toMillis(2);
  private static final long DELAY_PROGRESS = TimeUnit.SECONDS.toMillis(2);

  private long nextUpdate = 0L;

  private final Writer out;
  private final int lineWidth;

  public PlainConsoleView(ConsoleWriter out) {
    this(out, Collections.emptyList());
  }

  public PlainConsoleView(ConsoleWriter out, Collection<Task<?>> topTasks) {
    this(out, out.lineWidth(), topTasks);
  }

  public PlainConsoleView(Writer out, int lineWidth, Collection<Task<?>> topTasks) {
    this(out, lineWidth, topTasks, defaultFormatters());
  }

  public PlainConsoleView(
      Writer out,
      int lineWidth,
      Collection<Task<?>> topTasks,
      List<? extends TrackerFormatter> formatters) {
    super(topTasks, formatters);
    this.lineWidth = lineWidth;
    this.out = out;
  }

  protected void setActiveTask(Task<?> task) {
    super.setActiveTask(task);
    if (task == null) {
      nextUpdate = 0L;
    } else {
      nextUpdate = now() + DELAY_INITIAL;
    }
  }

  protected void taskCompletedUpdate(Task<?> t) {
    printLine(formatStatusLine(t));
  }

  protected void taskUpdate(Task<?> t) {
    if (now() < nextUpdate) {
      return;
    }

    nextUpdate = now() + DELAY_PROGRESS;
    printLine(formatStatusLine(t));
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
