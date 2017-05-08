package com.carrotsearch.progresso;

import java.util.function.LongSupplier;

import com.carrotsearch.progresso.util.MinMax;

public class ByteRangeTracker extends RangeTracker {
  public ByteRangeTracker(Task<?> owner, MinMax minmax) {
    this(owner, minmax, null);
  }
  
  public ByteRangeTracker(Task<?> owner, MinMax minmax, LongSupplier atSupplier) {
    super(owner, minmax, atSupplier);
  }
}