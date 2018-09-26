package com.carrotsearch.progresso;

import java.util.Objects;
import java.util.function.LongSupplier;

import com.carrotsearch.progresso.util.UnitFormatter;
import com.carrotsearch.progresso.views.console.WithUnit;

public class LongTask extends Task<LongTracker> implements WithUnit {
  private final UnitFormatter unit;

  public LongTask(String name, UnitFormatter unit) {
    super(name);
    this.unit = Objects.requireNonNull(unit);
  }

  public LongTask(UnitFormatter unit) {
    this(UNNAMED, unit);
  }

  public LongTask() {
    this(UnitFormatter.DECIMAL);
  }

  public LongTask(String name) {
    this(name, UnitFormatter.DECIMAL);
  }

  public LongTracker start(long initialValue) {
    return start(new LongTracker(this, initialValue));
  }
  
  public LongTracker start(long initialValue, LongSupplier supplier) {
    return start(new LongTracker(this, initialValue, supplier));
  }
  
  @Override
  public UnitFormatter unit() {
    return unit;
  }
}
