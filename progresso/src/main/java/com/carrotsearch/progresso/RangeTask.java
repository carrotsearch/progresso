package com.carrotsearch.progresso;

import com.carrotsearch.progresso.util.MinMax;
import com.carrotsearch.progresso.util.UnitFormatter;
import com.carrotsearch.progresso.util.Units;
import com.carrotsearch.progresso.views.console.WithUnit;
import java.util.function.LongSupplier;

public class RangeTask extends Task<RangeTracker> implements WithUnit {

  private final UnitFormatter unit;

  public RangeTask(UnitFormatter unit) {
    this(UNNAMED, Units.DECIMAL);
  }

  public RangeTask(String name, UnitFormatter unit) {
    super(name);
    this.unit = unit;
  }

  public RangeTask() {
    this(Units.DECIMAL);
  }

  public RangeTask(String name) {
    this(name, Units.DECIMAL);
  }

  public RangeTracker start(long fromInclusive, long toExclusive) {
    return start(new RangeTracker(this, new MinMax(fromInclusive, toExclusive)));
  }

  public RangeTracker start(long fromInclusive, long toExclusive, LongSupplier atSupplier) {
    return start(new RangeTracker(this, new MinMax(fromInclusive, toExclusive), atSupplier));
  }

  @Override
  public UnitFormatter unit() {
    return unit;
  }
}
