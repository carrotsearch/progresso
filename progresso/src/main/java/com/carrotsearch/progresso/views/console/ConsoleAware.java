package com.carrotsearch.progresso.views.console;

import com.carrotsearch.progresso.ProgressView;
import com.carrotsearch.progresso.Task;
import com.carrotsearch.progresso.annotations.SuppressForbidden;
import com.carrotsearch.progresso.util.ColumnCounter;
import java.io.Console;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.Charset;
import java.util.Collection;
import java.util.Collections;
import java.util.Locale;
import java.util.Objects;
import java.util.ServiceLoader;
import java.util.function.Predicate;

@SuppressForbidden("Legitimate use of System.out and properties.")
public class ConsoleAware {
  /** Sane default console with. We don't want to use native hooks to get it. */
  public static final int CONSOLE_WIDTH_DEFAULT = 80;

  public static final String CONSOLE_WIDTH_PROPERTY = "console.width";
  public static final String CONSOLE_UPDATEABLE_PROPERTY = "console.updateable";

  private static ConsoleWriter consoleWriter;
  private static int globalWidthHint = CONSOLE_WIDTH_DEFAULT;

  /**
   * Assume the terminal is updateable if system console is available and - on newer JDKs - the
   * isTerminal method returns true.
   */
  private static final Predicate<Console> consoleIsTerminal;

  static {
    Predicate<Console> predicate = null;
    try {
      // Only available on JDK22+.
      Method isTerminal = Console.class.getMethod("isTerminal");
      predicate =
          (console) -> {
            try {
              return console != null && (boolean) isTerminal.invoke(console);
            } catch (IllegalAccessException | InvocationTargetException e) {
              return false;
            }
          };
    } catch (NoSuchMethodException e) {
      // ignore.
    }

    if (predicate == null) {
      predicate = Objects::nonNull;
    }

    consoleIsTerminal = predicate;
  }

  public static ProgressView newConsoleProgressView() {
    return newConsoleProgressView(Collections.emptyList());
  }

  public static ProgressView newConsoleProgressView(Collection<Task<?>> topLevelSteps) {
    if (isUpdateable()) {
      return new UpdateableConsoleView(writer(), topLevelSteps);
    } else {
      return new PlainConsoleView(writer(), topLevelSteps);
    }
  }

  public static boolean isUpdateable() {
    boolean consoleUpdateable;
    if (System.getProperty(CONSOLE_UPDATEABLE_PROPERTY) != null) {
      consoleUpdateable = Boolean.parseBoolean(System.getProperty(CONSOLE_UPDATEABLE_PROPERTY));
    } else {
      consoleUpdateable = consoleIsTerminal.test(System.console());
    }
    return consoleUpdateable;
  }

  /** Returns the console writer. */
  public static synchronized ConsoleWriter writer() {
    if (consoleWriter == null) {
      Console console = System.console();
      Writer writer;
      if (console != null) {
        writer = console.writer();
      } else {
        writer = new OutputStreamWriter(System.out, Charset.defaultCharset());
      }

      consoleWriter = new ConsoleWriter(writer, ColumnCounter.DEFAULT, consoleWidth());
    }

    return consoleWriter;
  }

  /**
   * @return Returns current console width based on the global hint or user property override.
   */
  public static int consoleWidth() throws IllegalArgumentException {
    // The sane default.
    int width = globalWidthHint;

    String manual = System.getProperty(CONSOLE_WIDTH_PROPERTY);
    if (manual != null) {
      width = Integer.parseInt(manual);
      checkWidth(width);
    } else {
      ServiceLoader<ConsoleWidthSupplier> loader = ServiceLoader.load(ConsoleWidthSupplier.class);
      for (ConsoleWidthSupplier supplier : loader) {
        if (supplier.isSupported()) {
          width = supplier.getConsoleWidth();
          break;
        }
      }
    }

    return width;
  }

  /**
   * Provide the default console width hint (in columns).
   *
   * @throws IllegalArgumentException If console width &lt; 1.
   */
  public static void setDefaultConsoleWidth(int widthHint) throws IllegalArgumentException {
    checkWidth(widthHint);
    globalWidthHint = widthHint;
  }

  static void checkWidth(int widthHint) {
    if (widthHint < 1) {
      throw new IllegalArgumentException("Console width hint must be >= 1: " + widthHint);
    }
  }

  /** Prints console information to command-line. */
  public static void main(String[] args) throws IOException {
    try (ConsoleWriter w = ConsoleAware.writer()) {
      w.printLine(
          String.format(
              Locale.ROOT,
              "Console %s, width: %s.",
              ConsoleAware.isUpdateable() ? "is updateable" : "is not updateable",
              ConsoleAware.consoleWidth()));
    }
  }
}
