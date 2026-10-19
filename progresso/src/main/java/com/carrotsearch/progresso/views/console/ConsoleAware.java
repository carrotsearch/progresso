package com.carrotsearch.progresso.views.console;

import com.carrotsearch.progresso.ProgressView;
import com.carrotsearch.progresso.Task;
import com.carrotsearch.progresso.annotations.SuppressForbidden;
import com.carrotsearch.progresso.util.ColumnCounter;
import com.carrotsearch.progresso.util.OsDetection;
import java.io.Console;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.ServiceLoader;

@SuppressForbidden("Legitimate use of System.out and properties.")
public class ConsoleAware {
  /** Sane default console with. We don't want to use native hooks to get it. */
  public static final int CONSOLE_WIDTH_DEFAULT = 80;

  public static final String CONSOLE_WIDTH_PROPERTY = "console.width";
  public static final String CONSOLE_UPDATEABLE_PROPERTY = "console.updateable";

  /**
   * System property with the length of the sampling window, in milliseconds, used to estimate
   * throughput and completion time (ETA) of running tasks. The default is 5000 (5 seconds). Bursty
   * tasks (for example those reading from a compressed stream) benefit from a longer window; the
   * value 0 uses all samples collected since the task started.
   */
  public static final String ETA_WINDOW_PROPERTY = "progresso.eta.window.millis";

  /**
   * System property selecting the glyphs used to draw progress bars: one of {@link
   * ProgressBarStyle} names (case-insensitive) or {@code auto} (the default) to pick a style based
   * on the terminal, operating system and output encoding, see {@link #progressBarStyle()}.
   */
  public static final String PROGRESS_BAR_PROPERTY = "console.progressbar";

  /**
   * Environment variable equivalent of {@link #PROGRESS_BAR_PROPERTY}. The system property takes
   * precedence if both are set.
   */
  public static final String PROGRESS_BAR_ENV = "PROGRESSO_PROGRESSBAR";

  private static final String PROGRESS_BAR_AUTO = "auto";

  /** Candidate styles, from the most to the least preferred; ASCII is always last. */
  private static final ProgressBarStyle[] CANDIDATES = {
    ProgressBarStyle.BRAILLE,
    ProgressBarStyle.BLOCKS,
    ProgressBarStyle.SHADES,
    ProgressBarStyle.LINE,
    ProgressBarStyle.ASCII
  };

  /**
   * Candidates in CJK locales, where terminals commonly render ambiguous-width glyphs (block
   * elements, box drawing) two columns wide; only neutral-width styles are safe there.
   */
  private static final ProgressBarStyle[] CANDIDATES_CJK = {
    ProgressBarStyle.BRAILLE, ProgressBarStyle.ASCII
  };

  private static ConsoleWriter consoleWriter;
  private static int globalWidthHint = CONSOLE_WIDTH_DEFAULT;

  /** Returns true if the given console is attached to an interactive terminal. */
  private static boolean isTerminal(Console console) {
    return console != null && console.isTerminal();
  }

  public static ProgressView newConsoleProgressView() {
    return newConsoleProgressView(Collections.emptyList());
  }

  public static ProgressView newConsoleProgressView(Collection<Task<?>> topLevelSteps) {
    if (isUpdateable()) {
      return new UpdateableConsoleView(writer(), topLevelSteps);
    } else {
      return new PlainConsoleView(writer(), topLevelSteps);
    }
  }

  public static boolean isUpdateable() {
    boolean consoleUpdateable;
    if (System.getProperty(CONSOLE_UPDATEABLE_PROPERTY) != null) {
      consoleUpdateable = Boolean.parseBoolean(System.getProperty(CONSOLE_UPDATEABLE_PROPERTY));
    } else {
      consoleUpdateable = isTerminal(System.console());
    }
    return consoleUpdateable;
  }

