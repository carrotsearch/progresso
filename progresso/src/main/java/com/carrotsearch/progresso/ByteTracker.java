package com.carrotsearch.progresso;

import java.util.function.LongSupplier;

public class ByteTracker extends LongTracker {
  public ByteTracker(Task<?> task, long initialValue) {
    super(task, initialValue);
  }

  public ByteTracker(Task<?> task, long initialValue, LongSupplier atSupplier) {
    super(task, initialValue, atSupplier);
  }
}
