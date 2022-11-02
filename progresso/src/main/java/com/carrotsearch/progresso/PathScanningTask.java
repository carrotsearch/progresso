package com.carrotsearch.progresso;

import com.carrotsearch.progresso.PathScanningTask.PathTracker;
import java.nio.file.Path;
import java.util.Objects;

public class PathScanningTask extends Task<PathTracker> {
  public class PathTracker extends Tracker {
    private volatile Path current;
    private volatile long count;

    public PathTracker() {
      super(PathScanningTask.this);
    }

    @Override
    public long modHash() {
      return count();
    }

    public Path at() {
      return current;
    }

    public long count() {
      return count;
    }

    public Path at(Path path) {
      ensureOpen();
      this.current = Objects.requireNonNull(path);
      this.count++;
      return path;
    }
  }

  public PathScanningTask(String name) {
    super(name);
  }

  public PathTracker start() {
    return start(new PathTracker());
  }
}