  /**
   * Returns the progress bar style to use.
   *
   * <p>An explicit style set with the {@link #PROGRESS_BAR_PROPERTY} system property or the {@link
   * #PROGRESS_BAR_ENV} environment variable always wins. Otherwise the style is picked using these
   * heuristics, since no platform can tell whether the terminal's font actually has a glyph:
   *
   * <ul>
   *   <li>If the output is not a terminal (see {@link #isUpdateable()}), {@code TERM} is {@code
   *       dumb} or the operating system is Windows (console code pages and glyph fallback vary too
   *       much between hosts), {@link ProgressBarStyle#ASCII}.
   *   <li>Otherwise the candidates are, in order of preference, {@link ProgressBarStyle#BRAILLE},
   *       {@link ProgressBarStyle#BLOCKS}, {@link ProgressBarStyle#SHADES}, {@link
   *       ProgressBarStyle#LINE} and {@link ProgressBarStyle#ASCII}; in Chinese, Japanese and
   *       Korean locales ({@code LC_ALL}, {@code LC_CTYPE} or {@code LANG}), where terminals
   *       commonly render East Asian "ambiguous width" characters two columns wide and misalign
   *       block elements and box drawing glyphs, only the neutral-width {@link
   *       ProgressBarStyle#BRAILLE} and {@link ProgressBarStyle#ASCII}.
   *   <li>The first candidate whose glyphs the output charset can encode is used; see {@link
   *       #outputCharset()} and {@link #supportedProgressBarStyles()}.
   * </ul>
   *
   * @throws IllegalArgumentException if the explicitly requested style is unknown.
   */
  public static ProgressBarStyle progressBarStyle() {
    return detectProgressBarStyle(
        System.getProperty(PROGRESS_BAR_PROPERTY),
        System.getenv(),
        isUpdateable(),
        OsDetection.IS_OS_WINDOWS,
        outputCharset());
  }

  static ProgressBarStyle detectProgressBarStyle(
      String propertyOverride,
      Map<String, String> env,
      boolean isTerminal,
      boolean isWindows,
      Charset charset) {
    return explicitProgressBarStyle(propertyOverride, env)
        .orElseGet(() -> supportedProgressBarStyles(env, isTerminal, isWindows, charset).get(0));
  }

  /**
   * @return Returns the style requested explicitly with the {@link #PROGRESS_BAR_PROPERTY} system
   *     property or the {@link #PROGRESS_BAR_ENV} environment variable, if any ({@code auto} and
   *     blank values count as none).
   * @throws IllegalArgumentException if the requested style is unknown.
   */
  public static Optional<ProgressBarStyle> explicitProgressBarStyle() {
    return explicitProgressBarStyle(System.getProperty(PROGRESS_BAR_PROPERTY), System.getenv());
  }

  static Optional<ProgressBarStyle> explicitProgressBarStyle(
      String propertyOverride, Map<String, String> env) {
    String override = propertyOverride != null ? propertyOverride : env.get(PROGRESS_BAR_ENV);
    if (override != null) {
      override = override.trim();
      if (!override.isEmpty() && !override.equalsIgnoreCase(PROGRESS_BAR_AUTO)) {
        return Optional.of(ProgressBarStyle.parse(override));
      }
    }
    return Optional.empty();
  }

  /**
   * @return Returns the styles usable in the current environment, the most preferred first. The
   *     list always ends with {@link ProgressBarStyle#ASCII}; see {@link #progressBarStyle()} for
   *     the heuristics.
   */
  public static List<ProgressBarStyle> supportedProgressBarStyles() {
    return supportedProgressBarStyles(
        System.getenv(), isUpdateable(), OsDetection.IS_OS_WINDOWS, outputCharset());
  }

  static List<ProgressBarStyle> supportedProgressBarStyles(
      Map<String, String> env, boolean isTerminal, boolean isWindows, Charset charset) {
    // Windows consoles and fonts are too unpredictable (code pages, glyph fallback): ASCII only,
    // unless a style is requested explicitly.
    if (!isTerminal || isWindows || "dumb".equals(env.get("TERM"))) {
      return List.of(ProgressBarStyle.ASCII);
    }
    return supportedProgressBarStyles(env, charset);
  }

  /**
   * @return Returns the styles usable on a terminal known to display Unicode and written in the
   *     given charset, the most preferred first; the list always ends with {@link
   *     ProgressBarStyle#ASCII}. Unlike {@link #supportedProgressBarStyles()}, this does not look
   *     at the operating system or at whether the output is a terminal, only at the charset and the
   *     locale (CJK locales get neutral-width styles only). Meant for terminal libraries
   *     (progresso-jline) that bypass the console code page.
   */
  public static List<ProgressBarStyle> supportedProgressBarStyles(Charset charset) {
    return supportedProgressBarStyles(System.getenv(), charset);
  }

  static List<ProgressBarStyle> supportedProgressBarStyles(
      Map<String, String> env, Charset charset) {
    ProgressBarStyle[] candidates = isCjkLocale(env) ? CANDIDATES_CJK : CANDIDATES;
    List<ProgressBarStyle> supported =
        Arrays.stream(candidates).filter(style -> style.isEncodable(charset)).toList();
    return supported.isEmpty() ? List.of(ProgressBarStyle.ASCII) : supported;
  }

