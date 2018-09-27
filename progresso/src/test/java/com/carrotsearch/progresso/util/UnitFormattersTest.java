package com.carrotsearch.progresso.util;

import static org.assertj.core.api.Assertions.*;

import org.junit.Assert;
import org.junit.Test;

import com.carrotsearch.progresso.util.UnitFormatters.ByteFormatter;
import com.carrotsearch.progresso.util.UnitFormatters.DecimalCompactFormatter;
import com.carrotsearch.randomizedtesting.RandomizedTest;

public class UnitFormattersTest extends RandomizedTest {
  @Test
  public void testFormatBytes() {
    UnitFormatter fmt = Units.BYTES;
    assertThat(fmt.format(0)).isEqualTo("0B");
    assertThat(fmt.format(1023)).isEqualTo("1023B");
    assertThat(fmt.format(1024)).isEqualTo("1.00KB");
    assertThat(fmt.format(1024 * 1024)).isEqualTo("1.00MB");
    assertThat(fmt.format(1024 * 1024 * 1024)).isEqualTo("1.00GB");
    assertThat(fmt.format(1024L * 1024 * 1024 * 1024 * 2)).isEqualTo("2.00TB");
  }

  @Test
  public void testParseBytes() {
    ByteFormatter fmt = Units.BYTES;
    assertThat(fmt.parse("0")).isEqualTo(0);
    assertThat(fmt.parse("1023")).isEqualTo(1023);
    assertThat(fmt.parse("1KB")).isEqualTo(1024);
    assertThat(fmt.parse("2KB")).isEqualTo(2 * 1024);
    assertThat(fmt.parse("2MB")).isEqualTo(2 * 1024 * 1024);
    assertThat(fmt.parse("2GB")).isEqualTo(2L * 1024 * 1024 * 1024);
    assertThat(fmt.parse("2.01TB")).isEqualTo(1024L * 1024 * 1024 * 1024 * 201 / 100);

    assertThat(fmt.parse("1 023")).isEqualTo(1023);
    assertThat(fmt.parse("1,023")).isEqualTo(1023);
    assertThat(fmt.parse("10,023")).isEqualTo(10_023);
    assertThat(fmt.parse("10,023")).isEqualTo(10_023);
    assertThat(fmt.parse("0,001,023")).isEqualTo(1_023);

    assertDoesntParse(() -> fmt.parse(",001,023"));
    assertDoesntParse(() -> fmt.parse("1,23"));
    assertDoesntParse(() -> fmt.parse("1,"));
    assertDoesntParse(() -> fmt.parse("0,11"));

    assertDoesntParse(() -> fmt.parse(" 001 023"));
    assertDoesntParse(() -> fmt.parse("1 23"));
    assertDoesntParse(() -> fmt.parse("1 "));
    assertDoesntParse(() -> fmt.parse("0 11"));
  }

  private void assertDoesntParse(Runnable c) {
    try {
      c.run();
      Assert.fail("Parsed, but shouldn't.");
    } catch (NumberFormatException e) {
      // Expected, ok.
    }
  }

  @Test
  public void testFormatDecimalCompact() {
    DecimalCompactFormatter fmt = Units.DECIMAL_COMPACT;
    assertThat(fmt.format(0)).isEqualTo("0");
    assertThat(fmt.format(9)).isEqualTo("9");
    assertThat(fmt.format(10)).isEqualTo("10");
    assertThat(fmt.format(1000)).isEqualTo("1.00k");
    assertThat(fmt.format(1100)).isEqualTo("1.10k");
    assertThat(fmt.format(1000000)).isEqualTo("1.00M");
    assertThat(fmt.format(1000000000)).isEqualTo("1.00G");
  }

  @Test
  public void testDecimalCompactParse() {
    DecimalCompactFormatter fmt = Units.DECIMAL_COMPACT;

    assertThat(fmt.parse("0")).isEqualTo(0);
    assertThat(fmt.parse("1023")).isEqualTo(1023);
    assertThat(fmt.parse("1k")).isEqualTo(1000);
    assertThat(fmt.parse("2k")).isEqualTo(2000);
    assertThat(fmt.parse("2M")).isEqualTo(2_000_000);
    assertThat(fmt.parse("2G")).isEqualTo(2_000_000_000);

    assertThat(fmt.parse("1 023")).isEqualTo(1023);
    assertThat(fmt.parse("1,023")).isEqualTo(1023);
    assertThat(fmt.parse("10,023")).isEqualTo(10_023);
    assertThat(fmt.parse("10,023")).isEqualTo(10_023);
    assertThat(fmt.parse("0,001,023")).isEqualTo(1_023);        
  }
  
  @Test
  public void testFormatDecimal() {
    UnitFormatter fmt = Units.DECIMAL;
    assertThat(fmt.format(0)).isEqualTo("0");
    assertThat(fmt.format(9)).isEqualTo("9");
    assertThat(fmt.format(10)).isEqualTo("10");
    assertThat(fmt.format(1000)).isEqualTo("1,000");
    assertThat(fmt.format(1000000)).isEqualTo("1,000,000");
  }

