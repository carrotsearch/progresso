package com.carrotsearch.progresso.views.console;

import com.carrotsearch.randomizedtesting.RandomizedTest;
import com.carrotsearch.randomizedtesting.rules.SystemPropertiesRestoreRule;
import org.assertj.core.api.Assertions;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;

public class ConsoleViewSupplierTest extends RandomizedTest {
  @Rule public final TestRule restoreProperties = new SystemPropertiesRestoreRule();

  @Test
  public void plainViewUnderTheTestRunner() {
    // No interactive console here: the updateable view is unsupported, the plain one always is.
    System.clearProperty(ConsoleAware.CONSOLE_UPDATEABLE_PROPERTY);
    Assertions.assertThat(ConsoleAware.consoleViewSupplier())
        .get()
        .isInstanceOf(PlainConsoleViewSupplier.class);
    Assertions.assertThat(ConsoleAware.newConsoleProgressView())
        .isInstanceOf(PlainConsoleView.class);
  }

  @Test
  public void updateableViewWhenForced() {
    System.setProperty(ConsoleAware.CONSOLE_UPDATEABLE_PROPERTY, "true");
    Assertions.assertThat(ConsoleAware.consoleViewSupplier())
        .get()
        .isInstanceOf(UpdateableConsoleViewSupplier.class);
    Assertions.assertThat(ConsoleAware.newConsoleProgressView())
        .isInstanceOf(UpdateableConsoleView.class);
  }
}
