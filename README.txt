
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

The core jar's entry point draws a progress bar in every available style:

  java -jar progresso/build/libs/progresso-*.jar

./gradlew :progresso:demo assembles progresso/build/demo with the console width modules
and prints the command line to run the demo from the module path.
