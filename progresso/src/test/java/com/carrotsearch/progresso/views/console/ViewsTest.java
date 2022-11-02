package com.carrotsearch.progresso.views.console;

import com.carrotsearch.progresso.GenericTask;
import com.carrotsearch.progresso.LongTracker;
import com.carrotsearch.progresso.PathScanningTask;
import com.carrotsearch.progresso.Progress;
import com.carrotsearch.progresso.ProgressView;
import com.carrotsearch.progresso.RangeTask;
import com.carrotsearch.progresso.RangeTracker;
import com.carrotsearch.progresso.Task;
import com.carrotsearch.progresso.Tracker;
import com.carrotsearch.randomizedtesting.LifecycleScope;
import com.carrotsearch.randomizedtesting.RandomizedTest;
import com.carrotsearch.randomizedtesting.annotations.Repeat;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.function.Function;
import java.util.stream.Stream;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

@Repeat(iterations = 20)
public class ViewsTest extends RandomizedTest {
  private static Path scanDir;

  @BeforeClass
  public static void generateTestPaths() throws IOException {
    scanDir = newTempDir(LifecycleScope.SUITE);
    Files.createFile(scanDir.resolve("tmpfile1.txt"));
    Files.createFile(scanDir.resolve("tmpfile2.txt"));
  }

  @Test
  public void plainConsoleView() {
    runTest((t) -> new PlainConsoleView(ConsoleAware.writer(), t));
  }

  @Test
  public void quietConsoleView() {
    runTest((t) -> new QuietConsoleView(ConsoleAware.writer(), t));
  }

  @Test
  public void updateableConsoleView() {
    runTest((t) -> new UpdateableConsoleView(ConsoleAware.writer(), t));
  }

  void runTest(Function<Collection<Task<?>>, ProgressView> viewSupplier) {
    try (Progress p = new Progress(viewSupplier.apply(Collections.emptyList()))) {
      runMethods(p);
    }

    try (Progress p = new Progress()) {
      p.newGenericSubtask("closed").start().close();
      p.newRangeSubtask("range").start(0, 10).close();
      p.attach(viewSupplier.apply(Collections.emptyList()));
      runMethods(p);
    }

    try (Progress p = new Progress()) {
      GenericTask t1 = p.newGenericSubtask("closed");
      t1.start().close();
      RangeTask t2 = p.newRangeSubtask("range");
      t2.start(0, 10).close();
      p.attach(viewSupplier.apply(Arrays.asList(t1, t2)));
      runMethods(p);
    }
  }

  void runMethods(Progress p) {
    try (RangeTracker t = p.newByteRangeSubtask("byte range").start(0, 10)) {
      if (randomBoolean()) {
        t.at(randomIntBetween(0, 9));
      }
      if (randomBoolean()) {
        t.close();
      }
    }

    try (Tracker t = p.newGenericSubtask().start()) {
      if (randomBoolean()) {
        t.close();
      }
    }

    try (Tracker t = p.newGenericSubtask("generic").start()) {
      if (randomBoolean()) {
        t.close();
      }
    }

    // skipped tasks.
    p.newLongSubtask("skipped long task").skip();
    p.newRangeSubtask("skipped range task").skip();
    p.newGenericSubtask().skip();

    try (LongTracker t = p.newLongSubtask("long").start(0)) {
      if (randomBoolean()) {
        t.incrementBy(randomIntBetween(0, 10));
      }
      if (randomBoolean()) {
        t.close();
      }
    }

    try (RangeTracker t = p.newRangeSubtask("range").start(0, 10)) {
      if (randomBoolean()) {
        t.at(randomIntBetween(0, 9));
      }
      if (randomBoolean()) {
        t.close();
      }
    }

    // Empty ranges.
    try (RangeTracker t = p.newByteRangeSubtask("empty byte range").start(0, 0)) {}
    try (RangeTracker t = p.newRangeSubtask("empty range").start(0, 0)) {}

    // Do not allow updates on empty ranges.
    try (RangeTracker t = p.newRangeSubtask("subrange").start(0, 0)) {
      try {
        t.at(0);
        Assert.fail();
      } catch (RuntimeException e) {
        // Expected.
      }
    }

    // path scanner.
    try (PathScanningTask.PathTracker t = p.newPathScanningSubtask("path scanner").start();
        Stream<Path> pathStream = Files.walk(scanDir)) {
      pathStream.forEach(t::at);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }
}
