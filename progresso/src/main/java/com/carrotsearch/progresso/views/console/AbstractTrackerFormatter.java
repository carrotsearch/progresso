package com.carrotsearch.progresso.views.console;

import com.carrotsearch.progresso.Task;
import com.carrotsearch.progresso.Tracker;
import com.carrotsearch.progresso.util.LineFormatter;
import com.carrotsearch.progresso.util.LineFormatter.Alignment;
import com.carrotsearch.progresso.util.LineFormatter.Trim;

public abstract class AbstractTrackerFormatter<T> implements TrackerFormatter {

  /* */
  @Override
  public abstract boolean supports(int lineWidth, Tracker tracker);

  /* */
  @SuppressWarnings("unchecked")
  @Override
  public final void format(LineFormatter lf, int lineWidth, Task<?> task, Tracker tracker) {
    doFormat(lf, lineWidth, task, (T) tracker);
  }

  protected abstract void doFormat(LineFormatter lf, int lineWidth, Task<?> task, T tracker);

  protected static void appendOptional(LineFormatter lf, String value) {
    int cols = lf.getColumnCounter().columns(value);
    lf.cell(cols, cols, Alignment.LEFT, Trim.LEFT, LineFormatter.PRIORITY_OPTIONAL, value);
  }

  /** Renders the classic ASCII progress bar; see {@link #dots(ProgressBarStyle, int, double)}. */
  protected static String dots(int lineWidth, double completedRatio) {
    return dots(ProgressBarStyle.ASCII, lineWidth, completedRatio);
  }

  /**
   * Renders a progress bar in the given style. The bar's width depends on the available line width,
   * see {@link #progressBarWidth(int)}.
   */
  protected static String dots(ProgressBarStyle style, int lineWidth, double completedRatio) {
    return style.render(progressBarWidth(lineWidth), completedRatio);
  }

  /** Returns the width of the progress bar (including delimiters) for the given line width. */
  protected static int progressBarWidth(int lineWidth) {
    if (lineWidth <= 80) {
      return 4 * 5 + 1;
    } else if (lineWidth <= 120) {
      return 4 * 7 + 1;
    } else {
      return 4 * 9 + 1;
    }
  }
}
