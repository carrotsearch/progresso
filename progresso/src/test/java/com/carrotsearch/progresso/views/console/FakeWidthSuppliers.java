package com.carrotsearch.progresso.views.console;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Suppliers registered from the test classpath (META-INF/services). Each is inactive unless {@link
 * #widths} holds an entry for it, so other tests see no suppliers besides the property one.
 */
public final class FakeWidthSuppliers {
  /** Supplier's simple class name to the width it should report; absent means unsupported. */
  public static final Map<String, Integer> widths = new ConcurrentHashMap<>();

  private FakeWidthSuppliers() {}

  abstract static class Base implements ConsoleWidthSupplier {
    @Override
    public boolean isSupported() {
      return widths.containsKey(getClass().getSimpleName());
    }

    @Override
    public int getConsoleWidth() {
      return widths.get(getClass().getSimpleName());
    }
  }

  public static final class Low extends Base {
    @Override
    public int priority() {
      return 0;
    }
  }

  public static final class Mid extends Base {
    @Override
    public int priority() {
      return 50;
    }
  }

  public static final class High extends Base {
    @Override
    public int priority() {
      return 100;
    }
  }

  /** Listed after {@link Mid} in the service file, with the same priority. */
  public static final class MidToo extends Base {
    @Override
    public int priority() {
      return 50;
    }
  }
}
