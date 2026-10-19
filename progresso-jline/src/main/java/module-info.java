/** Console views and width detection on top of JLine (experimental). */
module com.carrotsearch.progresso.jline {
  requires com.carrotsearch.progresso;
  requires java.logging;
  requires transitive org.jline.terminal;

  exports com.carrotsearch.progresso.jline;

  provides com.carrotsearch.progresso.views.console.ConsoleWidthSupplier with
      com.carrotsearch.progresso.jline.JLineConsoleWidth;
}
