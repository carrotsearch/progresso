package com.carrotsearch.progresso.views.console;

public interface ConsoleWidthSupplier {
  boolean isSupported();
  int getConsoleWidth();
}
