package com.carrotsearch.progresso.autodetect;

import com.carrotsearch.progresso.views.console.ConsoleAware;
import com.carrotsearch.randomizedtesting.RandomizedTest;
import org.junit.Test;

public class CheckProviderLoaded extends RandomizedTest {
  @Test
  public void testProviderLoaded() {
    System.out.println(ConsoleAware.consoleWidth());
  }
}
