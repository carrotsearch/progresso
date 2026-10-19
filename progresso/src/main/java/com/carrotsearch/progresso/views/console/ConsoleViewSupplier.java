package com.carrotsearch.progresso.views.console;

import com.carrotsearch.progresso.ProgressView;
import com.carrotsearch.progresso.Task;
import java.util.Collection;

/**
 * A source of console progress views. Implementations are discovered with {@link
 * java.util.ServiceLoader} and consulted from the highest {@link #priority()} to the lowest; the
 * first one that {@link #isSupported() is supported} provides the view. The core registers {@link
 * UpdateableConsoleViewSupplier} and {@link PlainConsoleViewSupplier}; other modules add views for
 * richer terminals.
 *
 * @see ConsoleAware#newConsoleProgressView()
 */
public interface ConsoleViewSupplier {
  /** Priority of views needing a terminal with capabilities (cursor movement, colors). */
  int PRIORITY_TERMINAL = 100;

  /** Priority of views redrawing in place on an interactive console. */
  int PRIORITY_UPDATEABLE = 50;

  /** Priority of views printing plain lines, which work anywhere. */
  int PRIORITY_PLAIN = 0;

  /**
   * @return Returns true if this supplier's view works in the current environment. May be
   *     expensive; suppliers with a higher priority are consulted first, so this is called only if
   *     none of them was supported.
   */
  boolean isSupported();

  /**
   * @param topTasks The top-level tasks, for the overall progress counter; may be empty.
   * @return Returns a new view. Called only if {@link #isSupported()} returned true.
   */
  ProgressView newConsoleProgressView(Collection<Task<?>> topTasks);

  /**
   * @return Returns the priority of this supplier. Suppliers with a higher priority are consulted
   *     first; ties keep the service loading order.
   */
  default int priority() {
    return PRIORITY_PLAIN;
  }
}
