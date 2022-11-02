package com.carrotsearch.progresso.util;

import static com.carrotsearch.progresso.util.UnitFormatters.*;

import com.carrotsearch.progresso.util.UnitFormatters.ByteFormatter;
import com.carrotsearch.progresso.util.UnitFormatters.DecimalCompactFormatter;
import java.util.Locale;

public final class Units {
  public static final ByteFormatter BYTES = new ByteFormatter();

  public static final DecimalCompactFormatter DECIMAL_COMPACT = new DecimalCompactFormatter();

  public static final UnitFormatter DECIMAL =
      new UnitFormatter() {
        @Override
        public String format(long value) {
          return String.format(Locale.ROOT, "%,d", value);
        }
      };

  public static final UnitFormatter DURATION_COMPACT =
      new UnitFormatter() {
        public String format(long millis) {
          long[] units = splitToUnits(millis);
          compactRounding(millis, units);
          return formatUnits(units, UnitFormatters.TIME_UNITS_SHORT, false);
        }
      };

  public static final UnitFormatter DURATION =
      new UnitFormatter() {
        public String format(long millis) {
          long[] units = splitToUnits(millis);
          regularRounding(millis, units);
          return formatUnits(units, UnitFormatters.TIME_UNITS_SHORT, false);
        }
      };

  public static final UnitFormatter DURATION_VERBOSE =
      new UnitFormatter() {
        @Override
        public String format(long millis) {
          long[] units = splitToUnits(millis);
          regularRounding(millis, units);
          return formatUnits(units, UnitFormatters.TIME_UNITS_FULL, true);
        }
      };

  public static int clamp(int value, int min, int max) {
    if (value < min) return min;
    if (value > max) return max;
    return value;
  }

  public static long clamp(long value, long min, long max) {
    if (value < min) return min;
    if (value > max) return max;
    return value;
  }

  public static float clamp(float value, float min, float max) {
    if (value < min) return min;
    if (value > max) return max;
    return value;
  }

  public static double clamp(double value, double min, double max) {
    if (value < min) return min;
    if (value > max) return max;
    return value;
  }
}
