package com.carrotsearch.progresso.demos;

import com.carrotsearch.progresso.PathScanningTask.PathTracker;
import com.carrotsearch.progresso.Progress;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.concurrent.TimeUnit;
import org.junit.Test;

/** Path scanning has observable progress, but no explicit "range". */
public class E002_PathScanner extends AbstractExampleTest {
  @Test
  public void pathScanner() throws IOException {
    try (Progress progress = defaultProgress()) {
      final long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(4);
      final ArrayDeque<Path> paths = new ArrayDeque<>();
      FileSystems.getDefault().getRootDirectories().forEach((p) -> paths.add(p));
      try (PathTracker tracker = progress.newPathScanningSubtask("Scanning folders").start()) {
        while (!paths.isEmpty() && System.nanoTime() < deadline) {
          Path p = paths.pop();
          try {
            if (Files.isDirectory(p)
                && Files.isReadable(p)
                && Files.isExecutable(p)
                && !Files.isSymbolicLink(p)) {
              Files.list(p).forEach(s -> paths.add(tracker.at(s)));
            }
          } catch (InternalError e) {
            // Happens on my machine when resolving certain links (thrown by the JDK)...
          }
        }
      }
    }
  }
}
