/** A log4j2 appender that cooperates with progresso's console views. */
module com.carrotsearch.progresso.log4j {
  requires com.carrotsearch.progresso;
  requires org.apache.logging.log4j;
  requires org.apache.logging.log4j.core;

  exports com.carrotsearch.progresso.log4j;
}
