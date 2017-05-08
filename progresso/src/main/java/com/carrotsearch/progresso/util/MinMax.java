package com.carrotsearch.progresso.util;

public final class MinMax {
  public final long minInclusive;
  public final long maxExclusive;

  public MinMax(long minInclusive, long maxExclusive) {
    if (minInclusive > maxExclusive) {
      throw new IllegalArgumentException("Invalid range: [" + minInclusive + ";" + maxExclusive + ")");
    }
    this.minInclusive = minInclusive;
    this.maxExclusive = maxExclusive;
  }
  
  @Override
  public String toString() {
    return "[" +  minInclusive + "; " + maxExclusive + ")";
  }

  public boolean isWithin(long value) {
    return value >= minInclusive && value < maxExclusive;
  }

  public boolean emptyRange() {
    return minInclusive == maxExclusive;
  }
}
