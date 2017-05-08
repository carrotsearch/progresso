package com.carrotsearch.progresso.util;

import org.assertj.core.api.Assertions;
import org.junit.Test;

import com.carrotsearch.progresso.util.LineFormatter.Alignment;
import com.carrotsearch.progresso.util.LineFormatter.Trim;
import com.carrotsearch.randomizedtesting.RandomizedTest;

public class LineFormatterTest extends RandomizedTest {
  @Test
  public void stretchableField() {
    check("abc",  3, new LineFormatter().cell(0, "abc"));
    check("abc ", 4, new LineFormatter().cell(0, "abc"));
    check("abc",  4, new LineFormatter().cell(0, 3, Alignment.LEFT, "abc"));

    check(  "  ", 2, new LineFormatter().cell(0, "abc"));
    check("a...", 4, new LineFormatter().cell(0, "abcde"));
    check("a...", 4, new LineFormatter().cell(2, "abcde"));
  }

  @Test
  public void alignmentFixedLengthValue() {
    check("abc",    5, new LineFormatter().cell("abc"));
  }
  
  @Test
  public void alignmentLeft() {
    check("abc  ",  5, new LineFormatter().cell(0, Integer.MAX_VALUE, Alignment.LEFT, "abc"));
  }

  @Test
  public void alignmentRight() {
    check("  abc",  5, new LineFormatter().cell(0, Integer.MAX_VALUE, Alignment.RIGHT, "abc"));
  }

  @Test
  public void alignmentAndTrimTruncated() {
    check("...ef",  5, new LineFormatter().cell(0, Integer.MAX_VALUE, Alignment.RIGHT, Trim.RIGHT, "abcdef"));
    check("ab...",  5, new LineFormatter().cell(0, Integer.MAX_VALUE, Alignment.RIGHT, Trim.LEFT, "abcdef"));
    check("...",    3, new LineFormatter().cell(0, Integer.MAX_VALUE, Alignment.RIGHT, Trim.LEFT, "abcdef"));
    check("  ",     2, new LineFormatter().cell(0, Integer.MAX_VALUE, Alignment.RIGHT, Trim.LEFT, "abcdef"));

    check("ab...",  5, new LineFormatter().cell(0, Integer.MAX_VALUE, Alignment.LEFT, Trim.LEFT, "abcdef"));
    check("...ef",  5, new LineFormatter().cell(0, Integer.MAX_VALUE, Alignment.LEFT, Trim.RIGHT, "abcdef"));
    check("...",    3, new LineFormatter().cell(0, Integer.MAX_VALUE, Alignment.LEFT, Trim.RIGHT, "abcdef"));
    check("  ",     2, new LineFormatter().cell(0, Integer.MAX_VALUE, Alignment.LEFT, Trim.RIGHT, "abcdef"));

    check("a...f",  5, new LineFormatter().cell(0, Integer.MAX_VALUE, Alignment.LEFT, Trim.MIDDLE, "abcdef"));
    check("a...",   4, new LineFormatter().cell(0, Integer.MAX_VALUE, Alignment.LEFT, Trim.MIDDLE, "abcdef"));
    check("...",    3, new LineFormatter().cell(0, Integer.MAX_VALUE, Alignment.LEFT, Trim.MIDDLE, "abcdef"));
    check("  ",       2, new LineFormatter().cell(0, Integer.MAX_VALUE, Alignment.LEFT, Trim.MIDDLE, "abcdef"));

    check("",  5, new LineFormatter().cell("abcdef"));
  }

  @Test
  public void simpleFields() {
    check("abc", 3, new LineFormatter().cell("abc"));
    check("abc", 4, new LineFormatter().cell("abc")); // goes unpadded.
    check("", 2, new LineFormatter().cell("abc"));    // Not enough space for a field.
  }

  @Test
  public void multiFields() {
    check("abcdef", 6, new LineFormatter()
        .cell("abc")
        .cell("def"));
    // Unpadded.
    check("abcdef", 7, new LineFormatter()
        .cell("abc")
        .cell("def"));
    // Not enough space for the second field.
    check("abc", 5, new LineFormatter()
        .cell("abc")
        .cell("def"));
  }

  @Test
  public void multiFieldsWithStretchables() {
    check("abcdef ", 7, new LineFormatter() 
        .cell("abc") 
        .cell(0, "def"));
    check("abc def", 7, new LineFormatter()
        .cell(0, "abc") 
        .cell("def"));
    check("abcdef ", 7, new LineFormatter() 
        .cell("abc") 
        .cell(0, "def"));
    check("abc def ", 8, new LineFormatter() 
        .cell(0, "abc") 
        .cell(0, "def"));
  }

  @Test
  public void stretchablesWithLimit() {
    check("abc  |def  ",  100, new LineFormatter() 
        .cell(3, 5, "abc")
        .cell("|")
        .cell(3, 5, "def"));
    
    check("abc  >  <  def  ", 5 + 5 + 3 + 3, new LineFormatter()
        .cell(3, 5, "abc")
        .cell(0, Integer.MAX_VALUE, ">")
        .cell(0, Integer.MAX_VALUE, "<")
        .cell(3, 5, "def"));
  }

  @Test
  public void prioritizedColumns() {
    check("  xyz", 5, new LineFormatter() 
        .cell(3, 8, Alignment.LEFT, Trim.LEFT, LineFormatter.PRIORITY_OPTIONAL, "abc") 
        .cell(3, 8, Alignment.LEFT, Trim.LEFT, LineFormatter.PRIORITY_DEFAULT, "def")
        .cell(3, 8, Alignment.RIGHT, Trim.LEFT, LineFormatter.PRIORITY_HIGH, "xyz"));
    check("def xyz", 7, new LineFormatter() 
        .cell(3, 8, Alignment.LEFT, Trim.LEFT, LineFormatter.PRIORITY_OPTIONAL, "abc") 
        .cell(3, 8, Alignment.LEFT, Trim.LEFT, LineFormatter.PRIORITY_DEFAULT, "def")
        .cell(3, 8, Alignment.RIGHT, Trim.LEFT, LineFormatter.PRIORITY_HIGH, "xyz"));
  }

  private void check(String expected, int width, LineFormatter lf) {
    String actual = lf.format(width);
    if (!actual.equals(expected)) {
      System.out.println("e:" + expected);
      System.out.println("a:" + actual);
    }
    Assertions.assertThat(actual).isEqualTo(expected);
  }
}
