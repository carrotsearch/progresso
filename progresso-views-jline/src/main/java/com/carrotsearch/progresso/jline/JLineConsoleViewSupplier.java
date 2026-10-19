package com.carrotsearch.progresso.jline;

import com.carrotsearch.progresso.ProgressView;
import com.carrotsearch.progresso.Task;
import com.carrotsearch.progresso.views.console.ConsoleAware;
import com.carrotsearch.progresso.views.console.ConsoleViewSupplier;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Collection;

/**
 * Supplies {@link JLineConsoleView} when stdout is an interactive terminal (see {@link
 * JLineConsoleView#isSupported()}), unless the {@link ConsoleAware#CONSOLE_UPDATEABLE_PROPERTY}
 * system property is explicitly {@code false}. Ranks above the core's views.
 */
public final class JLineConsoleViewSupplier implements ConsoleViewSupplier {
  @Override
  public boolean isSupported() {
    String updateable = System.getProperty(ConsoleAware.CONSOLE_UPDATEABLE_PROPERTY);
    if (updateable != null && !Boolean.parseBoolean(updateable)) {
      return false;
    }
    return JLineConsoleView.isSupported();
  }

  @Override
  public ProgressView newConsoleProgressView(Collection<Task<?>> topTasks) {
    try {
      return JLineConsoleView.open(topTasks);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  @Override
  public int priority() {
    return PRIORITY_TERMINAL;
  }
}