  @Test
  public void testFormatDuration() {
    final long S = 1000;
    final long M = S * 60;
    final long H = M * 60;
    final long D = H * 24;

    UnitFormatter fmt = Units.DURATION;
    assertThat(fmt.format(0)).isEqualTo("0ms");
    assertThat(fmt.format(10)).isEqualTo("10ms");
    assertThat(fmt.format(S)).isEqualTo("1s");
    assertThat(fmt.format(S + 1)).isEqualTo("1s 1ms");
    assertThat(fmt.format(30 * S)).isEqualTo("30s");
    assertThat(fmt.format(59 * S + 999)).isEqualTo("59s");
    assertThat(fmt.format(M)).isEqualTo("1m");
    assertThat(fmt.format(M + S + 10)).isEqualTo("1m 1s");
    assertThat(fmt.format(M + 10)).isEqualTo("1m");
    assertThat(fmt.format(29 * M + S + 10)).isEqualTo("29m 1s");
    assertThat(fmt.format(1 * H + 1 * M + 10 * S)).isEqualTo("1h 1m");
    assertThat(fmt.format(6 * H)).isEqualTo("6h");
    assertThat(fmt.format(6 * H + 10 * S)).isEqualTo("6h");
    assertThat(fmt.format(6 * H + 10 * S)).isEqualTo("6h");
    assertThat(fmt.format(6 * H + 1 * M)).isEqualTo("6h 1m");
    assertThat(fmt.format(1 * D + 1 * M)).isEqualTo("1d 1m");
    assertThat(fmt.format(2 * D + 1 * H + 3 * M + 1 * S)).isEqualTo("2d 1h 3m");
  }
  
  @Test
  public void testFormatCompactDuration() {
    final long S = 1000;
    final long M = S * 60;
    final long H = M * 60;
    final long D = H * 24;

    UnitFormatter fmt = Units.DURATION_COMPACT;
    assertThat(fmt.format(0)).isEqualTo("0ms");
    assertThat(fmt.format(10)).isEqualTo("10ms");
    assertThat(fmt.format(S)).isEqualTo("1s");
    assertThat(fmt.format(S + 1)).isEqualTo("1s");
    assertThat(fmt.format(30 * S)).isEqualTo("30s");
    assertThat(fmt.format(59 * S + 999)).isEqualTo("59s");
    assertThat(fmt.format(M)).isEqualTo("1m");
    assertThat(fmt.format(M + S + 10)).isEqualTo("1m 1s");
    assertThat(fmt.format(M + 10)).isEqualTo("1m");
    assertThat(fmt.format(29 * M + S + 10)).isEqualTo("29m 1s");
    assertThat(fmt.format(1 * H + 1 * M + 10 * S)).isEqualTo("1h 1m");
    assertThat(fmt.format(6 * H)).isEqualTo("6h");
    assertThat(fmt.format(6 * H + 10 * S)).isEqualTo("6h");
    assertThat(fmt.format(6 * H + 10 * S)).isEqualTo("6h");
    assertThat(fmt.format(6 * H + 1 * M)).isEqualTo("6h 1m");
    assertThat(fmt.format(1 * D + 1 * M)).isEqualTo("1d");
    assertThat(fmt.format(2 * D + 1 * H + 3 * M + 1 * S)).isEqualTo("2d 1h");
    assertThat(fmt.format(7 * D + 1 * H + 3 * M + 1 * S)).isEqualTo("7d");
    assertThat(fmt.format(14 * D + 1 * H + 3 * M + 1 * S)).isEqualTo("14d");
  }
  
  @Test
  public void testFormatVerboseDuration() {
    final long S = 1000;
    final long M = S * 60;
    final long H = M * 60;
    final long D = H * 24;

    UnitFormatter fmt = Units.DURATION_VERBOSE;
    assertThat(fmt.format(0)).isEqualTo("0 milliseconds");
    assertThat(fmt.format(10)).isEqualTo("10 milliseconds");
    assertThat(fmt.format(S)).isEqualTo("1 second");
    assertThat(fmt.format(S + 1)).isEqualTo("1 second 1 millisecond");
    assertThat(fmt.format(30 * S)).isEqualTo("30 seconds");
    assertThat(fmt.format(59 * S + 999)).isEqualTo("59 seconds");
    assertThat(fmt.format(M)).isEqualTo("1 minute");
    assertThat(fmt.format(M + S + 10)).isEqualTo("1 minute 1 second");
    assertThat(fmt.format(M + 10)).isEqualTo("1 minute");
    assertThat(fmt.format(29 * M + S + 10)).isEqualTo("29 minutes 1 second");
    assertThat(fmt.format(1 * H + 1 * M + 10 * S)).isEqualTo("1 hour 1 minute");
    assertThat(fmt.format(6 * H)).isEqualTo("6 hours");
    assertThat(fmt.format(6 * H + 10 * S)).isEqualTo("6 hours");
    assertThat(fmt.format(6 * H + 10 * S)).isEqualTo("6 hours");
    assertThat(fmt.format(6 * H + 1 * M)).isEqualTo("6 hours 1 minute");
    assertThat(fmt.format(1 * D + 1 * M)).isEqualTo("1 day 1 minute");
    assertThat(fmt.format(2 * D + 1 * H + 3 * M + 1 * S)).isEqualTo("2 days 1 hour 3 minutes");
  }  
}
