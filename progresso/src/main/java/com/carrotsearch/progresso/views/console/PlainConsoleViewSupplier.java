package com.carrotsearch.progresso.views.console;

import com.carrotsearch.progresso.ProgressView;
import com.carrotsearch.progresso.Task;
import java.util.Collection;

/** Supplies {@link PlainConsoleView}, which works with any output; the last resort. */
public final class PlainConsoleViewSupplier implements ConsoleViewSupplier {
  @Override
  public boolean isSupported() {
    return true;
  }

  @Override
  public ProgressView newConsoleProgressView(Collection<Task<?>> topTasks) {
    return new PlainConsoleView(ConsoleAware.writer(), topTasks);
  }

  @Override
  public int priority() {
    return PRIORITY_PLAIN;
  }
}
