package com.carrotsearch.progresso.views.console;

import java.util.IdentityHashMap;
import java.util.Set;
import java.util.function.Consumer;

import com.carrotsearch.progresso.Task;
import com.carrotsearch.progresso.Task.Status;

/**
 * Simplifies status transition management a bit.
 */
class TaskStatusRecovery {
  static class State {
    Task.Status status;
    
    public State(Task<?> task) {
      update(task);
    }

    public void update(Task<?> task) {
      this.status = task.getStatus();
    }
  }
  
  private IdentityHashMap<Task<?>, State> tasks = new IdentityHashMap<>();

  public synchronized void update(Set<Task<?>> updateSet,
                                  Consumer<Task<?>> taskStarted,
                                  Consumer<Task<?>> taskDone) {
    for (Task<?> task : updateSet) {
      State state = tasks.get(task);
      if (state == null) {
        tasks.put(task, state = new State(task));
        if (state.status == Status.STARTED) {
          taskStarted.accept(task);
        } else if (state.status == Status.DONE) {
          taskDone.accept(task);
        }
      } else {
        if (state.status == Status.NEW)  {
          if (task.getStatus() == Status.STARTED) {
            taskStarted.accept(task);
          }
          if (task.getStatus() == Status.SKIPPED) {
            taskDone.accept(task);
          }
        } else if (state.status == Status.STARTED && task.getStatus() == Status.DONE) {
          taskDone.accept(task);
        }
        state.update(task);
      }
    }
  }
}
