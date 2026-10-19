package com.carrotsearch.progresso.views.console;

import com.carrotsearch.randomizedtesting.RandomizedTest;
import java.util.ServiceLoader;
import org.assertj.core.api.Assertions;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class ConsoleWidthFromSystemPropertyTest extends RandomizedTest {
  private String savedProperty;

  @Before
  public void clearProperty() {
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

  @Test
  public void registeredWithTopPriority() {
    Assertions.assertThat(ServiceLoader.load(ConsoleWidthSupplier.class))
        .anyMatch(
            s ->
                s instanceof ConsoleWidthFromSystemProperty
                    && s.priority() == ConsoleWidthSupplier.PRIORITY_EXPLICIT);
  }

  @Test
  public void propertySetsTheWidth() {
    Assertions.assertThat(ConsoleAware.consoleWidthSupplier()).isEmpty();
    Assertions.assertThat(ConsoleAware.consoleWidth())
        .isEqualTo(ConsoleAware.CONSOLE_WIDTH_DEFAULT);

    System.setProperty(ConsoleAware.CONSOLE_WIDTH_PROPERTY, " 132 ");
    Assertions.assertThat(ConsoleAware.consoleWidthSupplier())
        .containsInstanceOf(ConsoleWidthFromSystemProperty.class);
    Assertions.assertThat(ConsoleAware.consoleWidth()).isEqualTo(132);
  }

  @Test
  public void invalidPropertyIsAnError() {
    for (String bad : new String[] {"wide", "0", "-5", ""}) {
      System.setProperty(ConsoleAware.CONSOLE_WIDTH_PROPERTY, bad);
      Assertions.assertThatThrownBy(ConsoleAware::consoleWidth)
          .as("console.width=%s", bad)
          .isInstanceOf(IllegalArgumentException.class);
    }
  }
}
