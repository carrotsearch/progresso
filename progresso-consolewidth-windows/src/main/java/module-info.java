/**
 * Console width on Windows, read from the console screen buffer through kernel32 (foreign function
 * API). Requires native access: {@code --enable-native-access=com.carrotsearch.progresso.windows}.
 */
module com.carrotsearch.progresso.windows {
  requires com.carrotsearch.progresso;
  requires java.logging;

  exports com.carrotsearch.progresso.autodetect.windows;

  provides com.carrotsearch.progresso.views.console.ConsoleWidthSupplier with
      com.carrotsearch.progresso.autodetect.windows.WindowsConsoleWidth;
}
