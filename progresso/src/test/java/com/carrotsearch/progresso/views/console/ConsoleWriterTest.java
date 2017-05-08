package com.carrotsearch.progresso.views.console;

import java.io.StringWriter;

import org.assertj.core.api.Assertions;
import org.junit.Test;

import com.carrotsearch.progresso.util.ColumnCounter;
import com.carrotsearch.randomizedtesting.RandomizedTest;

@SuppressWarnings("resource")
public class ConsoleWriterTest extends RandomizedTest {
  @Test
  public void testPrintLine() throws Exception {
    StringWriter sw = new StringWriter();
    ConsoleWriter cw = new ConsoleWriter(sw, ColumnCounter.DEFAULT, 80);
    cw.printLine("12345");
    Assertions.assertThat(sw.toString()).isEqualTo("12345" + ConsoleWriter.LF);
    
    sw.getBuffer().setLength(0);
    cw.printLine("abcdef");
    Assertions.assertThat(sw.toString()).isEqualTo("abcdef" + ConsoleWriter.LF);

    sw.getBuffer().setLength(0);
    cw.printLine("");
    Assertions.assertThat(sw.toString()).isEqualTo(ConsoleWriter.LF);
    
    sw.getBuffer().setLength(0);
    cw.printLine("");
    Assertions.assertThat(sw.toString()).isEqualTo(ConsoleWriter.LF);    
  }

  @Test
  public void testUpdateLine() throws Exception {
    StringWriter sw = new StringWriter();
    ConsoleWriter cw = new ConsoleWriter(sw, ColumnCounter.DEFAULT, 80);

    cw.updateLine("12345");
    Assertions.assertThat(sw.toString()).isEqualTo("12345");

    // CR used to overprint last line.
    sw.getBuffer().setLength(0);
    cw.updateLine("abcde");
    Assertions.assertThat(sw.toString()).isEqualTo("\rabcde");

    // Check padding to remove previous line's content.
    sw.getBuffer().setLength(0);
    cw.updateLine("123");
    Assertions.assertThat(sw.toString()).isEqualTo("\r123  ");
    
    // Only remove what's actually needed from last line.
    sw.getBuffer().setLength(0);
    cw.updateLine("ab");
    Assertions.assertThat(sw.toString()).isEqualTo("\rab ");
  }
  
  @Test
  public void testCrInside() throws Exception {
    StringWriter sw = new StringWriter();
    ConsoleWriter cw = new ConsoleWriter(sw, ColumnCounter.DEFAULT, 80);

    cw.updateLine("12345");
    Assertions.assertThat(sw.toString()).isEqualTo("12345");

    sw.getBuffer().setLength(0);
    cw.updateLine("abc\rde");
    Assertions.assertThat(sw.toString()).isEqualTo("\rabc  \rde");
  }

  @Test
  public void testCrLfInside() throws Exception {
    StringWriter sw = new StringWriter();
    ConsoleWriter cw = new ConsoleWriter(sw, ColumnCounter.DEFAULT, 80);

    cw.updateLine("12345");
    Assertions.assertThat(sw.toString()).isEqualTo("12345");

    sw.getBuffer().setLength(0);
    cw.printLine("abc\r\nde");
    Assertions.assertThat(sw.toString()).isEqualTo("\rabc  \r\nde" + ConsoleWriter.LF);
  }
}
