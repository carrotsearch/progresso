package com.carrotsearch.progresso.views.console;

import com.carrotsearch.randomizedtesting.RandomizedTest;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import org.assertj.core.api.Assertions;
import org.junit.Assume;
import org.junit.Test;

public class ProgressBarStyleDetectionTest extends RandomizedTest {
  private static final boolean TERMINAL = true;
  private static final boolean NOT_TERMINAL = false;
  private static final boolean WINDOWS = true;
  private static final boolean UNIX = false;

  private static ProgressBarStyle detect(
      String property, Map<String, String> env, boolean terminal, boolean windows, Charset cs) {
    return ConsoleAware.detectProgressBarStyle(property, env, terminal, windows, cs);
  }

  private static Map<String, String> env(String... keyValues) {
    Map<String, String> env = new HashMap<>();
    for (int i = 0; i < keyValues.length; i += 2) {
      env.put(keyValues[i], keyValues[i + 1]);
    }
    return env;
  }

  @Test
  public void explicitOverrides() {
    // The property wins over everything, including the environment and a non-terminal.
    Assertions.assertThat(
            detect("line", env("PROGRESSO_PROGRESSBAR", "ascii"), NOT_TERMINAL, WINDOWS, ascii()))
        .isEqualTo(ProgressBarStyle.LINE);

    // Environment variable alone.
    Assertions.assertThat(
            detect(null, env("PROGRESSO_PROGRESSBAR", "Shades"), NOT_TERMINAL, UNIX, ascii()))
        .isEqualTo(ProgressBarStyle.SHADES);

    // "auto" and blanks fall through to detection.
    Assertions.assertThat(detect("auto", env(), TERMINAL, UNIX, utf8()))
        .isEqualTo(ProgressBarStyle.BLOCKS);
    Assertions.assertThat(detect(" ", env("PROGRESSO_PROGRESSBAR", "AUTO"), TERMINAL, UNIX, utf8()))
        .isEqualTo(ProgressBarStyle.BLOCKS);

    // Braille is never auto-detected, only requested.
    Assertions.assertThat(detect("braille", env(), TERMINAL, UNIX, utf8()))
        .isEqualTo(ProgressBarStyle.BRAILLE);

    Assertions.assertThatThrownBy(() -> detect("fancy", env(), TERMINAL, UNIX, utf8()))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  public void notATerminal() {
    Assertions.assertThat(detect(null, env("WT_SESSION", "x"), NOT_TERMINAL, WINDOWS, utf8()))
        .isEqualTo(ProgressBarStyle.ASCII);
    Assertions.assertThat(detect(null, env("TERM", "dumb"), TERMINAL, UNIX, utf8()))
        .isEqualTo(ProgressBarStyle.ASCII);
  }

  @Test
  public void unix() {
    Assertions.assertThat(detect(null, env("TERM", "xterm-256color"), TERMINAL, UNIX, utf8()))
        .isEqualTo(ProgressBarStyle.BLOCKS);
    // Non-unicode locale: nothing fancy is encodable.
    Assertions.assertThat(detect(null, env(), TERMINAL, UNIX, StandardCharsets.ISO_8859_1))
        .isEqualTo(ProgressBarStyle.ASCII);
  }

  @Test
  public void cjkLocales() {
    // Terminals in CJK locales commonly render ambiguous-width glyphs double-wide: only
    // neutral-width styles are picked.
    Assertions.assertThat(detect(null, env("LANG", "ja_JP.UTF-8"), TERMINAL, UNIX, utf8()))
        .isEqualTo(ProgressBarStyle.BRAILLE);
    Assertions.assertThat(detect(null, env("LANG", "zh_CN.UTF-8"), TERMINAL, WINDOWS, utf8()))
        .isEqualTo(ProgressBarStyle.BRAILLE);
    Assertions.assertThat(detect(null, env("LC_CTYPE", "ko_KR.UTF-8"), TERMINAL, UNIX, utf8()))
        .isEqualTo(ProgressBarStyle.BRAILLE);

    // POSIX precedence: LC_ALL beats LC_CTYPE beats LANG.
    Assertions.assertThat(
            detect(
                null, env("LC_ALL", "en_US.UTF-8", "LANG", "ja_JP.UTF-8"), TERMINAL, UNIX, utf8()))
        .isEqualTo(ProgressBarStyle.BLOCKS);
    Assertions.assertThat(
            detect(
                null,
                env("LC_CTYPE", "ja_JP.UTF-8", "LANG", "en_US.UTF-8"),
                TERMINAL,
                UNIX,
                utf8()))
        .isEqualTo(ProgressBarStyle.BRAILLE);

    // Braille not encodable: never fall back to the ambiguous-width styles.
    Assertions.assertThat(detect(null, env("LANG", "ja_JP.UTF-8"), TERMINAL, UNIX, ascii()))
        .isEqualTo(ProgressBarStyle.ASCII);

    // Non-CJK and odd values are not CJK.
    for (String lang : new String[] {"C", "C.UTF-8", "POSIX", "en_US.UTF-8", "pl_PL.UTF-8", ""}) {
      Assertions.assertThat(detect(null, env("LANG", lang), TERMINAL, UNIX, utf8()))
          .as("LANG=%s", lang)
          .isEqualTo(ProgressBarStyle.BLOCKS);
    }
    Assertions.assertThat(
            detect(null, env("TERM", "dumb", "LANG", "ja_JP.UTF-8"), TERMINAL, UNIX, utf8()))
        .isEqualTo(ProgressBarStyle.ASCII);
  }

  @Test
  public void windows() {
    // Known unicode-capable hosts.
    Assertions.assertThat(detect(null, env("WT_SESSION", "x"), TERMINAL, WINDOWS, utf8()))
        .isEqualTo(ProgressBarStyle.BLOCKS);
    Assertions.assertThat(detect(null, env("ConEmuANSI", "ON"), TERMINAL, WINDOWS, utf8()))
        .isEqualTo(ProgressBarStyle.BLOCKS);
    Assertions.assertThat(detect(null, env("TERM", "xterm"), TERMINAL, WINDOWS, utf8()))
        .isEqualTo(ProgressBarStyle.BLOCKS);

    // Legacy console host: at most shades, even when the code page is UTF-8.
    Assertions.assertThat(detect(null, env(), TERMINAL, WINDOWS, utf8()))
        .isEqualTo(ProgressBarStyle.SHADES);

    // Windows ANSI code page without block elements.
    Assume.assumeTrue(Charset.isSupported("windows-1250"));
    Assertions.assertThat(
            detect(
                null, env("WT_SESSION", "x"), TERMINAL, WINDOWS, Charset.forName("windows-1250")))
        .isEqualTo(ProgressBarStyle.ASCII);

    // OEM code page: shades are available.
    Assume.assumeTrue(Charset.isSupported("IBM437"));
    Assertions.assertThat(detect(null, env(), TERMINAL, WINDOWS, Charset.forName("IBM437")))
        .isEqualTo(ProgressBarStyle.SHADES);
  }

  @Test
  public void liveDetectionDoesNotThrow() {
    // Whatever the test environment, auto-detection must produce something.
    Assertions.assertThat(ConsoleAware.progressBarStyle()).isNotNull();
    Assertions.assertThat(ConsoleAware.outputCharset()).isNotNull();
  }

  private static Charset utf8() {
    return StandardCharsets.UTF_8;
  }

  private static Charset ascii() {
    return StandardCharsets.US_ASCII;
  }
}
