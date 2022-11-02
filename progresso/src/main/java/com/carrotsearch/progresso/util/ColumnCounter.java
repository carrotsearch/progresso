package com.carrotsearch.progresso.util;

public interface ColumnCounter {
  /**
   * This for now assumes codepoint == one column. This is not exactly true <a
   * href="https://en.wikipedia.org/wiki/Halfwidth_and_fullwidth_forms">for certain Unicode
   * glyphs</a> but we don't deal with this complexity here.
   */
  public static final ColumnCounter DEFAULT = (s) -> s.codePointCount(0, s.length());

  public int columns(String s);
}
