package com.carrotsearch.progresso.autodetect.windows;

import com.carrotsearch.progresso.util.OsDetection;
import com.carrotsearch.progresso.views.console.ConsoleAware;
import com.carrotsearch.progresso.views.console.ConsoleWidthSupplier;
import com.carrotsearch.randomizedtesting.RandomizedTest;
import java.util.ServiceLoader;
import org.assertj.core.api.Assertions;
import org.junit.Assume;
import org.junit.Test;

public class WindowsConsoleWidthTest extends RandomizedTest {
  @Test
  public void providerIsRegistered() {
    Assertions.assertThat(ServiceLoader.load(ConsoleWidthSupplier.class))
        .anyMatch(supplier -> supplier instanceof WindowsConsoleWidth);
  }

  @Test
  public void notSupportedOutsideWindows() {
    Assume.assumeFalse(OsDetection.IS_OS_WINDOWS);
    Assertions.assertThat(new WindowsConsoleWidth().isSupported()).isFalse();
  }

  @Test
  public void detectionOnWindows() throws Throwable {
    Assume.assumeTrue(OsDetection.IS_OS_WINDOWS);
    Assertions.assertThat(getClass().getModule().isNativeAccessEnabled())
        .as("tests must run with --enable-native-access")
        .isTrue();

    // Either there is no console attached to the test JVM (null) or the size is sane. Neither
    // outcome may throw.
    WindowsConsoleSize.Info info = WindowsConsoleSize.detect();
    System.out.println("Console info: " + info);
    if (info != null) {
      Assertions.assertThat(info.bufferWidth()).isPositive();
      Assertions.assertThat(info.windowWidth())
          .isPositive()
          .isLessThanOrEqualTo(info.bufferWidth());
    }

    WindowsConsoleWidth supplier = new WindowsConsoleWidth();
    if (!OsDetection.IS_CYGWIN) {
      Assertions.assertThat(supplier.isSupported()).isEqualTo(info != null);
    }
    if (supplier.isSupported()) {
      Assertions.assertThat(supplier.getConsoleWidth()).isEqualTo(info.bufferWidth());
      Assertions.assertThat(ConsoleAware.consoleWidth()).isEqualTo(info.bufferWidth());
    }
    System.out.println("ConsoleAware.consoleWidth(): " + ConsoleAware.consoleWidth());
  }
}
