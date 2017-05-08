package com.carrotsearch.progresso.autodetect;

import org.junit.Test;

import com.carrotsearch.progresso.views.console.ConsoleAware;
import com.carrotsearch.randomizedtesting.RandomizedTest;

public class CheckProviderLoaded extends RandomizedTest {
  @Test
  public void testProviderLoaded() {
    System.out.println(ConsoleAware.consoleWidth());
  }
}
