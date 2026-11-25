package com.deriva.sensor.windows;

import com.deriva.sensor.EmActivitySample;
import com.deriva.sensor.EmWindowSensor;
import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.Kernel32;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinBase;
import com.sun.jna.platform.win32.WinDef.HWND;
import com.sun.jna.platform.win32.WinNT;
import com.sun.jna.platform.win32.WinNT.HANDLE;
import com.sun.jna.ptr.IntByReference;

import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.util.Objects;

/**
 * Windows adapter for the foreground window.
 *
 * <p>Uses one reusable path buffer, so the instance is owned by the single sampling
 * thread described in the architecture. Any native failure becomes an unavailable
 * sample instead of guessing a process identity.</p>
 */
public final class EmWindowsWindowSensor implements EmWindowSensor {
    private static final int MAX_PATH_CHARS = 32_768;

    private final Clock clock;
    private final char[] pathBuffer = new char[MAX_PATH_CHARS];
    private final IntByReference pathSize = new IntByReference();

    public EmWindowsWindowSensor() {
        this(Clock.systemUTC());
    }

    public EmWindowsWindowSensor(Clock clock) {
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    @Override
    public EmActivitySample sample() {
        Instant timestamp = clock.instant();
        HWND hwnd = User32.INSTANCE.GetForegroundWindow();
        if (hwnd == null || hwnd.getPointer() == null) return unavailable(timestamp);

        long windowId = Pointer.nativeValue(hwnd.getPointer());
        if (windowId == 0) return unavailable(timestamp);

        IntByReference pid = new IntByReference();
        if (User32.INSTANCE.GetWindowThreadProcessId(hwnd, pid) == 0 || pid.getValue() <= 0)
            return unavailable(timestamp);

        String processName = processName(pid.getValue());
        if (processName == null) return unavailable(timestamp);

        return new EmActivitySample(timestamp, processName, pid.getValue(), windowId, false, true);
    }

    private static EmActivitySample unavailable(Instant timestamp) {
        return new EmActivitySample(timestamp, null, 0, 0, false, false);
    }

    private String processName(int pid) {
        HANDLE process = Kernel32.INSTANCE.OpenProcess(WinNT.PROCESS_QUERY_LIMITED_INFORMATION, false, pid);
        if (process == null || WinBase.INVALID_HANDLE_VALUE.equals(process)) return null;
        try {
            pathSize.setValue(pathBuffer.length);
            if (!Kernel32.INSTANCE.QueryFullProcessImageName(process, 0, pathBuffer, pathSize)) return null;
            String fullPath = new String(pathBuffer, 0, pathSize.getValue());
            String name = Path.of(fullPath).getFileName().toString();
            return name.isBlank() ? null : name;
        } catch (RuntimeException ex) {
            return null;
        } finally {
            Kernel32.INSTANCE.CloseHandle(process);
        }
    }
}
