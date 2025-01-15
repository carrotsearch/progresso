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

  protected static String dots(int lineWidth, double completedRatio) {
    final int progressBarWidth;
    if (lineWidth <= 80) {
      progressBarWidth = 4 * 5 + 1;
    } else if (lineWidth <= 120) {
      progressBarWidth = 4 * 7 + 1;
    } else {
      progressBarWidth = 4 * 9 + 1;
    }

    final int tickModulo = (progressBarWidth - 1) / 4;

    final StringBuilder out = new StringBuilder();
    final int atColumn = (int) (completedRatio * (progressBarWidth - 1));
    for (int i = 0; i < progressBarWidth; i++) {
      final char chr;

      if (i == 0) {
        chr = '[';
      } else if (i == progressBarWidth - 1) {
        chr = ']';
      } else if (i == atColumn) {
        chr = '>';
      } else {
        if (i % tickModulo == 0) {
          chr = ':';
        } else {
          if (i < atColumn) {
            chr = '=';
          } else {
            chr = ' ';
          }
        }
      }

      out.append(chr);
    }

    return out.toString();
  }
}
