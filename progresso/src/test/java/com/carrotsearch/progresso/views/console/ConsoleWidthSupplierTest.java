package com.carrotsearch.progresso.views.console;

import com.carrotsearch.randomizedtesting.RandomizedTest;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.ServiceLoader;
import org.assertj.core.api.Assertions;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class ConsoleWidthSupplierTest extends RandomizedTest {
  private String savedProperty;

  @Before
  public void saveProperty() {
    savedProperty = System.getProperty(ConsoleAware.CONSOLE_WIDTH_PROPERTY);
    System.clearProperty(ConsoleAware.CONSOLE_WIDTH_PROPERTY);
  }

  @After
  public void restoreProperty() {
    if (savedProperty == null) {
      System.clearProperty(ConsoleAware.CONSOLE_WIDTH_PROPERTY);
    } else {
      System.setProperty(ConsoleAware.CONSOLE_WIDTH_PROPERTY, savedProperty);
    }
  }

  private record Fixed(String name, int priority, boolean supported, int width)
      implements ConsoleWidthSupplier {
    @Override
    public boolean isSupported() {
      return supported;
    }

    @Override
    public int getConsoleWidth() {
      return width;
    }
  }

  @Test
  public void highestSupportedPriorityWins() {
    List<ConsoleWidthSupplier> suppliers =
        new ArrayList<>(
            List.of(
                new Fixed("low", 0, true, 10),
                new Fixed("high-unsupported", 100, false, 20),
                new Fixed("mid", 50, true, 30)));
    Optional<ConsoleWidthSupplier> selected = ConsoleAware.selectSupplier(suppliers);
    Assertions.assertThat(selected).map(ConsoleWidthSupplier::getConsoleWidth).hasValue(30);
  }

  @Test
  public void tiesKeepLoadingOrder() {
    List<ConsoleWidthSupplier> suppliers =
        new ArrayList<>(
            List.of(
                new Fixed("first", 5, true, 1),
                new Fixed("second", 5, true, 2),
                new Fixed("third", 5, true, 3)));
    Assertions.assertThat(ConsoleAware.selectSupplier(suppliers))
        .map(ConsoleWidthSupplier::getConsoleWidth)
        .hasValue(1);
  }

  @Test
  public void noneSupported() {
    List<ConsoleWidthSupplier> suppliers =
        new ArrayList<>(List.of(new Fixed("a", 1, false, 1), new Fixed("b", 2, false, 2)));
    Assertions.assertThat(ConsoleAware.selectSupplier(suppliers)).isEmpty();
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
    ConsoleWidthFromSystemProperty supplier = new ConsoleWidthFromSystemProperty();
    Assertions.assertThat(supplier.isSupported()).isFalse();

    System.setProperty(ConsoleAware.CONSOLE_WIDTH_PROPERTY, " 132 ");
    Assertions.assertThat(supplier.isSupported()).isTrue();
    Assertions.assertThat(supplier.getConsoleWidth()).isEqualTo(132);
    Assertions.assertThat(ConsoleAware.consoleWidth()).isEqualTo(132);
    Assertions.assertThat(ConsoleAware.consoleWidthSupplier())
        .containsInstanceOf(ConsoleWidthFromSystemProperty.class);
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

  @Test
  public void defaultHintWhenNothingSupported() {
    // In the core module's tests, only the property supplier is on the classpath.
    Assertions.assertThat(ConsoleAware.consoleWidthSupplier()).isEmpty();
    Assertions.assertThat(ConsoleAware.consoleWidth())
        .isEqualTo(ConsoleAware.CONSOLE_WIDTH_DEFAULT);
  }
}
