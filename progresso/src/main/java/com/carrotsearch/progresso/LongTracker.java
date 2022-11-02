package com.carrotsearch.progresso;

import java.util.concurrent.atomic.AtomicLong;
import java.util.function.LongSupplier;

public class LongTracker extends Tracker {
  private AtomicLong value;
  private final LongSupplier atSupplier;

  public LongTracker(Task<?> task, long initialValue, LongSupplier atSupplier) {
    super(task);
    this.value = new AtomicLong(initialValue);
    this.atSupplier = atSupplier;
  }

  public LongTracker(Task<?> task, long initialValue) {
    this(task, initialValue, null);
  }

  @Override
  public long modHash() {
    return at();
  }

  public long at() {
    return atSupplier == null ? value.get() : atSupplier.getAsLong();
  }

  public void at(long newValue) {
    ensureOpen();
    ensureNoSupplier();

    if (value.get() > newValue) {
      throw new IllegalArgumentException(
          "New value must be >= old value: " + newValue + ", old: " + this.value);
    }

    value.set(newValue);
  }

  public void increment() {
    ensureNoSupplier();
    this.value.incrementAndGet();
  }

  public void incrementBy(long value) {
    ensureNoSupplier();
    this.value.addAndGet(value);
  }

  private void ensureNoSupplier() {
    if (atSupplier != null) {
      throw new RuntimeException("Can't set when supplier is provided.");
    }
  }
}
