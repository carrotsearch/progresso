package com.carrotsearch.progresso;

import java.util.Arrays;
import java.util.List;

import com.carrotsearch.progresso.util.UnitFormatter;
import com.carrotsearch.progresso.util.Units;

public interface Tasks {
  void attach(Iterable<? extends Task<?>> tasks);

  default <T extends Task<?>> T attach(T task) {
    attach(Arrays.asList(task));
    return task;
  }

  List<Task<?>> subtasks();

  default RangeTask newByteRangeSubtask(String name) { return attach(newByteRangeTask(name)); }
  static  RangeTask newByteRangeTask(String name) { return new RangeTask(name, Units.BYTES); }
  static  RangeTask newByteRangeTask() { return new RangeTask(Units.BYTES); }

  default LongTask newByteSubtask(String name) { return attach(newByteTask(name)); }
  static  LongTask newByteTask(String name) { return newLongTask(name, Units.BYTES); }
  static  LongTask newByteTask() { return newLongTask(Units.BYTES); }

  default RangeTask newRangeSubtask(String name) { return attach(newRangeTask(name)); }
  static  RangeTask newRangeTask(String name) { return new RangeTask(name); }
  static  RangeTask newRangeTask() { return new RangeTask(); }

  default RangeTask newRangeSubtask(String name, UnitFormatter unit) { return attach(newRangeTask(name, unit)); }
  static  RangeTask newRangeTask(String name, UnitFormatter unit) { return new RangeTask(name, unit); }
  static  RangeTask newRangeTask(UnitFormatter unit) { return new RangeTask(unit); }

  default LongTask newLongSubtask(String name) { return attach(newLongTask(name)); }
  static  LongTask newLongTask(String name) { return new LongTask(name); }
  static  LongTask newLongTask() { return new LongTask(); }

  default LongTask newLongSubtask(String name, UnitFormatter unit) { return attach(newLongTask(name, unit)); }
  static  LongTask newLongTask(String name, UnitFormatter unit) { return new LongTask(name, unit); }
  static  LongTask newLongTask(UnitFormatter unit) { return new LongTask(unit); }

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
