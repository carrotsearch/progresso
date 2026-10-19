package com.carrotsearch.progresso.views.console;

import com.carrotsearch.randomizedtesting.RandomizedTest;
import java.util.ServiceLoader;
import org.assertj.core.api.Assertions;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class ConsoleWidthSupplierTest extends RandomizedTest {
  private String savedProperty;

  @Before
  public void reset() {
    savedProperty = System.getProperty(ConsoleAware.CONSOLE_WIDTH_PROPERTY);
    System.clearProperty(ConsoleAware.CONSOLE_WIDTH_PROPERTY);
    FakeWidthSuppliers.widths.clear();
  }

  @After
  public void restore() {
    if (savedProperty == null) {
      System.clearProperty(ConsoleAware.CONSOLE_WIDTH_PROPERTY);
    } else {
      System.setProperty(ConsoleAware.CONSOLE_WIDTH_PROPERTY, savedProperty);
    }
    FakeWidthSuppliers.widths.clear();
  }

  @Test
  public void highestSupportedPriorityWins() {
    FakeWidthSuppliers.widths.put("Low", 10);
    FakeWidthSuppliers.widths.put("Mid", 30);
    // High stays unsupported.
    Assertions.assertThat(ConsoleAware.consoleWidthSupplier())
        .containsInstanceOf(FakeWidthSuppliers.Mid.class);
    Assertions.assertThat(ConsoleAware.consoleWidth()).isEqualTo(30);

    FakeWidthSuppliers.widths.put("High", 20);
    Assertions.assertThat(ConsoleAware.consoleWidth()).isEqualTo(20);
  }

  @Test
  public void tiesKeepLoadingOrder() {
    FakeWidthSuppliers.widths.put("MidToo", 2);
    FakeWidthSuppliers.widths.put("Mid", 1);
    Assertions.assertThat(ConsoleAware.consoleWidthSupplier())
        .containsInstanceOf(FakeWidthSuppliers.Mid.class);
  }

  @Test
  public void defaultHintWhenNothingSupported() {
    Assertions.assertThat(ConsoleAware.consoleWidthSupplier()).isEmpty();
    Assertions.assertThat(ConsoleAware.consoleWidth())
        .isEqualTo(ConsoleAware.CONSOLE_WIDTH_DEFAULT);
  }

  @Test
  public void defaultPriority() {
    ConsoleWidthSupplier supplier =
        new ConsoleWidthSupplier() {
          @Override
          public boolean isSupported() {
            return true;
          }

          @Override
          public int getConsoleWidth() {
            return 1;
          }
        };
    Assertions.assertThat(supplier.priority()).isEqualTo(ConsoleWidthSupplier.PRIORITY_DEFAULT);
  }

  @Test
  public void systemPropertySupplierIsRegisteredWithTopPriority() {
    Assertions.assertThat(ServiceLoader.load(ConsoleWidthSupplier.class))
        .anyMatch(
            s ->
                s instanceof ConsoleWidthFromSystemProperty
                    && s.priority() == ConsoleWidthSupplier.PRIORITY_EXPLICIT);
  }

  @Test
  public void systemPropertyOverridesEverything() {
    FakeWidthSuppliers.widths.put("High", 20);
    System.setProperty(ConsoleAware.CONSOLE_WIDTH_PROPERTY, " 132 ");
    Assertions.assertThat(ConsoleAware.consoleWidthSupplier())
        .containsInstanceOf(ConsoleWidthFromSystemProperty.class);
    Assertions.assertThat(ConsoleAware.consoleWidth()).isEqualTo(132);
  }

  @Test
  public void invalidSystemPropertyIsAnError() {
    for (String bad : new String[] {"wide", "0", "-5", ""}) {
      System.setProperty(ConsoleAware.CONSOLE_WIDTH_PROPERTY, bad);
      Assertions.assertThatThrownBy(ConsoleAware::consoleWidth)
          .as("console.width=%s", bad)
          .isInstanceOf(IllegalArgumentException.class);
    }
  }
}
