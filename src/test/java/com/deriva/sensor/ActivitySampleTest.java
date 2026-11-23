package com.deriva.sensor;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class ActivitySampleTest {
    private static final Instant T = Instant.parse("2026-11-22T10:00:00Z");

    @Test
    void availableSampleRequiresIdentityAndWindow() {
        assertThrows(IllegalArgumentException.class, () -> new ActivitySample(T, null, 10, 20, false, true));
        assertThrows(IllegalArgumentException.class, () -> new ActivitySample(T, "  ", 10, 20, false, true));
        assertThrows(IllegalArgumentException.class, () -> new ActivitySample(T, "chrome.exe", 0, 20, false, true));
        assertThrows(IllegalArgumentException.class, () -> new ActivitySample(T, "chrome.exe", 10, 0, false, true));
    }

    @Test
    void unavailableSampleCannotCarryIdentityOrIdleState() {
        assertThrows(IllegalArgumentException.class, () -> new ActivitySample(T, "chrome.exe", 0, 0, false, false));
        assertThrows(IllegalArgumentException.class, () -> new ActivitySample(T, null, 10, 0, false, false));
        assertThrows(IllegalArgumentException.class, () -> new ActivitySample(T, null, 0, 20, false, false));
        assertThrows(IllegalArgumentException.class, () -> new ActivitySample(T, null, 0, 0, true, false));
        assertDoesNotThrow(() -> new ActivitySample(T, null, 0, 0, false, false));
    }

    @Test
    void trimsProcessNameAndAllowsIdleObservation() {
        ActivitySample sample = new ActivitySample(T, " chrome.exe ", 42, 99, true, true);
        assertEquals("chrome.exe", sample.processName());
        assertTrue(sample.idle());
        assertTrue(sample.available());
    }

    @Test
    void rejectsPathsAndControlCharactersInProcessName() {
        assertThrows(IllegalArgumentException.class,
                () -> new ActivitySample(T, "C:\\Chrome\\chrome.exe", 12, 34, false, true));
        assertThrows(IllegalArgumentException.class,
                () -> new ActivitySample(T, "chrome\nexe", 12, 34, false, true));
    }

    @Test
    void rejectsNullTimestamp() {
        assertThrows(IllegalArgumentException.class, () -> new ActivitySample(null, null, 0, 0, false, false));
    }
}
