package com.carrotsearch.progresso.jvmversion;

import java.io.PrintStream;

/**
 * A simple utility that emits JVM specification version (normalized to single-digit major). This is
 * useful in script conditionals to determine whether an application can launch on a given JVM or
 * whether special options are necessary.
 *
 * <p><b>This class is not compiled by the build.</b> It has to run on any JVM, including ones far
 * older than what current compilers can target, so the build packages a checked-in class file
 * (version 50, Java 6) from {@code src/main/resources} instead. If this source ever changes,
 * regenerate the binary with a JDK 11 or older compiler and copy it over the resource:
 *
 * <pre>
 * javac -source 1.6 -target 1.6 -d out JvmVersion.java
 * </pre>
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
