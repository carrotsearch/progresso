package com.carrotsearch.progresso.views.console;

import com.carrotsearch.randomizedtesting.RandomizedTest;
import java.io.ByteArrayOutputStream;
import java.io.Console;
import java.io.PrintStream;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import org.assertj.core.api.Assertions;
import org.junit.Assume;
import org.junit.Test;

public class ConsoleAwareWriterTest extends RandomizedTest {
  /**
   * When the output is not an interactive terminal (always the case under a test runner), progress
   * output must go through {@link System#out} so that whoever captures it sees it. This matters on
   * JDK 22+, where {@link System#console()} is non-null for redirected output and its writer
   * bypasses {@link System#setOut}.
   */
  @Test
  public void redirectedOutputGoesThroughSystemOut() throws Exception {
    Console console = System.console();
    Assume.assumeFalse(
        "Running in an interactive terminal.",
        console != null && !"java.io.ProxyingConsole".equals(console.getClass().getName()));

    PrintStream original = System.out;
    ByteArrayOutputStream captured = new ByteArrayOutputStream();
    System.setOut(new PrintStream(captured, true, StandardCharsets.UTF_8));
    try {
      Writer writer = ConsoleAware.outputWriter();
      writer.write("captured \u2588\u258d");
      writer.flush();
    } finally {
      System.setOut(original);
    }

    Assertions.assertThat(captured.toString(ConsoleAware.outputCharset()))
        .isEqualTo("captured \u2588\u258d");
  }
}
