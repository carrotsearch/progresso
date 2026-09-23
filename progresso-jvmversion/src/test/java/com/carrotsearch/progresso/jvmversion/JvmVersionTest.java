package com.carrotsearch.progresso.jvmversion;

import com.carrotsearch.randomizedtesting.RandomizedTest;
import java.io.DataInputStream;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.assertj.core.api.Assertions;
import org.junit.Test;

/** Sanity checks of the checked-in class file: it must be a Java 6 class and must actually run. */
public class JvmVersionTest extends RandomizedTest {
  private static final String CLASS_RESOURCE =
      "/com/carrotsearch/progresso/jvmversion/JvmVersion.class";

  @Test
  public void classFileIsJava6() throws Exception {
    try (InputStream is = JvmVersionTest.class.getResourceAsStream(CLASS_RESOURCE)) {
      Assertions.assertThat(is).as("Class file resource").isNotNull();
      DataInputStream dis = new DataInputStream(is);
      Assertions.assertThat(dis.readInt()).isEqualTo(0xCAFEBABE);
      dis.readUnsignedShort(); // minor
      Assertions.assertThat(dis.readUnsignedShort()).as("class file major version").isEqualTo(50);
    }
  }

  @Test
  public void runsAndPrintsSpecificationVersion() throws Exception {
    Path javaBin = Paths.get(System.getProperty("java.home"), "bin", "java");
    // Locate the directory holding the class file (target/classes).
    URL url = JvmVersionTest.class.getResource(CLASS_RESOURCE);
    Assertions.assertThat(url).isNotNull();
    Assertions.assertThat(url.getProtocol()).isEqualTo("file");
    Path classFile = Paths.get(url.toURI());
    Path classes = classFile;
    for (int i = 0; i < CLASS_RESOURCE.split("/").length - 1; i++) {
      classes = classes.getParent();
    }
    Assertions.assertThat(classes.resolve(CLASS_RESOURCE.substring(1))).isEqualTo(classFile);

    Process p =
        new ProcessBuilder(
                javaBin.toString(), "-cp", classes.toString(), JvmVersion.class.getName())
            .redirectErrorStream(true)
            .start();
    String output = new String(p.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
    Assertions.assertThat(p.waitFor()).as("exit code, output: " + output).isEqualTo(0);

    String expected = System.getProperty("java.specification.version");
    if (expected.startsWith("1.")) {
      expected = expected.substring(2);
    }
    Assertions.assertThat(output).isEqualTo(expected);
  }
}
