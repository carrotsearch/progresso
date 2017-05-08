package com.carrotsearch.progresso.views.console;

import java.io.IOException;
import java.io.Writer;

import com.carrotsearch.progresso.util.ColumnCounter;

public class ConsoleWriter extends Writer {
  public final static String LF = System.getProperty("line.separator"); 
  private final static String CR = "\r";

  private final ColumnCounter cc;
  private final Writer delegate;

  private final int lineWidth;
  private int lastLineWidth;
  
  public ConsoleWriter(Writer delegate, ColumnCounter cc, int lineWidth) {
    this.delegate = delegate;
    this.lineWidth = lineWidth;
    this.cc = cc;
  }

  @Override
  public void write(char[] cbuf) throws IOException {
    delegate.write(cbuf);
  }

  @Override
  public void write(int c) throws IOException {
    delegate.write(c);
  }

  @Override
  public void write(char[] cbuf, int off, int len) throws IOException {
    delegate.write(cbuf, off, len);
  }

  @Override
  public void flush() throws IOException {
    delegate.flush();
  }

  @Override
  public void close() throws IOException {
    throw new IOException("Console writer shouldn't be closed.");
  }

  public void updateLine(String content) throws IOException {
    synchronized (super.lock) {
      if (content.indexOf(LF) >= 0) {
        throw new IllegalArgumentException("Line updates can't contain line feeds: " + content); 
      }

      lineOut(content);
    }
  }

  public void printLine(String content) throws IOException {
    synchronized (super.lock) {
      lineOut(content + LF);
    }
  }  

  private void lineOut(String content) throws IOException {
    int break1;
    for (break1 = 0; break1 < content.length(); break1++) {
      char chr = content.charAt(break1);
      if (chr == '\r' || chr == '\n') {
        break;
      }
    }

    int break2 = content.length();
    if (break1 < content.length()) {
      for (break2 = content.length(); break2 > break1; break2--) {
        char chr = content.charAt(break2 - 1);
        if (chr == '\r' || chr == '\n') {
          break;
        }
      }
    }

    String s1;
    if (break1 < content.length()) {
      s1 = content.substring(0, break1);
    } else {
      s1 = content;
    }

    if (lastLineWidth > 0) {
      write(CR);
    }
    write(s1);
    for (int j = cc.columns(s1); j < lastLineWidth; j++) {
      write(' ');
    }

    if (break1 == content.length()) {
      lastLineWidth = cc.columns(s1);
    } else {
      write(content.substring(break1, break2));
      String s3 = content.substring(break2, content.length());
      write(s3);
      lastLineWidth = cc.columns(s3);
    }

    flush();
  }

  public int lineWidth() {
    return lineWidth;
  }
}
