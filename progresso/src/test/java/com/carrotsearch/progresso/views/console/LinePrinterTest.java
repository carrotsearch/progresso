package com.carrotsearch.progresso.views.console;

import com.carrotsearch.randomizedtesting.RandomizedTest;
import java.util.ArrayList;
import java.util.List;
import org.assertj.core.api.Assertions;
import org.junit.Test;

public class LinePrinterTest extends RandomizedTest {
  @Test
  public void installedPrinterReceivesLinesUntilReleased() throws Exception {
    List<String> seen = new ArrayList<>();
    LinePrinter printer = seen::add;

    ConsoleAware.installLinePrinter(printer);
    try {
      ConsoleAware.printLine("routed");
      // Releasing another printer leaves the installed one in place.
      ConsoleAware.releaseLinePrinter(line -> {});
      ConsoleAware.printLine("still routed");
    } finally {
      ConsoleAware.releaseLinePrinter(printer);
    }
    ConsoleAware.printLine("to the console writer");

    Assertions.assertThat(seen).containsExactly("routed", "still routed");
  }
}
