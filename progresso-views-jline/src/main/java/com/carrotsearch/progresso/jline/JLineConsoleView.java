package com.carrotsearch.progresso.jline;

import com.carrotsearch.progresso.ProgressView;
import com.carrotsearch.progresso.Task;
import com.carrotsearch.progresso.Task.Status;
import com.carrotsearch.progresso.Tracker;
import com.carrotsearch.progresso.util.ColumnCounter;
import com.carrotsearch.progresso.util.LineFormatter;
import com.carrotsearch.progresso.util.LineFormatter.Alignment;
import com.carrotsearch.progresso.views.console.AbstractConsoleView;
import com.carrotsearch.progresso.views.console.ConsoleAware;
import com.carrotsearch.progresso.views.console.LinePrinter;
import com.carrotsearch.progresso.views.console.ProgressBarStyle;
import com.carrotsearch.progresso.views.console.TaskStatusRecovery;
import com.carrotsearch.progresso.views.console.TrackerFormatter;
import com.carrotsearch.progresso.views.console.TrackerRateCalculator;
import com.carrotsearch.progresso.views.console.ViewHelpers;
import java.io.Closeable;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.jline.terminal.Size;
import org.jline.terminal.Terminal;
import org.jline.utils.AttributedString;
import org.jline.utils.AttributedStringBuilder;
import org.jline.utils.AttributedStyle;
import org.jline.utils.Display;
import org.jline.utils.InfoCmp.Capability;

/**
 * A progress view on a JLine terminal: every running task has its own live line (redrawn in place,
 * with colors when the terminal has them), finished tasks and {@link #printLine(String) printed
 * lines} scroll away above the live block, and the layout follows the terminal's current width.
 *
 * <p>Experimental.
 */
public final class JLineConsoleView implements ProgressView, LinePrinter, Closeable {
  private static final Logger LOG = Logger.getLogger(JLineConsoleView.class.getName());

  private final Terminal terminal;
  private final boolean interactive;
  private final int dumbColumns;
  private final boolean colors;
  private final Display display;
  private final List<? extends TrackerFormatter> formatters;
  private final Set<Task<?>> topTasks;

  private final TaskStatusRecovery statusUpdater = new TaskStatusRecovery();
  private final TrackerRateCalculator rates = new TrackerRateCalculator();
  private final LinkedHashSet<Task<?>> running = new LinkedHashSet<>();
  private List<AttributedString> live = List.of();

  /**
   * Returns true if this view can draw live lines: the {@link JLineTerminals#system() system
   * terminal} is an interactive terminal. False when stdout is redirected (a pipe, tee, a file) or
   * there is no terminal at all, as under a test runner; never throws.
   *
   * @see JLineConsoleViewSupplier
   */
  public static boolean isSupported() {
    try {
      return JLineTerminals.isInteractive(JLineTerminals.system());
    } catch (IOException | RuntimeException e) {
      LOG.log(Level.FINE, "Could not open the terminal: {0}", e.toString());
      return false;
    }
  }

  /**
   * A view on the shared {@link JLineTerminals#system() system terminal}, whatever it is. {@link
   * ConsoleAware#newConsoleProgressView()} picks this view through {@link JLineConsoleViewSupplier}
   * only when {@link #isSupported() it is supported}, and plain output otherwise.
   */
  public static JLineConsoleView open() throws IOException {
    return open(Collections.emptyList());
  }

  public static JLineConsoleView open(Collection<? extends Task<?>> topTasks) throws IOException {
    Terminal terminal = JLineTerminals.system();
    JLineConsoleView view =
        new JLineConsoleView(
            terminal,
            topTasks,
            AbstractConsoleView.defaultFormatters(progressBarStyle(terminal)),
            hasColors(terminal));
    // Log lines and other console output go above the live area while this view is open.
    JLineTerminals.viewOpened(view);
    return view;
  }

  /**
   * The progress bar style for a JLine terminal: an explicit request wins, a dumb terminal gets
   * ASCII, anything else the most preferred style encodable in UTF-8. JLine writes Unicode to the
   * console itself (on Windows through the wide-character API, past the console code page), so the
   * operating system gates of {@link ConsoleAware#progressBarStyle()} do not apply.
   */
  public static ProgressBarStyle progressBarStyle(Terminal terminal) {
    return ConsoleAware.explicitProgressBarStyle()
        .orElseGet(
            () ->
                JLineTerminals.isInteractive(terminal)
                    ? ConsoleAware.supportedProgressBarStyles(StandardCharsets.UTF_8).get(0)
                    : ProgressBarStyle.ASCII);
  }

  public JLineConsoleView(
      Terminal terminal,
      Collection<? extends Task<?>> topTasks,
      List<? extends TrackerFormatter> formatters,
      boolean colors) {
    this.terminal = terminal;
    this.interactive = JLineTerminals.isInteractive(terminal);
    // A dumb terminal has no size of its own: use the width progresso's suppliers find (the
    // terminal on stderr if stdout is redirected, COLUMNS, an explicit setting or the default).
    this.dumbColumns = interactive ? 0 : ConsoleAware.consoleWidth();
    this.topTasks = new HashSet<>(topTasks);
    this.formatters = new ArrayList<>(formatters);
    this.colors = colors;
    this.display = new Display(terminal, false);
  }

  public Terminal terminal() {
    return terminal;
  }

