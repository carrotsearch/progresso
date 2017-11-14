package com.carrotsearch.progresso;

import java.util.function.LongSupplier;

public class ByteTask extends Task<ByteTracker> {
  public ByteTask() {
    super();
  }

  public ByteTask(String name) {
    super(name);
  }

  public ByteTracker start(long initialValue) {
    return start(new ByteTracker(this, initialValue));
  }

  public ByteTracker start(long initialValue, LongSupplier atSupplier) {
    return start(new ByteTracker(this, initialValue, atSupplier));
  }  
}
