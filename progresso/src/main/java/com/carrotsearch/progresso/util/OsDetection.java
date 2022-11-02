package com.carrotsearch.progresso.util;

import java.util.Objects;

public final class OsDetection {
  public static final String OS_NAME;

  public static final boolean IS_OS_WINDOWS;
  public static final boolean IS_OS_MAC_OSX;
  public static final boolean IS_OS_LINUX;

  public static final boolean IS_OS_UNIXISH;

  public static final boolean IS_CYGWIN;

  static {
    OS_NAME = Objects.requireNonNull(System.getProperty("os.name"));
    IS_OS_WINDOWS = OS_NAME.startsWith("Windows");
    IS_OS_MAC_OSX = OS_NAME.startsWith("Mac OS X");
    IS_OS_LINUX = OS_NAME.startsWith("Linux") || OS_NAME.startsWith("LINUX");

    boolean IS_OS_SOLARIS = OS_NAME.startsWith("Solaris");
    boolean IS_OS_FREE_BSD = OS_NAME.startsWith("FreeBSD");
    boolean IS_OS_OPEN_BSD = OS_NAME.startsWith("OpenBSD");
    boolean IS_OS_NET_BSD = OS_NAME.startsWith("NetBSD");

    IS_OS_UNIXISH =
        IS_OS_LINUX
            || IS_OS_MAC_OSX
            || IS_OS_SOLARIS
            || IS_OS_FREE_BSD
            || IS_OS_OPEN_BSD
            || IS_OS_NET_BSD;

    // A crude heuristic but we don't want to run external commands (uname) and still
    // be able to quickly detect it.
    IS_CYGWIN =
        IS_OS_WINDOWS && (System.getenv("SHELL") != null && System.getenv("ORIGINAL_PATH") != null);
  }
}
