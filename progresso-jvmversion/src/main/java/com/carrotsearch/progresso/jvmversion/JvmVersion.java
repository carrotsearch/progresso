package com.carrotsearch.progresso.jvmversion;

import java.io.PrintStream;

/**
 * A simple utility that emits JVM specification version (normalized to single-digit major). This is
 * useful in script conditionals to determine whether an application can launch on a given JVM or
 * whether special options are necessary.
 */
public class JvmVersion {
  public static void main(String[] args) {
    PrintStream out = System.out;

    String specVersion = System.getProperty("java.specification.version");
    if (specVersion == null) {
      out.print("Empty java.specification.version?");
      out.flush();
      System.exit(1);
    } else {
      if ("1.6".equals(specVersion)) {
        specVersion = "6";
      } else if ("1.7".equals(specVersion)) {
        specVersion = "7";
      } else if ("1.8".equals(specVersion)) {
        specVersion = "8";
      } else if ("1.9".equals(specVersion)) {
        specVersion = "9";
      }
    }

    out.print(specVersion);
    out.flush();
    System.exit(0);
  }
}
