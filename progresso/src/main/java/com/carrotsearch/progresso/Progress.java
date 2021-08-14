package com.carrotsearch.progresso;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import com.carrotsearch.progresso.Task.Status;
import com.carrotsearch.progresso.annotations.SuppressForbidden;

public final class Progress implements AutoCloseable, Tasks {
  private final List<ProgressView> views = new ArrayList<>();
  private final Object viewSync = new Object();

  private final Thread updaterThread;

  private LinkedHashSet<Task<?>> taskSet = new LinkedHashSet<>();

  private TaskListener taskListener = new TaskListener() {
    @Override
    public void attachedChild(Task<?> task, Task<?> child) {
      attach(child);
    }

    @Override
    public void statusChanged(Task<?> task, Status newStatus) {
      notifyViews();
    }
  };

  public Progress(ProgressView... views) {
    this.updaterThread = new Thread(Progress.this::pollForUpdates, "Progress updater");
    this.updaterThread.setDaemon(true);
    this.updaterThread.start();

    attach(Arrays.asList(views));
  }

  @Override
  public List<Task<?>> subtasks() {
    return new ArrayList<>(taskSet);
  }

  public void attach(ProgressView view) {
    attach(Collections.singleton(view));
  }
  
  public void attach(Collection<? extends ProgressView> views) {
    synchronized (viewSync) {
      this.views.addAll(views);

      Set<Task<?>> tasks = Collections.unmodifiableSet(taskSet);
      views.forEach((view) -> view.update(tasks));
    }
  }

  @Override
  public void attach(Iterable<? extends Task<?>> tasks) {
    synchronized (viewSync) {
      for (Task<?> task : tasks) {
        if (taskSet.add(task)) {
          task.addListener(taskListener);
          // if task already has sub-tasks, add them recursively.
          attach(task.subtasks());
        }
      }
      notifyViews();
    }
  }

  private void notifyViews() {
    Set<Task<?>> tasks = Collections.unmodifiableSet(taskSet);
    synchronized (viewSync) {
      views.forEach((view) -> view.update(tasks));
    }    
  }

  @SuppressForbidden("wait/notify legitimate")
  private void pollForUpdates() {
    assert updaterThread == Thread.currentThread();

    final int pollingFrequency = 500;
    while (!Thread.interrupted()) {
      synchronized (viewSync) {
        try {
          viewSync.wait(pollingFrequency);
          notifyViews();
        } catch (InterruptedException e) {
          break;
        }
      }
    }
  }

  @SuppressForbidden("wait/notify legitimate")
  @Override
  public void close() {
    try {
      if (updaterThread.isAlive()) {
        synchronized (viewSync) {
          updaterThread.interrupt();
        }
        updaterThread.join();
      }
    } catch (InterruptedException e) {
      throw new RuntimeException("Interrupted while waiting for the updater?", e);
    }
  }

  public Set<Task<?>> tasks() {
    synchronized (viewSync) {
      return new LinkedHashSet<>(taskSet);
    }
  }
}