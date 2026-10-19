package com.carrotsearch.progresso.views.console;

import java.io.IOException;

/**
 * Prints whole lines to the console in a way that cooperates with a progress view's live output: a
 * view that redraws lines in place implements this to print a line above its live area and redraw.
 * {@link ConsoleAware#printLine(String)} routes through the installed printer, if any.
 */
public interface LinePrinter {
  void printLine(String line) throws IOException;
}
