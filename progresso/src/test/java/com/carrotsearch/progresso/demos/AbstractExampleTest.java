package com.carrotsearch.progresso.demos;

import com.carrotsearch.progresso.Progress;
import com.carrotsearch.progresso.views.console.ConsoleAware;
import com.carrotsearch.randomizedtesting.RandomizedContext;
import com.carrotsearch.randomizedtesting.RandomizedTest;
import com.carrotsearch.randomizedtesting.annotations.ThreadLeakAction;
import com.carrotsearch.randomizedtesting.annotations.ThreadLeakGroup;
import com.carrotsearch.randomizedtesting.annotations.ThreadLeakGroup.Group;
import com.carrotsearch.randomizedtesting.annotations.ThreadLeakScope;
import com.carrotsearch.randomizedtesting.annotations.ThreadLeakScope.Scope;
import org.junit.After;
import org.junit.Before;

@ThreadLeakScope(Scope.TEST)
@ThreadLeakAction({ThreadLeakAction.Action.WARN})
@ThreadLeakGroup(Group.MAIN)
@Example
public abstract class AbstractExampleTest extends RandomizedTest {
  static final int DEFAULT_WIDTH = 70;

  @Before
  public void emitTestName() {
    System.out.println("-- " + RandomizedContext.current().getTargetMethod().getName());
  }

  @After
  public void emitNewline() {
    System.out.println();
  }

  protected Progress defaultProgress() {
    return new Progress(ConsoleAware.newConsoleProgressView());
  }
}
