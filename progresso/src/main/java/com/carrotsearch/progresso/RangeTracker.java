package com.carrotsearch.progresso;

import java.util.Objects;
import java.util.function.LongSupplier;

import com.carrotsearch.progresso.util.MinMax;

public class RangeTracker extends LongTracker implements CompletedRatio {
  public final MinMax minMax;

  public RangeTracker(Task<?> task, MinMax minmax, LongSupplier atSupplier) {
    super(task, minmax.minInclusive, atSupplier);
    this.minMax = Objects.requireNonNull(minmax);
  }

  public RangeTracker(Task<?> task, MinMax minmax) {
    this(task, minmax, null);
  }
  
  @Override
  public void at(long newValue) {
    super.at(withinMinMax(newValue));
  }

  private long withinMinMax(long value) {
    if (!minMax.isWithin(value)) {
      throw new RuntimeException("Value out of tracker bounds: " + value + ", " + minMax + ", task: " + task().getName());
    }
    return value;
  }

  @Override
  public long at() {
    // L4G-772: do return the minimum on empty ranges, even though it's excluded. While this doesn't make
    // sense logically, it makes the code less complex in other places.
    if (isEmptyRange()) {
      return minMax.minInclusive;
    } else {
      return withinMinMax(super.at());
    }
  }
  
  @Override
  protected void close0() {
    assert at() == 0 || true; // Exception or always true.
  }

  private boolean isEmptyRange() {
    return minMax.emptyRange();
  }

  @Override
  public void increment() {
    super.increment();
    withinMinMax(at());
  }

  @Override
  public void incrementBy(long value) {
    super.incrementBy(value);
    withinMinMax(at());
  }

  @Override
  public double completedRatio() {
    // If the range is empty or a single-element then there's a simplified
    // "view" of task's completion -> either it's done or it's not.
    if (minMax.maxExclusive - minMax.minInclusive <= 1) {
      if (task().isDone()) {
        return 1d;
      } else {
        return 0d;
      }
    } else {
      return ((double) at() - minMax.minInclusive) / (minMax.maxExclusive - minMax.minInclusive - 1);
    }
  }
  
  @Override
  public String toString() {
    return "RangeTracker, @" + at() + ", " + minMax;
  }
}
