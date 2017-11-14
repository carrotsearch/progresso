package com.carrotsearch.progresso;

import java.util.Arrays;
import java.util.List;

public interface Tasks {
  void attach(Iterable<? extends Task<?>> tasks);

  default <T extends Task<?>> T attach(T task) {
    attach(Arrays.asList(task));
    return task;
  }

  List<Task<?>> subtasks();

  default ByteRangeTask newByteRangeSubtask(String name) { return attach(newByteRangeTask(name)); }
  static  ByteRangeTask newByteRangeTask(String name) { return new ByteRangeTask(name); }
  static  ByteRangeTask newByteRangeTask() { return new ByteRangeTask(); }

  default ByteTask newByteSubtask(String name) { return attach(newByteTask(name)); }
  static  ByteTask newByteTask(String name) { return new ByteTask(name); }
  static  ByteTask newByteTask() { return new ByteTask(); }

  default RangeTask newRangeSubtask(String name) { return attach(newRangeTask(name)); }
  static  RangeTask newRangeTask(String name) { return new RangeTask(name); }
  static  RangeTask newRangeTask() { return new RangeTask(); }

  default LongTask newLongSubtask(String name) { return attach(newLongTask(name)); }
  static  LongTask newLongTask(String name) { return new LongTask(name); }
  static  LongTask newLongTask() { return new LongTask(); }

  default PathScanningTask newPathScanningSubtask(String name) { return attach(newPathScanningTask(name)); }
  static  PathScanningTask newPathScanningTask(String name) { return new PathScanningTask(name); }

  default GenericTask newGenericSubtask(String name) { return attach(newGenericTask(name)); }
  static  GenericTask newGenericTask(String name) { return new GenericTask(name); }

  default GenericTask newGenericSubtask() { return attach(newGenericTask()); }
  static  GenericTask newGenericTask() { return new GenericTask(); }

  default CompositeTask newCompositeSubtask(String name) { return attach(newCompositeTask(name)); }
  static  CompositeTask newCompositeTask(String name) { return new CompositeTask(name); }

  default CompositeTask newCompositeSubtask() { return attach(newCompositeTask()); }
  static  CompositeTask newCompositeTask() { return new CompositeTask(); }
}
