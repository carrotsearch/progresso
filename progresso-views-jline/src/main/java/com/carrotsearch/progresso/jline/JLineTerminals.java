package com.carrotsearch.progresso.jline;

import com.carrotsearch.progresso.views.console.ConsoleAware;
import com.carrotsearch.progresso.views.console.LinePrinter;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayDeque;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.jline.terminal.TerminalBuilder.SystemOutput;
import org.jline.terminal.spi.SystemStream;
import org.jline.terminal.spi.TerminalProvider;

/**
 * The process's JLine system terminal, shared by everything in progresso that needs it. JLine
 * attaches to the system terminal once: a second attempt while the first is open yields a dumb
 * terminal, so the views and the width supplier must use the same instance. It is closed when the
 * JVM exits.
 */
public final class JLineTerminals {
  private static final Logger LOG = Logger.getLogger(JLineTerminals.class.getName());

  /** JLine's system property naming the terminal providers to try, in order. */
  private static final String PROVIDERS_PROPERTY = "org.jline.terminal.provider";

  /** JLine's default provider order. */
  private static final String PROVIDERS_DEFAULT = "ffm,jni,jansi,jna,exec";

  private static Terminal system;

  /**
   * Views with a live area on the system terminal, the most recently opened last. Only one view
   * draws at a time; printed lines go above the live area of the last one.
   */
  private static final ArrayDeque<LinePrinter> liveViews = new ArrayDeque<>();

  /**
   * The single printer installed in {@link ConsoleAware} for the system terminal: lines go to the
   * live view, if any, else straight to the terminal.
   */
  private static final LinePrinter printer =
      line -> {
        try {
          LinePrinter view;
          synchronized (liveViews) {
            view = liveViews.peekLast();
          }
          if (view != null) {
            view.printLine(line);
          } else {
            Terminal terminal = system();
            terminal.writer().println(line);
            terminal.flush();
          }
        } catch (IllegalStateException e) {
          // The terminal has been closed (JVM shutdown); fall through to the default writer.
          ConsoleAware.writer().printLine(line);
        }
      };

  private JLineTerminals() {}

  /** Makes the view the one that printed lines go through, until {@link #viewClosed}. */
  static void viewOpened(LinePrinter view) {
    synchronized (liveViews) {
      liveViews.removeIf(v -> v == view);
      liveViews.addLast(view);
    }
  }

  /** Lines go to the view opened before this one, or straight to the terminal. */
  static void viewClosed(LinePrinter view) {
    synchronized (liveViews) {
      liveViews.removeIf(v -> v == view);
    }
  }

  /** The printer installed for the system terminal (visible for tests). */
  static LinePrinter printer() {
    return printer;
  }

  /**
   * Returns the shared system terminal, opening it on first use. Without an interactive terminal
   * (redirected streams, test runners) this is a dumb terminal (type {@code dumb}), without JLine's
   * warning about it.
   */
  public static synchronized Terminal system() throws IOException {
    if (system == null) {
      Terminal terminal =
          TerminalBuilder.builder()
              .system(true)
              // Only stdout is the terminal: when it is redirected (a pipe, tee, a file) this
              // degrades to a dumb terminal instead of JLine's default of silently switching the
              // whole output to stderr if that still is a terminal.
              .systemOutput(SystemOutput.SysOut)
              .dumb(true)
              .nativeSignals(false)
              .build();
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
                  "progresso-views-jline terminal close"));
      system = terminal;
      if (isInteractive(terminal)) {
        // Console output (log lines) is routed through this terminal from now on, above the live
        // area of whichever view is open.
        ConsoleAware.installLinePrinter(printer);
      }
    }
    return system;
  }

  /** Returns true if the terminal is a real one, not JLine's fallback for redirected streams. */
  public static boolean isInteractive(Terminal terminal) {
    return !terminal.getType().startsWith("dumb");
  }

  /**
   * Returns the width of the terminal the process is attached to, asking the operating system about
   * stdout and then stderr; this works when stdout is redirected (a pipe, tee, a file) but stderr
   * still is the terminal, which is where a dumb {@link #system() system terminal} gets its width
   * from.
   *
   * @return The width in columns or 0 if neither stream is a terminal or no JLine provider can
   *     tell.
   */
  public static int systemStreamWidth() {
    String names = System.getProperty(PROVIDERS_PROPERTY, PROVIDERS_DEFAULT);
    for (String name : names.split(",")) {
      name = name.trim();
      if (name.isEmpty() || name.equals("dumb")) {
        continue;
      }
      try {
        TerminalProvider provider = TerminalProvider.load(name);
        for (SystemStream stream : List.of(SystemStream.Output, SystemStream.Error)) {
          if (provider.isSystemStream(stream)) {
            int columns = provider.systemStreamWidth(stream);
            LOG.log(
                Level.FINE,
                "Terminal width from {0} ({1}): {2} columns.",
                new Object[] {stream, name, columns});
            if (columns > 0) {
              return columns;
            }
          }
        }
        // The provider works, neither stream is a terminal.
        return 0;
      } catch (Throwable t) {
        // Provider missing or not working here (no native access, unsupported platform).
        LOG.log(Level.FINE, "Terminal provider {0} unavailable: {1}", new Object[] {name, t});
      }
    }
    return 0;
  }
}
