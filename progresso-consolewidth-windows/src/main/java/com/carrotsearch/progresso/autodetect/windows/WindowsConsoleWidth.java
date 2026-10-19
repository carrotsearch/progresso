package com.carrotsearch.progresso.autodetect.windows;

import com.carrotsearch.progresso.util.OsDetection;
import com.carrotsearch.progresso.views.console.ConsoleWidthSupplier;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Console width on Windows, read from the console's screen buffer through {@link
 * WindowsConsoleSize}. The screen buffer width is reported (not the visible window's), because that
 * is where the console wraps lines.
 *
 * <p>Not used under Cygwin/MSYS terminals, where the {@code COLUMNS} environment variable is the
 * right source, and inactive unless native access is enabled for the JVM ({@code
 * --enable-native-access=ALL-UNNAMED}), in which case the reason is logged at debug level.
 */
public final class WindowsConsoleWidth implements ConsoleWidthSupplier {
  private static final Logger LOG = Logger.getLogger(WindowsConsoleWidth.class.getName());

  /** Width detected by {@link #isSupported()}, reused by {@link #getConsoleWidth()}; 0 if none. */
  private int width;

  @Override
  public boolean isSupported() {
    if (!OsDetection.IS_OS_WINDOWS || OsDetection.IS_CYGWIN) {
      return false;
    }

    if (!WindowsConsoleWidth.class.getModule().isNativeAccessEnabled()) {
      LOG.fine(
          "Native access is not enabled for this JVM, console width not detected"
              + " (start with --enable-native-access=ALL-UNNAMED).");
      return false;
    }

    width = detectWidth();
    return width > 0;
  }

  @Override
  public int priority() {
    return PRIORITY_NATIVE;
  }

  @Override
  public int getConsoleWidth() {
    if (width <= 0) {
      width = detectWidth();
    }
    return width;
  }

  /**
   * @return The console's screen buffer width or 0 if there is no console or its size could not be
   *     acquired.
   */
  static int detectWidth() {
    try {
      WindowsConsoleSize.Info info = WindowsConsoleSize.detect();
      if (info == null) {
        LOG.fine("No console attached, console width not detected.");
        return 0;
      }

      LOG.log(
          Level.FINE,
          "Auto-detected console size: buffer={0}x{1}, window={2}x{3}.",
          new Object[] {
            info.bufferWidth(), info.bufferHeight(), info.windowWidth(), info.windowHeight()
          });
      return info.bufferWidth();
    } catch (Throwable t) {
      // Never fail because of this, but report the problem.
      LOG.log(Level.WARNING, "Could not acquire console size: {0}", t.toString());
      return 0;
    }
  }
}
