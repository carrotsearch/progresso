package com.carrotsearch.progresso;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

public abstract class Task<T extends Tracker> implements Tasks {
  public static enum Status {
    NEW,
    STARTED,
    SKIPPED,
    DONE;
  }

  // Unique sentinel object.
  protected static final String UNNAMED = new String("<unnamed task>");

  private final String name;
  private final List<TaskListener> taskListeners = new CopyOnWriteArrayList<>();
  private final List<Task<?>> children = new CopyOnWriteArrayList<>();
  private final List<Attribute> attributes = new CopyOnWriteArrayList<>();

  private final AtomicReference<Task<?>> parent = new AtomicReference<Task<?>>();

  private Status status = Status.NEW;
  private Instant lastStatusChange = Instant.now();
  private T tracker;

  /** Helps in debugging where a given task was instantiated. */
  private final String allocationStack =
      Arrays.asList(Thread.currentThread().getStackTrace()).stream()
          .map((e) -> e.toString())
          .collect(Collectors.joining("\n"));

  public Task() {
    this(UNNAMED);
  }

  public Task(String name) {
    if (name == null) {
      throw new IllegalArgumentException("Name cannot be null.");
    }
    this.name = name;
  }

  public final Status getStatus() {
    return status;
  }

  public final T getTracker() {
    if (tracker == null) {
      throw new RuntimeException("A tracker is not available in task state: " + getStatus());
    }
    return tracker;
  }

  public void skip() {
    if (getStatus() != Status.NEW) {
      throw new RuntimeException("Only new tasks can be skipped: " + this);
    }

    setStatus(Status.SKIPPED);
  }

  public boolean hasTracker() {
    return tracker != null;
  }

  public boolean hasName() {
    return name != UNNAMED;
  }

  public String getName() {
    return name;
  }

  public Instant getLastStatusChangeTime() {
    return lastStatusChange;
  }

  @Override
  public void attach(Iterable<? extends Task<?>> tasks) {
    for (Task<?> child : tasks) {
      child.setParent(this);
      children.add(child);
      taskListeners.forEach((c) -> c.attachedChild(this, child));
    }
  }

  public Task<?> getParent() {
    return parent.get();
  }

  public List<Task<?>> subtasks() {
    return children;
  }

  private void setParent(Task<?> newParent) {
    if (!parent.compareAndSet(null, newParent)) {
      if (parent.get() != newParent) {
        throw new RuntimeException(
            "A task must be bound to only one parent: "
                + this
                + " parent: "
                + parent.get()
                + ", newParent: "
                + newParent);
      }
    }

    taskListeners.forEach((c) -> c.attachedParent(this, newParent));
  }

  protected T start(T tracker) {
    if (this.tracker != null) {
      throw new RuntimeException("A task cannot be started twice.");
    }
    if (tracker.task != this) {
      throw new RuntimeException("Tracker belongs to a different task.");
    }
    if (getStatus() != Status.NEW) {
      throw new RuntimeException("Task cannot be started in this status: " + this);
    }

    this.tracker = Objects.requireNonNull(tracker);
    setStatus(Status.STARTED);
    return tracker;
  }

  void setStatus(Status newStatus) {
    this.status = newStatus;
    this.lastStatusChange = Instant.now();
    taskListeners.forEach((c) -> c.statusChanged(this, newStatus));
  }

  void addListener(TaskListener listener) {
    this.taskListeners.add(listener);
  }

  public boolean isDone() {
    return getStatus() == Status.DONE || getStatus() == Status.SKIPPED;
  }

  @Override
  public String toString() {
    StringBuilder b = new StringBuilder();
    b.append("Task: ");
    b.append(name);
    b.append(" ");
    b.append(status);

    Tracker t = this.tracker;
    if (t != null) {
      b.append(", ");
      b.append(tracker);
    }
    return b.toString();
  }

  public boolean isChildOf(Task<?> candidate) {
    if (candidate != null) {
      for (Task<?> parent = this.parent.get(); parent != null; parent = parent.parent.get()) {
        if (candidate == parent) {
          return true;
        }
      }
    }
    return false;
  }

  public Task<?> attribute(String description, String argsFormat, Object... args) {
    attributes.add(new Attribute(description, String.format(Locale.ROOT, argsFormat, args)));
    return this;
  }

  public List<Attribute> attributes() {
    return attributes;
  }

  public String instantiationStack() {
    return allocationStack;
  }
}
