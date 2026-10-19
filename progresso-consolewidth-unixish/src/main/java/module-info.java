/** Console width from the COLUMNS environment variable (Unix-like systems and Cygwin). */
module com.carrotsearch.progresso.consolewidth_unixish {
  requires com.carrotsearch.progresso;
  requires java.logging;

  exports com.carrotsearch.progresso.autodetect;

  provides com.carrotsearch.progresso.views.console.ConsoleWidthSupplier with
      com.carrotsearch.progresso.autodetect.UnixishWidthFromColumnsEnvVariable;
}
