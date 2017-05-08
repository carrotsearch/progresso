package com.carrotsearch.progresso;

import java.util.IdentityHashMap;

public class CompositeTask extends Task<CompositeTask.WeightedTracker> {
  public final class WeightedTracker extends Tracker implements CompletedRatio {
    private final double totalWeight;

    public WeightedTracker() {
      super(CompositeTask.this);
      this.totalWeight = weights.values().stream().mapToDouble((v) -> v).sum();
    }

    @Override
    public long modHash() {
      return Double.doubleToLongBits(completedRatio());
    }
    
    @Override
    public double completedRatio() {
      if (weights.isEmpty()) {
        return task().isDone() ? 1d : 0d;
      }

      double current = weights.entrySet().stream().mapToDouble((e) -> {
        Task<?> task = e.getKey();
        double weight = e.getValue();

        if (task.hasTracker() && task.getTracker() instanceof CompletedRatio) {
          return ((CompletedRatio) task.getTracker()).completedRatio() * weight;
        } else {
          return (task.isDone() ? 1d : 0d) * weight;
        }
      }).sum();

      return current / totalWeight;
    }
  }

  private final IdentityHashMap<Task<?>, Double> weights = new IdentityHashMap<>();

  public CompositeTask(String name) {
    super(name);
  }
  
  public CompositeTask() {
    super();
  }

  @Override
  public <T extends Task<?>> T attach(T task) {
    return attach(task, 1d);
  }

  public <T extends Task<?>> T attach(T task, double weight) {
    if (getStatus() != Status.NEW) {
      throw new RuntimeException("Tasks cannot be attached to a composite once it's started: "
          + task);
    }

    weights.put(task, weight);
    return super.attach(task);
  }

  public Tracker start() {
    return super.start(new WeightedTracker());
  }
}
