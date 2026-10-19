package com.carrotsearch.progresso.views.console;

import com.carrotsearch.progresso.annotations.SuppressForbidden;

/**
 * Console width set explicitly with the {@link ConsoleAware#CONSOLE_WIDTH_PROPERTY} system
 * property. Has the highest priority, so an explicit setting always wins over detection.
 */
@SuppressForbidden("Legitimate use of system properties.")
public final class ConsoleWidthFromSystemProperty implements ConsoleWidthSupplier {
  @Override
  public boolean isSupported() {
    return System.getProperty(ConsoleAware.CONSOLE_WIDTH_PROPERTY) != null;
  }

  /**
   * @throws IllegalArgumentException if the property is not a positive integer; an explicit but
   *     invalid setting is an error, not a reason to fall back to detection.
   */
  @Override
  public int getConsoleWidth() {
    String value = System.getProperty(ConsoleAware.CONSOLE_WIDTH_PROPERTY);
    final int width;
    try {
      width = Integer.parseInt(value.trim());
    } catch (NumberFormatException e) {
      throw new IllegalArgumentException(
          "Not an integer in the " + ConsoleAware.CONSOLE_WIDTH_PROPERTY + " property: " + value);
    }
    ConsoleAware.checkWidth(width);
    return width;
  }

  @Override
  public int priority() {
    return PRIORITY_EXPLICIT;
  }
}
