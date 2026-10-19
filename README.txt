
PROGRESSO
=========

A set of utilities for reporting tasks, their attributes
and progress information to the console.

See LICENSE.txt to make your company's lawyer happy.

The code itself is the documentation. The API is subject
to change without notice.

Building requires JDK 25:

  ./gradlew check          # compile, format checks, tests
  ./gradlew tidy           # apply source formatting
  ./gradlew buildOptions   # list build options (tests.seed, tests.verbose, ...)
  ./gradlew :progresso:demos -Ptests.verbose=true   # run the console view demos
