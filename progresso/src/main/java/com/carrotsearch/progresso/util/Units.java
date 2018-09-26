package com.carrotsearch.progresso.util;

import static com.carrotsearch.progresso.util.UnitFormatterImpl.*;

import java.util.Locale;

import com.carrotsearch.progresso.util.UnitFormatterImpl.ByteFormatter;
import com.carrotsearch.progresso.util.UnitFormatterImpl.DecimalCompactFormatter;

public final class Units {
  public static final ByteFormatter BYTES = new ByteFormatter();

  public static final DecimalCompactFormatter DECIMAL_COMPACT = new DecimalCompactFormatter();

  public static final UnitFormatter DECIMAL = new UnitFormatter() {
    @Override
    public String format(long value) {
      return String.format(Locale.ROOT, "%,d", value);
    }
  };
  
  public static final UnitFormatter DURATION_COMPACT = new UnitFormatter() {
    public String format(long millis) {
      long[] units = splitToUnits(millis);
      compactRounding(millis, units);
      return formatUnits(units, UnitFormatterImpl.TIME_UNITS_SHORT, false);
    }
  };
  
  public static final UnitFormatter DURATION = new UnitFormatter() {
    public String format(long millis) {
      long[] units = splitToUnits(millis);
      regularRounding(millis, units);
      return formatUnits(units, UnitFormatterImpl.TIME_UNITS_SHORT, false);
    }
  };

  public static final UnitFormatter DURATION_VERBOSE = new UnitFormatter() {
    @Override
    public String format(long millis) {
      long[] units = splitToUnits(millis);
      regularRounding(millis, units);
      return formatUnits(units, UnitFormatterImpl.TIME_UNITS_FULL, true);
    }
  };
}
