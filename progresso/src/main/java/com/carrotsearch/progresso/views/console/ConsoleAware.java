package com.carrotsearch.progresso.views.console;

import java.io.Console;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.util.Collection;
import java.util.ServiceLoader;

import com.carrotsearch.progresso.ProgressView;
import com.carrotsearch.progresso.Task;
import com.carrotsearch.progresso.annotations.SuppressForbidden;
import com.carrotsearch.progresso.util.ColumnCounter;

@SuppressForbidden("Legitimate use of System.out and properties.")
public class ConsoleAware {
  /**
   * Sane default console with. We don't want to use native hooks to get it.
   */
  public static final int CONSOLE_WIDTH_DEFAULT = 80;

  public static final String CONSOLE_WIDTH_PROPERTY = "console.width";
  public static final String CONSOLE_UPDATEABLE_PROPERTY = "console.updateable";

  private static ConsoleWriter consoleWriter;
  private static int globalWidthHint = CONSOLE_WIDTH_DEFAULT;

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
      // Assume the terminal is updateable if system console is available.
      consoleUpdateable = System.console() != null;
    }
    return consoleUpdateable;
  }

  /**
   * Returns the console writer. 
   */
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
   * @return Returns current console width based on the global hint or user property
   * override. 
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
   * @throws IllegalArgumentException If console width < 1.
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
}
