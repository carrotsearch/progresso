package com.carrotsearch.progresso.jline;

import java.io.IOException;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;

/** Opens JLine terminals the way progresso wants them. */
public final class JLineTerminals {
  private JLineTerminals() {}

  /**
   * Opens the system terminal. Falls back to a dumb terminal (type {@code dumb}) when the process
   * has no interactive terminal, without JLine's warning about it.
   */
  public static Terminal openSystem() throws IOException {
    return TerminalBuilder.builder().system(true).dumb(true).nativeSignals(false).build();
  }

  /** Returns true if the terminal is a real one, not JLine's fallback for redirected streams. */
  public static boolean isInteractive(Terminal terminal) {
    return !terminal.getType().startsWith("dumb");
  }
}
