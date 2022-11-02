package com.carrotsearch.progresso;

public class GenericTask extends Task<Tracker> {
  public GenericTask() {
    super();
  }

  public GenericTask(String name) {
    super(name);
  }

  public Tracker start() {
    return start(
        new Tracker(this) {
          @Override
          public long modHash() {
            return 0L;
          }
        });
  }
}
