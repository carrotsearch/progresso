package com.carrotsearch.progresso.autodetect;

import java.security.AccessController;
import java.security.PrivilegedAction;

import org.slf4j.LoggerFactory;

import com.carrotsearch.progresso.util.OsDetection;
import com.carrotsearch.progresso.views.console.ConsoleWidthSupplier;

/**
 * Get the console width from the COLUMNS environment variable, if it's set.
 */
public final class UnixishWidthFromColumnsEnvVariable implements ConsoleWidthSupplier {
  @Override
  public boolean isSupported() {
    return (OsDetection.IS_OS_UNIXISH || OsDetection.IS_CYGWIN) 
        && AccessController.doPrivileged((PrivilegedAction<Boolean>) () -> {
          try {
            String columns = System.getenv().get("COLUMNS");
            return columns != null && Integer.parseInt(columns) > 0;
          } catch (NumberFormatException e) {
            return false;
          }
        });
  }

  public int getConsoleWidth() {
    return AccessController.doPrivileged((PrivilegedAction<Integer>) () -> {
      int columns = Integer.parseInt(System.getenv().get("COLUMNS"));
      LoggerFactory.getLogger(getClass())
        .debug("Console width from COLUMNS environment variable: " + columns);
      return columns;
    });
  }
}
