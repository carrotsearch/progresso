package com.carrotsearch.progresso.views.console;

import com.carrotsearch.progresso.ProgressView;
import com.carrotsearch.progresso.Task;
import java.util.Collection;

/**
 * Supplies {@link UpdateableConsoleView} when the console {@link ConsoleAware#isUpdateable() is
 * updateable}: an interactive terminal, or the {@link ConsoleAware#CONSOLE_UPDATEABLE_PROPERTY}
 * system property says so.
 */
public final class UpdateableConsoleViewSupplier implements ConsoleViewSupplier {
  @Override
  public boolean isSupported() {
    return ConsoleAware.isUpdateable();
  }

  @Override
  public ProgressView newConsoleProgressView(Collection<Task<?>> topTasks) {
    return new UpdateableConsoleView(ConsoleAware.writer(), topTasks);
  }

  @Override
  public int priority() {
    return PRIORITY_UPDATEABLE;
  }
}
