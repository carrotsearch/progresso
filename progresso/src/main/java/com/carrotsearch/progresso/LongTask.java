package com.carrotsearch.progresso;

import java.util.function.LongSupplier;

public class LongTask extends Task<LongTracker> {
  public LongTask() {
    super();
  }

  public LongTask(String name) {
    super(name);
  }

  public LongTracker start(long initialValue) {
    return super.start(new LongTracker(this, initialValue));
  }
  
  public LongTracker start(long initialValue, LongSupplier supplier) {
    return super.start(new LongTracker(this, initialValue, supplier));
  }
}
