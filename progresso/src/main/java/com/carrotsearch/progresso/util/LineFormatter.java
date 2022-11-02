package com.carrotsearch.progresso.util;

import java.util.ArrayList;

/**
 * Formats fields into a given number of columns. Fields support optional stretching, trimming and
 * alignments.
 */
public class LineFormatter {
  public static final int PRIORITY_OPTIONAL = 0;
  public static final int PRIORITY_DEFAULT = 100;
  public static final int PRIORITY_HIGH = 200;

  public static enum Alignment {
    LEFT,
    RIGHT;
  }

  /** Content trimming if it's too large to fit in a column. */
  public static enum Trim {
    /** ...xxxxx */
    LEFT,

    /** xxxxx... */
    RIGHT,

    /** xx...xx */
    MIDDLE;
  }

  private static class Cell {
    int width;

    final int priority;
    final int minWidth;
    final int maxWidth;
    final String value;
    final Alignment alignment;
    final Trim trim;

    public Cell(
        String value, int minWidth, int maxWidth, Alignment alignment, Trim trim, int priority) {
      this.value = value.toString();
      this.minWidth = minWidth;
      this.maxWidth = maxWidth;
      this.alignment = alignment;
      this.trim = trim;
      this.priority = priority;

      if (minWidth > maxWidth) {
        throw new IllegalArgumentException();
      }
    }

    private boolean stretchable() {
      return minWidth < maxWidth;
    }
  }

  private final ColumnCounter cc;

  public LineFormatter(ColumnCounter cc) {
    this.cc = cc;
  }

  public LineFormatter() {
    this(ColumnCounter.DEFAULT);
  }

  private static String ELLIPSIS = "...";
  private StringBuilder b = new StringBuilder();
  private ArrayList<Cell> cells = new ArrayList<>();

  public LineFormatter cell(String value) {
    return cell(columns(value), columns(value), Alignment.LEFT, value);
  }

  public LineFormatter cell(int minWidth, String value) {
    return cell(minWidth, Integer.MAX_VALUE, Alignment.LEFT, value);
  }

  public LineFormatter cell(int minWidth, int maxWidth, String value) {
    return cell(minWidth, maxWidth, Alignment.LEFT, value);
  }

  public LineFormatter cell(int minWidth, int maxWidth, Alignment alignment, String value) {
    final Trim trim;
    switch (alignment) {
      case LEFT:
        trim = Trim.LEFT;
        break;
      case RIGHT:
        trim = Trim.RIGHT;
        break;
      default:
        throw new AssertionError();
    }
    return cell(minWidth, maxWidth, alignment, trim, PRIORITY_DEFAULT, value);
  }

  public LineFormatter cell(
      int minWidth, int maxWidth, Alignment alignment, Trim trim, String value) {
    return cell(minWidth, maxWidth, alignment, trim, PRIORITY_DEFAULT, value);
  }

  public LineFormatter cell(
      int minWidth, int maxWidth, Alignment alignment, Trim trim, int priority, String value) {
    cells.add(new Cell(value, minWidth, maxWidth, alignment, trim, priority));
    return this;
  }

  public String format(int lineWidth) {
    ArrayList<Cell> thisCells = new ArrayList<>(cells);

    // Find the minimum width at which we can still fit a subset of columns.
    int minWidth;
    while (true) {
      // Short-circuit on no space.
      if (thisCells.isEmpty()) {
        return "";
      }

      minWidth = 0;
      for (Cell c : thisCells) {
        minWidth += c.minWidth;
      }

      if (minWidth <= lineWidth) {
        // Enough space, we're fine.
        break;
      } else {
        // Not enough space. Pick a column and remove it from the set.
        Cell worst = thisCells.get(0);
        for (int i = 1, max = thisCells.size(); i < max; i++) {
          Cell current = thisCells.get(i);
          if (current.priority <= worst.priority) {
            worst = current;
          }
        }
        thisCells.remove(worst);
      }
    }

    // Assign initial width as the minimum width of a cell.
    for (Cell c : thisCells) {
      c.width = c.minWidth;
    }

    if (minWidth < lineWidth) {
      // Filter out only stretchables.
      ArrayList<Cell> stretchables = new ArrayList<>();
      for (Cell c : thisCells) {
        if (c.stretchable()) {
          stretchables.add(c);
        }
      }

      // Distribute paddings over stretchables equally, any excesses from maxWidth
      // stretchables are propagated to the next round. Perhaps there's a direct way
      // to calculate this, but it's beyond me now.
      int padding = lineWidth - minWidth;
      while (padding > 0 && !stretchables.isEmpty()) {
        int perCell = padding / stretchables.size();
        int lastExtra = padding - (perCell * stretchables.size());

        int extraSpace = 0;
        for (int i = stretchables.size(); --i >= 0; lastExtra = 0) {
          Cell c = stretchables.get(i);
          c.width += lastExtra + perCell;
          if (c.width >= c.maxWidth) {
            stretchables.remove(i);
            extraSpace += (c.width - c.maxWidth);
            c.width = c.maxWidth;
          }
        }
        padding = extraSpace;
      }
    }

    this.b.setLength(0);
    for (Cell c : thisCells) {
      String value = c.value;

      int columns = columns(value);
      if (columns != c.width) {
        value = align(c);
      }

      columns = columns(value);
      if (columns > lineWidth) {
        // We can't squeeze any more fields. Break out
        break;
      }

      lineWidth -= columns;
      b.append(value);
    }

    return b.toString();
  }

  private String align(Cell cell) {
    String value = cell.value;
    int valueColumns = columns(value);

    if (cell.width < valueColumns) {
      // We have to trim the value to fit inside cell.width.
      final int max = cell.width - columns(ELLIPSIS);
      if (max == 0) {
        value = ELLIPSIS;
      } else if (max <= 0) {
        value = "";
      } else {
        switch (cell.trim) {
          case LEFT:
            value = value.substring(0, value.offsetByCodePoints(0, max)) + ELLIPSIS;
            break;
          case RIGHT:
            value =
                ELLIPSIS
                    + value.substring(
                        value.offsetByCodePoints(0, value.codePointCount(0, value.length()) - max));
            break;
          case MIDDLE:
            if (max == 1) {
              value = value.substring(0, 1) + ELLIPSIS;
            } else {
              int left = max / 2;
              int right = value.codePointCount(0, value.length()) - left;
              value =
                  value.substring(0, value.offsetByCodePoints(0, left))
                      + ELLIPSIS
                      + value.substring(value.offsetByCodePoints(0, right));
            }
            break;
          default:
            throw new AssertionError();
        }
      }

      // Recalculate
      valueColumns = columns(value);
    }

    int paddingSpaces = cell.width - valueColumns;
    if (paddingSpaces != 0) {
      final int bufferReset = b.length();
      switch (cell.alignment) {
        case LEFT:
          b.append(value);
          for (int i = 0; i < paddingSpaces; i++) {
            b.append(' ');
          }
          break;
        case RIGHT:
          for (int i = 0; i < paddingSpaces; i++) {
            b.append(' ');
          }
          b.append(value);
          break;
        default:
          throw new AssertionError();
      }
      value = b.substring(bufferReset);
      b.setLength(bufferReset);
    }

    return value;
  }

  public int columns(String value) {
    return cc.columns(value);
  }
}
