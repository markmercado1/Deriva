package com.deriva.sensor.windows;

import com.deriva.sensor.IdleDetector;
import com.sun.jna.platform.win32.Kernel32;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinUser;

import java.time.Duration;

/** Windows adapter backed by {@code GetLastInputInfo}. */
public final class WindowsIdleDetector implements IdleDetector {
    @Override
    public Duration idleTime() {
        WinUser.LASTINPUTINFO info = new WinUser.LASTINPUTINFO();
        info.cbSize = info.size();
        if (!User32.INSTANCE.GetLastInputInfo(info))
            throw new IllegalStateException("GetLastInputInfo failed");

        // Both values are 32-bit tick counts; unsigned subtraction handles wrap-around.
        int elapsed = Kernel32.INSTANCE.GetTickCount() - info.dwTime;
        return Duration.ofMillis(Integer.toUnsignedLong(elapsed));
    }
}
