package com.carrotsearch.progresso;

public class ByteTracker extends LongTracker {
  public ByteTracker(Task<?> task, long initialValue) {
    super(task, initialValue);
  }
}