  /**
   * Returns true if the character-type locale, as POSIX resolves it ({@code LC_ALL}, then {@code
   * LC_CTYPE}, then {@code LANG}), is Chinese, Japanese or Korean.
   */
  static boolean isCjkLocale(Map<String, String> env) {
    for (String variable : new String[] {"LC_ALL", "LC_CTYPE", "LANG"}) {
      String value = env.get(variable);
      if (value != null && !value.isEmpty()) {
        String language = value.toLowerCase(Locale.ROOT);
        return language.startsWith("zh") || language.startsWith("ja") || language.startsWith("ko");
      }
    }
    return false;
  }

  /**
   * Returns the charset of the console output: the console's charset if a console is attached,
   * otherwise the JVM's standard output encoding ({@code stdout.encoding}) or, failing that, the
   * default charset.
   */
  public static Charset outputCharset() {
    Console console = System.console();
    if (console != null) {
      return console.charset();
    }

    String encoding = System.getProperty("stdout.encoding");
    if (encoding != null) {
      try {
        return Charset.forName(encoding);
      } catch (IllegalArgumentException e) {
        // Unknown or illegal name; fall through.
      }
    }

    return Charset.defaultCharset();
  }

  /** Returns the console writer. */
  public static synchronized ConsoleWriter writer() {
    if (consoleWriter == null) {
      consoleWriter = new ConsoleWriter(outputWriter(), ColumnCounter.DEFAULT, consoleWidth());
    }

    return consoleWriter;
  }

  /**
   * Returns the writer progress output goes to: the console's own writer if the console is an
   * interactive terminal, otherwise {@link System#out} in the {@link #outputCharset()}.
   *
   * <p>Since JDK 22, {@link System#console()} is non-null even when the output is redirected, and
   * its writer bypasses {@link System#setOut(java.io.PrintStream)}. Anything capturing standard
   * output (IDE test runners, build tools, log capture) would then never see the progress output.
   */
  static Writer outputWriter() {
    Console console = System.console();
    if (isTerminal(console)) {
      return console.writer();
    } else {
      return new OutputStreamWriter(System.out, outputCharset());
    }
  }

  /**
   * Returns the current console width: the width from the supported {@link ConsoleWidthSupplier}
   * with the highest priority, or the default width hint if there is none. The {@link
   * #CONSOLE_WIDTH_PROPERTY} system property is one of the suppliers, with the highest priority.
   *
   * @throws IllegalArgumentException if the property is set to an invalid value.
   * @see #setDefaultConsoleWidth(int)
   */
  public static int consoleWidth() throws IllegalArgumentException {
    return consoleWidthSupplier()
        .map(ConsoleWidthSupplier::getConsoleWidth)
        .orElse(globalWidthHint);
  }

  /**
   * @return Returns the supported {@link ConsoleWidthSupplier} with the highest priority, if any.
   */
  public static Optional<ConsoleWidthSupplier> consoleWidthSupplier() {
    return ServiceLoader.load(ConsoleWidthSupplier.class).stream()
        .map(ServiceLoader.Provider::get)
        .sorted(Comparator.comparingInt(ConsoleWidthSupplier::priority).reversed())
        .filter(ConsoleWidthSupplier::isSupported)
        .findFirst();
  }

  /**
   * Provide the default console width hint (in columns).
   *
   * @throws IllegalArgumentException If console width &lt; 1.
   */
  public static void setDefaultConsoleWidth(int widthHint) throws IllegalArgumentException {
    checkWidth(widthHint);
    globalWidthHint = widthHint;
  }

  static void checkWidth(int widthHint) {
    if (widthHint < 1) {
      throw new IllegalArgumentException("Console width hint must be >= 1: " + widthHint);
    }
  }

  /** Prints console information to command-line. */
  public static void main(String[] args) throws IOException {
    try (ConsoleWriter w = ConsoleAware.writer()) {
      w.printLine(
          String.format(
              Locale.ROOT,
              "Console %s, width: %s (%s), charset: %s, progress bar: %s.",
              ConsoleAware.isUpdateable() ? "is updateable" : "is not updateable",
              ConsoleAware.consoleWidth(),
              ConsoleAware.consoleWidthSupplier()
                  .map(supplier -> supplier.getClass().getSimpleName())
                  .orElse("default"),
              ConsoleAware.outputCharset().name(),
              ConsoleAware.progressBarStyle().name().toLowerCase(Locale.ROOT)));
    }
  }
}
