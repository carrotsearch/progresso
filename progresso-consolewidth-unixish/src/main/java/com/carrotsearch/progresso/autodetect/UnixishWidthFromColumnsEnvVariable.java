package com.carrotsearch.progresso.autodetect;

import com.carrotsearch.progresso.util.OsDetection;
import com.carrotsearch.progresso.views.console.ConsoleWidthSupplier;
import java.util.logging.Level;
import java.util.logging.Logger;

/** Get the console width from the COLUMNS environment variable, if it's set. */
public final class UnixishWidthFromColumnsEnvVariable implements ConsoleWidthSupplier {
  @Override
  public boolean isSupported() {
    if (!(OsDetection.IS_OS_UNIXISH || OsDetection.IS_CYGWIN)) {
      return false;
    }

    try {
      String columns = System.getenv("COLUMNS");
      return columns != null && Integer.parseInt(columns) > 0;
    } catch (NumberFormatException e) {
      return false;
    }
  }

  @Override
  public int priority() {
    return PRIORITY_ENVIRONMENT;
  }

  @Override
  public int getConsoleWidth() {
    int columns = Integer.parseInt(System.getenv("COLUMNS"));
    Logger.getLogger(getClass().getName())
        .log(Level.FINE, "Console width from COLUMNS environment variable: {0}", columns);
    return columns;
  }
}
