package com.carrotsearch.progresso.views.console;

import com.carrotsearch.randomizedtesting.RandomizedTest;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import org.assertj.core.api.Assertions;
import org.junit.Assume;
import org.junit.Test;

public class ProgressBarStyleTest extends RandomizedTest {
  @Test
  public void asciiIsUnchanged() {
    // The pre-existing bar layout: delimiters, quarter ticks and a '>' head.
    Assertions.assertThat(ProgressBarStyle.ASCII.render(21, 0)).isEqualTo("[    :    :    :    ]");
    Assertions.assertThat(ProgressBarStyle.ASCII.render(21, 0.5))
        .isEqualTo("[====:====>    :    ]");
    Assertions.assertThat(ProgressBarStyle.ASCII.render(21, 1)).isEqualTo("[====:====:====:====]");
  }

  @Test
  public void fractionalGlyphs() {
    Assertions.assertThat(ProgressBarStyle.BLOCKS.render(12, 0)).isEqualTo("[          ]");
    Assertions.assertThat(ProgressBarStyle.BLOCKS.render(12, 0.03)).isEqualTo("[▎         ]");
    Assertions.assertThat(ProgressBarStyle.BLOCKS.render(12, 0.25)).isEqualTo("[██▌       ]");
    Assertions.assertThat(ProgressBarStyle.BLOCKS.render(12, 0.5)).isEqualTo("[█████     ]");
    Assertions.assertThat(ProgressBarStyle.BLOCKS.render(12, 0.99)).isEqualTo("[█████████▉]");
    Assertions.assertThat(ProgressBarStyle.BLOCKS.render(12, 1)).isEqualTo("[██████████]");

    Assertions.assertThat(ProgressBarStyle.SHADES.render(12, 0.25)).isEqualTo("[██▒░░░░░░░]");
    Assertions.assertThat(ProgressBarStyle.SHADES.render(12, 0.28)).isEqualTo("[██▓░░░░░░░]");
    Assertions.assertThat(ProgressBarStyle.LINE.render(12, 0.25)).isEqualTo("[━━╸───────]");
    Assertions.assertThat(ProgressBarStyle.LINE.render(12, 0.24)).isEqualTo("[━━────────]");
  }

  @Test
  public void widthAndMonotonicity() {
    for (ProgressBarStyle style : ProgressBarStyle.values()) {
      for (int round = 0; round < 50; round++) {
        int width = randomIntBetween(2, 60);
        String previous = style.render(width, 0);
        Assertions.assertThat(previous.codePointCount(0, previous.length())).isEqualTo(width);

        double ratio = 0;
        while (ratio < 1) {
          ratio = Math.min(1, ratio + randomDouble() / 10);
          String current = style.render(width, ratio);
          Assertions.assertThat(current.codePointCount(0, current.length())).isEqualTo(width);
          Assertions.assertThat(filled(style, current))
              .as("%s at %.3f: '%s' after '%s'", style, ratio, current, previous)
              .isGreaterThanOrEqualTo(filled(style, previous));
          previous = current;
        }
      }
    }
  }

  @Test
  public void outOfRangeRatios() {
    for (ProgressBarStyle style : ProgressBarStyle.values()) {
      Assertions.assertThat(style.render(21, -1)).isEqualTo(style.render(21, 0));
      Assertions.assertThat(style.render(21, 2)).isEqualTo(style.render(21, 1));
      Assertions.assertThat(style.render(21, Double.NaN)).isEqualTo(style.render(21, 0));
    }
  }

  @Test
  public void encodability() {
    for (ProgressBarStyle style : ProgressBarStyle.values()) {
      Assertions.assertThat(style.isEncodable(StandardCharsets.UTF_8)).as("%s", style).isTrue();
      Assertions.assertThat(style.isEncodable(StandardCharsets.US_ASCII))
          .as("%s", style)
          .isEqualTo(style == ProgressBarStyle.ASCII);
    }

    Assume.assumeTrue("IBM437 charset not available", Charset.isSupported("IBM437"));
    Charset cp437 = Charset.forName("IBM437");
    Assertions.assertThat(ProgressBarStyle.ASCII.isEncodable(cp437)).isTrue();
    Assertions.assertThat(ProgressBarStyle.SHADES.isEncodable(cp437)).isTrue();
    Assertions.assertThat(ProgressBarStyle.BLOCKS.isEncodable(cp437)).isFalse();
    Assertions.assertThat(ProgressBarStyle.LINE.isEncodable(cp437)).isFalse();
  }

  @Test
  public void parse() {
    Assertions.assertThat(ProgressBarStyle.parse("blocks")).isEqualTo(ProgressBarStyle.BLOCKS);
    Assertions.assertThat(ProgressBarStyle.parse(" Ascii ")).isEqualTo(ProgressBarStyle.ASCII);
    Assertions.assertThatThrownBy(() -> ProgressBarStyle.parse("fancy"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("fancy")
        .hasMessageContaining("blocks");
  }

  /**
   * Counts "fill" columns. For the ASCII style this is the run of non-space columns (ticks and the
   * '>' head included, since they move with the fill); for the others anything that is not the
   * empty glyph.
   */
  private static int filled(ProgressBarStyle style, String bar) {
    int count = 0;
    for (int i = 1; i < bar.length() - 1; i++) {
      char c = bar.charAt(i);
      if (style == ProgressBarStyle.ASCII) {
        if (c == ' ') {
          break;
        }
        count++;
      } else if (c != ' ' && c != '\u2591' && c != '\u2500') {
        count++;
      }
    }
    return count;
  }
}
