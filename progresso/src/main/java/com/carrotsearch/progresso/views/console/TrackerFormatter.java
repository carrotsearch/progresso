package com.carrotsearch.progresso.views.console;

import com.carrotsearch.progresso.Task;
import com.carrotsearch.progresso.Tracker;
import com.carrotsearch.progresso.util.LineFormatter;

public interface TrackerFormatter {
  void taskStarted(Task<?> task);
  boolean supports(int lineWidth, Tracker tracker);
  void format(LineFormatter lf, int lineWidth, Task<?> task, Tracker tracker);
}