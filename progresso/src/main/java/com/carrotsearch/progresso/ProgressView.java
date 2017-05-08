package com.carrotsearch.progresso;

import java.util.Set;

public interface ProgressView {
  void update(Set<Task<?>> tasks);
}