package com.deriva.sensor.windows;

import com.deriva.sensor.EmActivitySample;

/**
 * Manual probe for the Windows adapters. Not a JUnit test: it depends on the real
 * foreground window and the current input idle time.
 */
public final class EmWindowsSensorProbe {
    private EmWindowsSensorProbe() {}

    public static void main(String[] args) {
        EmActivitySample sample = new EmWindowsWindowSensor().sample();
        System.out.println("available=" + sample.available());
        System.out.println("process=" + (sample.processName() == null ? "null" : sample.processName()));
        System.out.println("pidPositive=" + (sample.pid() > 0));
        System.out.println("windowNonZero=" + (sample.foregroundWindowId() != 0));
        System.out.println("idleMillis=" + new EmWindowsIdleDetector().idleTime().toMillis());
    }
}