  /** The size the view lays out for: the terminal's own, or its substitute for a dumb terminal. */
  public Size size() {
    return interactive ? terminal.getSize() : Size.of(dumbColumns, 0);
  }

  /** True if the terminal reports at least 8 colors. */
  public static boolean hasColors(Terminal terminal) {
    Integer maxColors = terminal.getNumericCapability(Capability.max_colors);
    return maxColors != null && maxColors >= 8;
  }

  @Override
  public synchronized void update(Set<Task<?>> tasks) {
    List<Task<?>> finished = new ArrayList<>();
    statusUpdater.update(
        tasks,
        task -> {
          running.add(task);
          formatters.forEach(formatter -> formatter.taskStarted(task));
        },
        task -> {
          running.remove(task);
          finished.add(task);
        });

    if (!finished.isEmpty()) {
      eraseLive();
      for (Task<?> task : finished) {
        terminal.writer().println(render(task).toAnsi(terminal));
      }
    }
    redrawLive();
  }

  /** Prints a permanent line above the live block (log output, for example). */
  @Override
  public synchronized void printLine(String line) {
    eraseLive();
    terminal.writer().println(line);
    redrawLive();
  }

  /** Finishes the live block: still running tasks are printed as permanent lines. */
  @Override
  public synchronized void close() throws IOException {
    JLineTerminals.viewClosed(this);
    eraseLive();
    for (Task<?> task : running) {
      terminal.writer().println(render(task).toAnsi(terminal));
    }
    running.clear();
    terminal.flush();
  }

  private void eraseLive() {
    if (!live.isEmpty()) {
      display.update(List.of(), 0);
      live = List.of();
    }
    terminal.flush();
  }

  private void redrawLive() {
    List<AttributedString> lines = new ArrayList<>();
    for (Task<?> task : running) {
      lines.add(render(task));
    }
    if (lines.isEmpty() && live.isEmpty()) {
      terminal.flush();
      return;
    }
    display.reset();
    display.resize(size());
    display.update(lines, -1);
    live = lines;
  }

  private AttributedString render(Task<?> task) {
    // Leave a column for the cursor; some terminals wrap otherwise.
    int lineWidth = Math.max(20, size().getColumns() - 1);
    return colorize(formatStatusLine(task, lineWidth), colors);
  }

  /** The same layout as the core console views: counter, name, tracker, status, time. */
  private String formatStatusLine(Task<?> task, int lineWidth) {
    LineFormatter lf = new LineFormatter(ColumnCounter.DEFAULT);

    if (!topTasks.isEmpty()) {
      String top = Long.toString(topTasks.size());
      long current =
          topTasks.stream().filter(t -> t == task || task.isChildOf(t) || t.isDone()).count();
      int width = 2 * lf.getColumnCounter().columns(top) + 2;
      lf.cell(width, width, Alignment.RIGHT, current + "/" + top + " ");
    }

    ViewHelpers.appendTaskName(lf, task);

    Optional<TrackerRateCalculator.TrackerStats> stats = Optional.empty();
    if (task.hasTracker()) {
      Tracker tracker = task.getTracker();
      for (TrackerFormatter formatter : formatters) {
        if (formatter.supports(lineWidth, tracker)) {
          formatter.format(lf, lineWidth, task, tracker);
          break;
        }
      }
      stats = Optional.of(rates.update(tracker));
    }

    Status status = task.getStatus();
    if (status == Status.DONE) {
      ViewHelpers.appendStatus(lf, "done");
    } else if (status == Status.SKIPPED) {
      ViewHelpers.appendStatus(lf, "skipped");
    }
    ViewHelpers.appendTime(lf, task, stats);

    return lf.format(lineWidth);
  }

  private static final Pattern TOKENS =
      Pattern.compile(
          "(?<bar>\\[[^\\]]*\\])|(?<pct>\\d+%)|(?<done>\\bdone\\b)|(?<skip>\\bskipped\\b)|(?<eta>~\\S+)|(?<rate>\\S+/s)");

  /** Adds colors to a status line formatted by the core views; a no-op without colors. */
  static AttributedString colorize(String line, boolean colors) {
    if (!colors) {
      return new AttributedString(line);
    }

    AttributedStringBuilder b = new AttributedStringBuilder();
    Matcher m = TOKENS.matcher(line);
    int at = 0;
    while (m.find()) {
      b.append(line, at, m.start());
      if (m.group("bar") != null) {
        String bar = m.group("bar");
        for (int i = 0; i < bar.length(); i++) {
          char c = bar.charAt(i);
          boolean fill = c != '[' && c != ']' && c != ' ' && c != ':' && c != '⠀';
          b.append(
              String.valueOf(c),
              fill
                  ? AttributedStyle.DEFAULT.foreground(AttributedStyle.CYAN)
                  : AttributedStyle.DEFAULT);
        }
      } else if (m.group("pct") != null) {
        b.append(m.group(), AttributedStyle.BOLD);
      } else if (m.group("done") != null) {
        b.append(m.group(), AttributedStyle.BOLD.foreground(AttributedStyle.GREEN));
      } else if (m.group("skip") != null) {
        b.append(m.group(), AttributedStyle.DEFAULT.foreground(AttributedStyle.YELLOW));
      } else {
        b.append(m.group(), AttributedStyle.DEFAULT.faint());
      }
      at = m.end();
    }
    b.append(line, at, line.length());
    return b.toAttributedString();
  }
}
