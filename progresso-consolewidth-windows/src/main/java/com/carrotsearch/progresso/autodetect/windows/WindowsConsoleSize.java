package com.carrotsearch.progresso.autodetect.windows;

import static java.lang.foreign.ValueLayout.ADDRESS;
import static java.lang.foreign.ValueLayout.JAVA_INT;
import static java.lang.foreign.ValueLayout.JAVA_SHORT;

import java.io.IOException;
import java.lang.foreign.Arena;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.Linker;
import java.lang.foreign.MemoryLayout.PathElement;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.StructLayout;
import java.lang.foreign.SymbolLookup;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.VarHandle;

/**
 * Acquires the size of the Windows console attached to this process using direct calls to {@code
 * kernel32} (foreign function interface).
 *
 * <p>The calls are <em>restricted methods</em>: the JVM must be started with {@code
 * --enable-native-access=ALL-UNNAMED} (or with this module's name when it is on the module path),
 * otherwise the JDK prints a warning on first use and may refuse the call in future releases.
 */
public final class WindowsConsoleSize {
  /** Console screen buffer size and the visible window's coordinates within that buffer. */
  public record Info(int bufferWidth, int bufferHeight, int left, int top, int right, int bottom) {
    /** Width of the visible window, in columns. */
    public int windowWidth() {
      return right - left + 1;
    }

    /** Height of the visible window, in rows. */
    public int windowHeight() {
      return bottom - top + 1;
    }
  }

  private static final int GENERIC_READ = 0x80000000;
  private static final int GENERIC_WRITE = 0x40000000;
  private static final int FILE_SHARE_READ = 0x00000001;
  private static final int FILE_SHARE_WRITE = 0x00000002;
  private static final int OPEN_EXISTING = 3;
  private static final long INVALID_HANDLE_VALUE = -1L;
  private static final int ERROR_INVALID_HANDLE = 6;

  // CONSOLE_SCREEN_BUFFER_INFO is 11 consecutive 16-bit fields:
  // COORD dwSize; COORD dwCursorPosition; WORD wAttributes;
  // SMALL_RECT srWindow; COORD dwMaximumWindowSize;
  private static final long CSBI_BYTES = 11 * Short.BYTES;
  private static final long CSBI_SIZE_X = 0;
  private static final long CSBI_SIZE_Y = 2;
  private static final long CSBI_WINDOW_LEFT = 10;
  private static final long CSBI_WINDOW_TOP = 12;
  private static final long CSBI_WINDOW_RIGHT = 14;
  private static final long CSBI_WINDOW_BOTTOM = 16;

  private WindowsConsoleSize() {}

  /**
   * @return Returns console information or {@code null} if this process has no console attached.
   * @throws IOException If the console is there but its size could not be acquired.
   * @throws Throwable On any other failure (native access denied, linkage problems, not Windows).
   */
  public static Info detect() throws Throwable {
    Linker linker = Linker.nativeLinker();
    StructLayout captureLayout = Linker.Option.captureStateLayout();
    VarHandle lastError = captureLayout.varHandle(PathElement.groupElement("GetLastError"));
    Linker.Option captureLastError = Linker.Option.captureCallState("GetLastError");

    try (Arena arena = Arena.ofConfined()) {
      SymbolLookup kernel32 = SymbolLookup.libraryLookup("kernel32", arena);

      MethodHandle createFile =
          linker.downcallHandle(
              kernel32.findOrThrow("CreateFileA"),
              FunctionDescriptor.of(
                  ADDRESS, ADDRESS, JAVA_INT, JAVA_INT, ADDRESS, JAVA_INT, JAVA_INT, ADDRESS),
              captureLastError);
      MethodHandle getConsoleScreenBufferInfo =
          linker.downcallHandle(
              kernel32.findOrThrow("GetConsoleScreenBufferInfo"),
              FunctionDescriptor.of(JAVA_INT, ADDRESS, ADDRESS),
              captureLastError);
      MethodHandle closeHandle =
          linker.downcallHandle(
              kernel32.findOrThrow("CloseHandle"), FunctionDescriptor.of(JAVA_INT, ADDRESS));

      MemorySegment capturedState = arena.allocate(captureLayout);

      // Open the console's output buffer directly, standard streams may be redirected.
      MemorySegment handle =
          (MemorySegment)
              createFile.invokeExact(
                  capturedState,
                  arena.allocateFrom("CONOUT$"),
                  GENERIC_READ | GENERIC_WRITE,
                  FILE_SHARE_READ | FILE_SHARE_WRITE,
                  MemorySegment.NULL,
                  OPEN_EXISTING,
                  0,
                  MemorySegment.NULL);

      if (handle.address() == INVALID_HANDLE_VALUE) {
        int err = (int) lastError.get(capturedState, 0L);
        if (err == ERROR_INVALID_HANDLE) {
          // No console attached.
          return null;
        }
        throw new IOException("CreateFile(CONOUT$) failed, error code: " + err);
      } else if (handle.address() == 0) {
        return null;
      }

      try {
        MemorySegment csbi = arena.allocate(CSBI_BYTES, Short.BYTES);
        int success = (int) getConsoleScreenBufferInfo.invokeExact(capturedState, handle, csbi);
        if (success == 0) {
          throw new IOException(
              "GetConsoleScreenBufferInfo failed, error code: "
                  + (int) lastError.get(capturedState, 0L));
        }

        return new Info(
            csbi.get(JAVA_SHORT, CSBI_SIZE_X),
            csbi.get(JAVA_SHORT, CSBI_SIZE_Y),
            csbi.get(JAVA_SHORT, CSBI_WINDOW_LEFT),
            csbi.get(JAVA_SHORT, CSBI_WINDOW_TOP),
            csbi.get(JAVA_SHORT, CSBI_WINDOW_RIGHT),
            csbi.get(JAVA_SHORT, CSBI_WINDOW_BOTTOM));
      } finally {
        int _ = (int) closeHandle.invokeExact(handle);
      }
    }
  }
}
