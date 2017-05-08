package com.carrotsearch.progresso.util;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class UnitFormatter {
  private static final long MS = 1;
  private static final long SECOND = MS * 1000;
  private static final long MINUTE = SECOND * 60;
  private static final long HOUR = MINUTE * 60;
  private static final long DAY = HOUR * 24;
  
  private static final long []   TIME_THRESHOLDS  = { DAY, HOUR, MINUTE, SECOND,   MS };
  private static final String [] TIME_UNITS_SHORT = { "d",  "h",    "m",    "s", "ms" };
  private static final String [] TIME_UNITS_FULL  = { " day", " hour", " minute", " second", " millisecond" };

  private static final int IDX_MS = 4;
  private static final int IDX_S  = 3;
  private static final int IDX_M  = 2;
  private static final int IDX_H  = 1;

  public abstract String format(long value);

  public static class ByteFormatter extends UnitFormatter {
    final String [] units = {"KB", "MB", "GB", "TB"}; 

    @Override
    public String format(long bytes) {
      if (bytes < 1024) {
        return Long.toString(bytes) + "B";
      } else {
        double v = bytes;
        for (String unit : units) {
          bytes /= 1024;
          v     /= 1024d;
          if (bytes < 1024) {
            return String.format(Locale.ROOT, "%,.2f%s", v, unit);
          }
        }
        return String.format(Locale.ROOT, "%,.2f%s", v, units[units.length - 1]);
      }
    }

    public long parse(String value) {
      Long val = separatedByGroupingChars(value);
      if (val != null) {
        return val.longValue();
      }

      Pattern p = Pattern.compile("(?<size>[0-9]+)(?<unit>GB|MB|KB)");
      Matcher matcher = p.matcher(value);
      if (matcher.matches()) {
        long size = Long.parseLong(matcher.group("size"));
        switch (matcher.group("unit")) {
          case "KB":
            size = 1024 * size;
            break;
          case "MB":
            size = (1024 * 1024) * size;
            break;
          case "GB":
            size = (1024 * 1024 * 1024) * size;
            break;
          default:
            throw new RuntimeException(); // Inaccessible.
        }
        return size;
      } else {
        return Long.parseLong(value);
      }
    }    
  }

  public static ByteFormatter BYTES = new ByteFormatter();

  public static class DecimalCompactFormatter extends UnitFormatter {
    final String [] units = {"k", "M", "G"}; 

    public long parse(String value) {
      Long val = separatedByGroupingChars(value);
      if (val != null) {
        return val.longValue();
      }

      Pattern p = Pattern.compile("(?<size>[0-9]+)(?<unit>G|M|k)");
      Matcher matcher = p.matcher(value);
      if (matcher.matches()) {
        long size = Long.parseLong(matcher.group("size"));
        switch (matcher.group("unit")) {
          case "k":
            size = 1000 * size;
            break;
          case "M":
            size = (1000_000L) * size;
            break;
          case "G":
            size = (1000_000_000L) * size;
            break;
          default:
            throw new RuntimeException(); // Inaccessible.
        }
        return size;
      } else {
        return Long.parseLong(value);
      }
    }
    
    @Override
    public String format(long value) {
      if (value < 1000) {
        return Long.toString(value);
      } else {
        double v = value;
        for (String unit : units) {
          value /= 1000;
          v     /= 1000d;
          if (value < 1000) {
            return String.format(Locale.ROOT, "%,.2f%s", v, unit);
          }
        }
        return String.format(Locale.ROOT, "%,.2f%s", v, units[units.length - 1]);
      }
    }
  }
  
  public static DecimalCompactFormatter DECIMAL_COMPACT = new DecimalCompactFormatter();

  public static UnitFormatter DECIMAL = new UnitFormatter() {
    @Override
    public String format(long value) {
      return String.format(Locale.ROOT, "%,d", value);
    }
  };

  public static UnitFormatter DURATION_COMPACT = new UnitFormatter() {
    public String format(long millis) {
      long[] units = splitToUnits(millis);
      compactRounding(millis, units);
      return formatUnits(units, TIME_UNITS_SHORT, false);
    }
  };

  public static UnitFormatter DURATION = new UnitFormatter() {
    public String format(long millis) {
      long[] units = splitToUnits(millis);
      regularRounding(millis, units);
      return formatUnits(units, TIME_UNITS_SHORT, false);
    }
  };

  public static UnitFormatter DURATION_VERBOSE = new UnitFormatter() {
    @Override
    public String format(long millis) {
      long[] units = splitToUnits(millis);
      regularRounding(millis, units);
      return formatUnits(units, TIME_UNITS_FULL, true);
    }
  };

  private static void regularRounding(long millis, long[] units) {
    // Apply some ad-hoc heuristics to truncate tiny fields
    // if the duration is large.
    if (millis > 5 * SECOND) {
      units[IDX_MS] = 0;
    }

    if (millis > 30 * MINUTE) {
      units[IDX_S] = 0;
    }
  }      

  private static void compactRounding(long millis, long[] unit) {
    // Don't report milliseconds if beyond one second.
    if (millis >= 1000) {
      unit[IDX_MS] = 0;
    }

    // No seconds after 59m59s
    if (millis >= 60 * MINUTE) {
      unit[IDX_S] = 0;
    }

    // No minutes after 23h59m
    if (millis >= 24 * HOUR) {
      unit[IDX_M] = 0;
    }

    // No hours if longer than a week.
    if (millis >= 7 * DAY) {
      unit[IDX_H] = 0;
    }
  }

  private static String formatUnits(long[] units, String[] unitNames, boolean pluralize) {
    int f = 0;
    while (units[f] == 0 && f + 1 < units.length) { 
      f++;
    }
    int t = TIME_THRESHOLDS.length - 1;
    while (units[t] == 0 && t > f) {
      t--;
    }

    StringBuilder b = new StringBuilder();
    for (;f <= t; f++) {
      if (f == t || units[f] != 0) {
        if (b.length() > 0) {
          b.append(' ');
        }
        b.append(units[f]);
        b.append(unitNames[f]);
        if (pluralize && units[f] != 1) {
          b.append("s");
        }
      }
    }
    return b.toString();
  }

  protected static long[] splitToUnits(long millis) {
    long [] v = new long [TIME_THRESHOLDS.length];
    long x = millis;
    for (int i = 0; i < TIME_THRESHOLDS.length; i++) {
      v[i] = x / TIME_THRESHOLDS[i];
      x -= v[i] * TIME_THRESHOLDS[i];
    }
    return v;
  }
  
  static Long separatedByGroupingChars(String value) {
    // Commas as grouping separators.
    if (value.matches("([0-9]{1,3})|(([0-9]{1,3})(,[0-9]{3})*)")) {
      return Long.parseLong(value.replace(",", ""));
    }

    // Spaces as grouping separators.
    if (value.matches("([0-9]{1,3})|(([0-9]{1,3})([ ][0-9]{3}))*")) {
      return Long.parseLong(value.replace(" ", ""));
    }

    return null;
  }
}
