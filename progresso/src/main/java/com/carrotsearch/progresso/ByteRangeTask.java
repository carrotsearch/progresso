package com.carrotsearch.progresso;

import java.util.function.LongSupplier;

import com.carrotsearch.progresso.util.MinMax;

public class ByteRangeTask extends Task<ByteRangeTracker> {
  public ByteRangeTask() {
    super();
  }

  public ByteRangeTask(String name) {
    super(name);
  }

  public ByteRangeTracker start(long fromInclusive, long toExclusive) {
    return start(new ByteRangeTracker(this, new MinMax(fromInclusive, toExclusive)));
  }
  
  public ByteRangeTracker start(long fromInclusive, long toExclusive, LongSupplier atSupplier) {
    return start(new ByteRangeTracker(this, new MinMax(fromInclusive, toExclusive), atSupplier));
  }  
}
