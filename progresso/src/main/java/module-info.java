/** Task management and progress reporting with console views. */
module com.carrotsearch.progresso {
  exports com.carrotsearch.progresso;
  exports com.carrotsearch.progresso.annotations;
  exports com.carrotsearch.progresso.util;
  exports com.carrotsearch.progresso.views.console;

  uses com.carrotsearch.progresso.views.console.ConsoleWidthSupplier;
  uses com.carrotsearch.progresso.views.console.ConsoleViewSupplier;

  provides com.carrotsearch.progresso.views.console.ConsoleWidthSupplier with
      com.carrotsearch.progresso.views.console.ConsoleWidthFromSystemProperty;
  provides com.carrotsearch.progresso.views.console.ConsoleViewSupplier with
      com.carrotsearch.progresso.views.console.UpdateableConsoleViewSupplier,
      com.carrotsearch.progresso.views.console.PlainConsoleViewSupplier;
}
