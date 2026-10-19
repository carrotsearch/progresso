package com.carrotsearch.progresso.jline;

import com.carrotsearch.progresso.ProgressView;
import com.carrotsearch.progresso.RangeTask;
import com.carrotsearch.progresso.RangeTracker;
import com.carrotsearch.progresso.Task;
import com.carrotsearch.progresso.Tasks;
import com.carrotsearch.progresso.views.console.AbstractConsoleView;
import com.carrotsearch.progresso.views.console.ConsoleAware;
import com.carrotsearch.progresso.views.console.ConsoleViewSupplier;
import com.carrotsearch.progresso.views.console.PlainConsoleView;
import com.carrotsearch.progresso.views.console.PlainConsoleViewSupplier;
import com.carrotsearch.progresso.views.console.ProgressBarStyle;
import com.carrotsearch.randomizedtesting.RandomizedTest;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Comparator;
import java.util.List;
import java.util.ServiceLoader;
import java.util.Set;
import org.assertj.core.api.Assertions;
import org.jline.terminal.Attributes;
import org.jline.terminal.Size;
import org.jline.terminal.Terminal;
import org.jline.terminal.impl.ExternalTerminal;
import org.junit.Test;

public class JLineConsoleViewTest extends RandomizedTest {
  /** An xterm-like terminal writing to a buffer: capabilities and colors, but no real tty. */
  private static Terminal xterm(ByteArrayOutputStream out) throws Exception {
    return new ExternalTerminal(
        null,
        "test",
        "xterm-256color",
        new ByteArrayInputStream(new byte[0]),
        out,
        StandardCharsets.UTF_8,
        Terminal.SignalHandler.SIG_DFL,
        false,
        new Attributes(),
        new Size(100, 30));
  }

  @Test
  public void liveLinesThenPermanentLines() throws Exception {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    try (Terminal terminal = xterm(out)) {
      JLineConsoleView view =
          new JLineConsoleView(
              terminal,
              List.of(),
              AbstractConsoleView.defaultFormatters(ProgressBarStyle.ASCII),
              true);

      RangeTask first = Tasks.newRangeTask("first task");
      RangeTask second = Tasks.newRangeTask("second task");
      Set<Task<?>> tasks = Set.of(first, second);

      RangeTracker t1 = first.start(0, 10);
      RangeTracker t2 = second.start(0, 10);
      t1.at(2);
      t2.at(5);
      view.update(tasks);

      String live = out.toString(StandardCharsets.UTF_8);
      Assertions.assertThat(live).contains("first task").contains("second task");
      // Two live lines, drawn with ANSI control sequences (colors, cursor movement).
      Assertions.assertThat(live).contains("\u001b[");

      out.reset();
      view.printLine("a log line");
      t1.close();
      view.update(tasks);
      String after = out.toString(StandardCharsets.UTF_8);
      Assertions.assertThat(after).contains("a log line").contains("done");

      out.reset();
      t2.close();
      view.update(tasks);
      view.close();
      Assertions.assertThat(out.toString(StandardCharsets.UTF_8)).contains("second task");
    }
  }

  @Test
  public void colorizeStylesTheInterestingParts() {
    String line = "Some task        [====:==> :    :    ]  42%   ~1s";
    Assertions.assertThat(JLineConsoleView.colorize(line, false).toAnsi()).isEqualTo(line);

    String ansi = JLineConsoleView.colorize(line, true).toAnsi();
    Assertions.assertThat(ansi).contains("\u001b[1m42%").contains("\u001b[36m");
    Assertions.assertThat(JLineConsoleView.colorize(line, true).toString()).isEqualTo(line);
  }

  @Test
  public void widthSupplierWithoutTerminal() {
    // Under the test runner there is no interactive terminal: unsupported, never an exception.
    Assertions.assertThat(new JLineConsoleWidth().isSupported()).isFalse();
  }

  @Test
  public void plainViewWithoutTerminal() {
    // The JLine supplier is registered and ranks first, but under the test runner stdout is not a
    // terminal: it is unsupported and the core's plain view takes over.
    List<ConsoleViewSupplier> suppliers =
        ServiceLoader.load(ConsoleViewSupplier.class).stream()
            .map(ServiceLoader.Provider::get)
            .sorted(Comparator.comparingInt(ConsoleViewSupplier::priority).reversed())
            .toList();
    Assertions.assertThat(suppliers).first().isInstanceOf(JLineConsoleViewSupplier.class);
    Assertions.assertThat(JLineConsoleView.isSupported()).isFalse();
    Assertions.assertThat(suppliers.get(0).isSupported()).isFalse();

    Assertions.assertThat(ConsoleAware.consoleViewSupplier())
        .get()
        .isInstanceOf(PlainConsoleViewSupplier.class);
    ProgressView view = ConsoleAware.newConsoleProgressView();
    Assertions.assertThat(view).isInstanceOf(PlainConsoleView.class);
  }
}
