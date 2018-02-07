package com.carrotsearch.progresso.log4j;

import java.io.IOException;
import java.io.UncheckedIOException;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginElement;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.apache.logging.log4j.core.impl.ThrowableProxy;

import com.carrotsearch.progresso.views.console.ConsoleAware;
import com.carrotsearch.progresso.views.console.ConsoleWriter;

/**
 * A log4j appender that is compatible with progresso console views so that
 * if a task is being tracked (and its progress is displayed), the log message
 * is gracefully displayed and the progress resumed.
 */
@Plugin(name = "ProgressAwareAppender", category = "Core", elementType = "appender", printObject = false)
public class ProgressAwareAppender extends AbstractAppender {
  private final ConsoleWriter consoleWriter = ConsoleAware.writer();

  /**
   * Print stack traces of logging events.
   */
  private boolean printStackTraces = true;

  public ProgressAwareAppender(String name, Filter filter) {
    super(name, filter, null, true);
  }
  
  public boolean isPrintStackTraces() {
    return printStackTraces;
  }
  
  public void setPrintStackTraces(boolean printStackTraces) {
    this.printStackTraces = printStackTraces;
  }

  @Override
  public void append(LogEvent event) {
    StringBuilder builder = new StringBuilder("> ");
    if (event.getLevel().isMoreSpecificThan(Level.WARN)) {
      if (event.getLevel() == Level.WARN) {
        builder.append("[WARNING]: ");
      } else {
        builder.append("[").append(event.getLevel()).append("]: ");
      }
    }
    builder.append(event.getMessage().getFormattedMessage());

    if (printStackTraces) {
    ThrowableProxy thrownProxy = event.getThrownProxy();
      if (thrownProxy != null) {
        builder.append(ConsoleWriter.LF);
        builder.append(thrownProxy.getCauseStackTraceAsString(""));
      }
    }

    try {
      consoleWriter.printLine(builder.toString());
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }
  
  @PluginFactory
  public static ProgressAwareAppender createAppender(
          @PluginAttribute("name") final String name,
          @PluginAttribute("printStackTraces") final boolean printStackTraces,
          @PluginElement("Filter") final Filter filter) {
      if (name == null) {
          LOGGER.error("No name provided");
          return null;
      }

      ProgressAwareAppender progressAwareAppender = new ProgressAwareAppender(name, filter);
      progressAwareAppender.setPrintStackTraces(printStackTraces);
      return progressAwareAppender;
  }
}
