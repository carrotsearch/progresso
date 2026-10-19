package com.carrotsearch.progresso.views.console;

/**
 * A source of the console width, in columns. Implementations are discovered with {@link
 * java.util.ServiceLoader} and consulted from the highest {@link #priority()} to the lowest; the
 * first one that {@link #isSupported() is supported} provides the width.
 *
 * @see ConsoleAware#consoleWidth()
 */
public interface ConsoleWidthSupplier {
  /** Priority of an explicit, user-provided setting (the {@code console.width} property). */
  int PRIORITY_EXPLICIT = Integer.MAX_VALUE;

  /** Priority of suppliers asking the operating system or the terminal directly. */
  int PRIORITY_NATIVE = 100;

  /** Priority of suppliers relying on environment heuristics (such as {@code COLUMNS}). */
  int PRIORITY_ENVIRONMENT = 50;

  /** Default priority. */
  int PRIORITY_DEFAULT = 0;

  /**
   * @return Returns true if this supplier can provide the width in the current environment. May be
   *     expensive; suppliers with a higher priority are consulted first, so this is called only if
   *     none of them was supported.
   */
  boolean isSupported();

  /**
   * @return Returns the console width, in columns (at least 1). Called only if {@link
   *     #isSupported()} returned true.
   */
  int getConsoleWidth();

  /**
   * @return Returns the priority of this supplier. Suppliers with a higher priority are consulted
   *     first; ties keep the service loading order.
   */
  default int priority() {
    return PRIORITY_DEFAULT;
  }
}
