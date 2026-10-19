package com.carrotsearch.progresso.jline;

import com.carrotsearch.progresso.views.console.ConsoleWidthSupplier;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jline.terminal.Terminal;

/**
 * Console width from JLine's system terminal, which asks the operating system directly (through
 * JLine's FFM provider, given native access) on every platform. Ranks above the platform-specific
 * suppliers, below an explicit setting.
 */
public final class JLineConsoleWidth implements ConsoleWidthSupplier {
  private static final Logger LOG = Logger.getLogger(JLineConsoleWidth.class.getName());

  private int width;

  @Override
  public boolean isSupported() {
    width = detectWidth();
    return width > 0;
  }

  @Override
  public int getConsoleWidth() {
    if (width <= 0) {
      width = detectWidth();
    }
    return width;
  }

  @Override
  public int priority() {
    return PRIORITY_NATIVE + 10;
  }

  /**
   * @return The terminal width in columns or 0 if there is no interactive terminal or JLine could
   *     not determine the size.
   */
  static int detectWidth() {
    try {
      Terminal terminal = JLineTerminals.system();
      if (!JLineTerminals.isInteractive(terminal)) {
        LOG.fine("No interactive terminal (JLine type: " + terminal.getType() + ").");
        return 0;
      }
      int columns = terminal.getSize().getColumns();
      LOG.log(
          Level.FINE,
          "JLine terminal {0} ({1}): {2} columns.",
          new Object[] {terminal.getName(), terminal.getType(), columns});
      return columns;
    } catch (IOException | RuntimeException e) {
      LOG.log(Level.WARNING, "Could not open the terminal: {0}", e.toString());
      return 0;
    }
  }
}
