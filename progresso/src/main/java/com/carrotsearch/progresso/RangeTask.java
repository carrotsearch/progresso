package com.carrotsearch.progresso;

import java.util.function.LongSupplier;

import com.carrotsearch.progresso.util.MinMax;

public class RangeTask extends Task<RangeTracker> {
  
  public RangeTask() {
    super();
  }

  public RangeTask(String name) {
    super(name);
  }

  public RangeTracker start(long fromInclusive, long toExclusive) {
    return start(new RangeTracker(this, new MinMax(fromInclusive, toExclusive)));
  }
  
  public RangeTracker start(long fromInclusive, long toExclusive, LongSupplier atSupplier) {
    return start(new RangeTracker(this, new MinMax(fromInclusive, toExclusive), atSupplier));
  }
}
