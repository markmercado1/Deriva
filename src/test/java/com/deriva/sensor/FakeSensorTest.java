package com.deriva.sensor;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class FakeSensorTest {
    private static ActivitySample sample(String process, int second) {
        return new ActivitySample(Instant.parse("2026-11-22T10:00:0" + second + "Z"),
                process, 100 + second, 200 + second, false, true);
    }

    @Test
    void returnsSamplesInDeterministicOrderAndFailsWhenExhausted() {
        FakeSensor sensor = new FakeSensor(sample("a.exe", 0), sample("b.exe", 1), sample("a.exe", 2));

        assertEquals("a.exe", sensor.sample().processName());
        assertEquals("b.exe", sensor.sample().processName());
        assertEquals("a.exe", sensor.sample().processName());
        assertEquals(0, sensor.remaining());
        assertThrows(IllegalStateException.class, sensor::sample);
    }

    @Test
    void idleDetectorReturnsQueuedDurationsThenZero() {
        FakeIdleDetector idle = new FakeIdleDetector(Duration.ofSeconds(5), Duration.ofSeconds(10));

        assertEquals(Duration.ofSeconds(5), idle.idleTime());
        assertEquals(Duration.ofSeconds(10), idle.idleTime());
        assertEquals(Duration.ZERO, idle.idleTime());
        assertEquals(0, idle.remaining());
    }

    @Test
    void rejectsNullAndNegativeFixtures() {
        assertThrows(NullPointerException.class, () -> new FakeSensor((ActivitySample) null));
        assertThrows(NullPointerException.class, () -> new FakeIdleDetector((Duration) null));
        assertThrows(IllegalArgumentException.class, () -> new FakeIdleDetector(Duration.ofMillis(-1)));
    }
}
