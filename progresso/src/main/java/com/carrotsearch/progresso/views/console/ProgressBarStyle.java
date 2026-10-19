package com.carrotsearch.progresso.views.console;

import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.util.Locale;

/**
 * Glyph sets used to draw the progress bar of tasks that report a completion ratio.
 *
 * <p>All styles render a bar of exactly the requested width (in code points) and all glyphs occupy
 * a single terminal column in the usual fonts. Block elements are "ambiguous width" characters in
 * Unicode's East Asian Width classification, so terminals configured for CJK locales may render
 * them two columns wide; use {@link #ASCII} there.
 *
 * @see ConsoleAware#progressBarStyle()
 */
public enum ProgressBarStyle {
  /**
   * The classic {@code [====> : ]} bar built from 7-bit ASCII. Works everywhere, including
   * non-terminals and legacy consoles.
   */
  ASCII("[", "]", '=', "", ' ') {
    @Override
    public String render(int width, double completedRatio) {
      if (width < 2) {
        throw new IllegalArgumentException("Progress bar width must be >= 2: " + width);
      }

      final int tickModulo = Math.max(1, (width - 1) / 4);
      final StringBuilder out = new StringBuilder(width);
      final int atColumn = (int) (clamp(completedRatio) * (width - 1));
      for (int i = 0; i < width; i++) {
        final char chr;
        if (i == 0) {
          chr = '[';
        } else if (i == width - 1) {
          chr = ']';
        } else if (i == atColumn) {
          chr = '>';
        } else if (i % tickModulo == 0) {
          chr = ':';
        } else if (i < atColumn) {
          chr = '=';
        } else {
          chr = ' ';
        }
        out.append(chr);
      }
      return out.toString();
    }
  },

  /**
   * Shade blocks {@code [███▓░░░░]} (U+2591..U+2593, U+2588). Three fractional steps per cell.
   * These glyphs are present in the IBM OEM code pages (437, 850, 852, ...) used by legacy Windows
   * consoles, so this style survives a non-UTF-8 console.
   */
  SHADES("[", "]", '█', "▒▓", '░'),

  /**
   * Left one-eighth blocks {@code [████▍ ]} (U+2588..U+258F). Seven fractional steps per cell; the
   * smoothest bar, rendered correctly by all modern terminal fonts (DejaVu Sans Mono, JetBrains
   * Mono, Cascadia, Menlo).
   */
  BLOCKS("[", "]", '█', "▏▎▍▌▋▊▉", ' '),

  /**
   * Thin heavy line {@code [━━━━╸────]} drawn with box drawing glyphs (U+2501, U+2578, U+2500). One
   * fractional step per cell.
   */
  LINE("[", "]", '━', "╸", '─');

  private final String left;
  private final String right;
  private final char full;
  private final String partials;
  private final char empty;

  ProgressBarStyle(String left, String right, char full, String partials, char empty) {
    this.left = left;
    this.right = right;
    this.full = full;
    this.partials = partials;
    this.empty = empty;
  }

  /**
   * Renders the progress bar.
   *
   * @param width Total width of the bar, including the delimiters, in code points.
   * @param completedRatio Completion ratio, clamped to [0, 1].
   */
  public String render(int width, double completedRatio) {
    if (width < 2) {
      throw new IllegalArgumentException("Progress bar width must be >= 2: " + width);
    }

    final int inner = width - left.length() - right.length();
    final double cells = clamp(completedRatio) * inner;
    final int whole = (int) cells;

    final StringBuilder out = new StringBuilder(width);
    out.append(left);
    for (int i = 0; i < whole; i++) {
      out.append(full);
    }
    if (whole < inner) {
      // Index 0 means an empty cell, 1..partials.length() the fractional glyphs.
      final int steps = partials.length() + 1;
      final int idx = (int) ((cells - whole) * steps);
      out.append(idx == 0 ? empty : partials.charAt(idx - 1));
      for (int i = whole + 1; i < inner; i++) {
        out.append(empty);
      }
    }
    out.append(right);
    return out.toString();
  }

  /** Returns all distinct glyphs this style can emit. */
  public String glyphs() {
    return left + right + full + partials + empty;
  }

  /** Returns true if every glyph of this style can be encoded in the given charset. */
  public boolean isEncodable(Charset charset) {
    CharsetEncoder encoder = charset.newEncoder();
    return encoder.canEncode(glyphs());
  }

  /**
   * Parses a style name, case-insensitively.
   *
   * @throws IllegalArgumentException if the name is not a known style.
   */
  public static ProgressBarStyle parse(String name) {
    String normalized = name.trim().toUpperCase(Locale.ROOT);
    for (ProgressBarStyle style : values()) {
      if (style.name().equals(normalized)) {
        return style;
      }
    }
    throw new IllegalArgumentException(
        "Unknown progress bar style: '" + name + "', expected one of: " + names());
  }

  static String names() {
    StringBuilder b = new StringBuilder();
    for (ProgressBarStyle style : values()) {
      if (b.length() > 0) {
        b.append(", ");
      }
      b.append(style.name().toLowerCase(Locale.ROOT));
    }
    return b.toString();
  }

  private static double clamp(double v) {
    if (Double.isNaN(v)) {
      return 0;
    }
    return Math.max(0, Math.min(1, v));
  }
}
