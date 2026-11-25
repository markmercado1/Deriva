package com.deriva.sensor;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class EmFakeSensorTest {
    private static EmActivitySample sample(String process, int second) {
        return new EmActivitySample(Instant.parse("2026-11-22T10:00:0" + second + "Z"),
                process, 100 + second, 200 + second, false, true);
    }

    @Test
    void returnsSamplesInDeterministicOrderAndFailsWhenExhausted() {
        EmFakeSensor sensor = new EmFakeSensor(sample("a.exe", 0), sample("b.exe", 1), sample("a.exe", 2));

        assertEquals("a.exe", sensor.sample().processName());
        assertEquals("b.exe", sensor.sample().processName());
        assertEquals("a.exe", sensor.sample().processName());
        assertEquals(0, sensor.remaining());
        assertThrows(IllegalStateException.class, sensor::sample);
    }

    @Test
    void idleDetectorReturnsQueuedDurationsThenZero() {
        EmFakeIdleDetector idle = new EmFakeIdleDetector(Duration.ofSeconds(5), Duration.ofSeconds(10));

        assertEquals(Duration.ofSeconds(5), idle.idleTime());
        assertEquals(Duration.ofSeconds(10), idle.idleTime());
        assertEquals(Duration.ZERO, idle.idleTime());
        assertEquals(0, idle.remaining());
    }

    @Test
    void rejectsNullAndNegativeFixtures() {
        assertThrows(NullPointerException.class, () -> new EmFakeSensor((EmActivitySample) null));
        assertThrows(NullPointerException.class, () -> new EmFakeIdleDetector((Duration) null));
        assertThrows(IllegalArgumentException.class, () -> new EmFakeIdleDetector(Duration.ofMillis(-1)));
    }
}
