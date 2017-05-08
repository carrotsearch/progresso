package com.carrotsearch.progresso;

import com.carrotsearch.progresso.Task.Status;

interface TaskListener {
  default void attachedChild(Task<?> task, Task<?> child) {}
  default void attachedParent(Task<?> task, Task<?> parent) {}
  default void statusChanged(Task<?> task, Status newStatus) {}
}
