package com.carrotsearch.progresso.jline;

import java.io.IOException;
import java.io.UncheckedIOException;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;

/**
 * The process's JLine system terminal, shared by everything in progresso that needs it. JLine
 * attaches to the system terminal once: a second attempt while the first is open yields a dumb
 * terminal, so the views and the width supplier must use the same instance. It is closed when the
 * JVM exits.
 */
public final class JLineTerminals {
  private static Terminal system;

  private JLineTerminals() {}

  /**
   * Returns the shared system terminal, opening it on first use. Without an interactive terminal
   * (redirected streams, test runners) this is a dumb terminal (type {@code dumb}), without JLine's
   * warning about it.
   */
  public static synchronized Terminal system() throws IOException {
    if (system == null) {
      Terminal terminal =
          TerminalBuilder.builder().system(true).dumb(true).nativeSignals(false).build();
      Runtime.getRuntime()
          .addShutdownHook(
              new Thread(
                  () -> {
                    try {
                      terminal.close();
                    } catch (IOException e) {
                      throw new UncheckedIOException(e);
                    }
                  },
                  "progresso-jline terminal close"));
      system = terminal;
    }
    return system;
  }

  /** Returns true if the terminal is a real one, not JLine's fallback for redirected streams. */
  public static boolean isInteractive(Terminal terminal) {
    return !terminal.getType().startsWith("dumb");
  }
}
