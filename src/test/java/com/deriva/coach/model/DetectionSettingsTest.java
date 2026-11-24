package com.deriva.coach.model;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

class DetectionSettingsTest {
    @Test
    void defaultsMatchDocumentedValues() {
        DetectionSettings defaults = DetectionSettings.defaults();
        assertEquals(10, defaults.switches());
        assertEquals(Duration.ofSeconds(120), defaults.window());
        assertEquals(Duration.ofSeconds(180), defaults.stability());
        assertEquals(Duration.ofSeconds(120), defaults.idle());
    }

    @Test
    void acceptsProductLimits() {
        assertDoesNotThrow(() -> new DetectionSettings(4,
                Duration.ofSeconds(30), Duration.ofSeconds(900), Duration.ofSeconds(30)));
        assertDoesNotThrow(() -> new DetectionSettings(30,
                Duration.ofSeconds(600), Duration.ofSeconds(30), Duration.ofSeconds(1800)));
    }

    @Test
    void rejectsOutOfRangeValues() {
        assertThrows(IllegalArgumentException.class, () -> new DetectionSettings(3,
                Duration.ofSeconds(120), Duration.ofSeconds(180), Duration.ofSeconds(120)));
        assertThrows(IllegalArgumentException.class, () -> new DetectionSettings(31,
                Duration.ofSeconds(120), Duration.ofSeconds(180), Duration.ofSeconds(120)));
        assertThrows(IllegalArgumentException.class, () -> new DetectionSettings(10,
                Duration.ofSeconds(29), Duration.ofSeconds(180), Duration.ofSeconds(120)));
        assertThrows(IllegalArgumentException.class, () -> new DetectionSettings(10,
                Duration.ofSeconds(601), Duration.ofSeconds(180), Duration.ofSeconds(120)));
        assertThrows(IllegalArgumentException.class, () -> new DetectionSettings(10,
                Duration.ofSeconds(120), Duration.ofSeconds(29), Duration.ofSeconds(120)));
        assertThrows(IllegalArgumentException.class, () -> new DetectionSettings(10,
                Duration.ofSeconds(120), Duration.ofSeconds(901), Duration.ofSeconds(120)));
        assertThrows(IllegalArgumentException.class, () -> new DetectionSettings(10,
                Duration.ofSeconds(120), Duration.ofSeconds(180), Duration.ofSeconds(29)));
        assertThrows(IllegalArgumentException.class, () -> new DetectionSettings(10,
                Duration.ofSeconds(120), Duration.ofSeconds(180), Duration.ofSeconds(1801)));
        assertThrows(IllegalArgumentException.class, () -> new DetectionSettings(10,
                Duration.ofSeconds(120), Duration.ofSeconds(180), Duration.ofMillis(120_500)));
    }

    @Test
    void idleWarningUsesLessThanOrEqual() {
        assertTrue(new DetectionSettings(10, Duration.ofSeconds(120),
                Duration.ofSeconds(180), Duration.ofSeconds(120)).idleAtOrBelowStability());
        assertTrue(new DetectionSettings(10, Duration.ofSeconds(120),
                Duration.ofSeconds(180), Duration.ofSeconds(180)).idleAtOrBelowStability());
        assertFalse(new DetectionSettings(10, Duration.ofSeconds(120),
                Duration.ofSeconds(180), Duration.ofSeconds(181)).idleAtOrBelowStability());
    }
}
