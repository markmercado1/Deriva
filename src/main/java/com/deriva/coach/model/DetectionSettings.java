package com.deriva.coach.model;

import java.time.Duration;
import java.util.Objects;

/**
 * Product limits for the configurable detection thresholds. These are guardrails,
 * not scientifically validated values.
 */
public record DetectionSettings(
        int switches,
        Duration window,
        Duration stability,
        Duration idle) {

    public static final int MIN_SWITCHES = 4;
    public static final int MAX_SWITCHES = 30;
    public static final int MIN_WINDOW_SECONDS = 30;
    public static final int MAX_WINDOW_SECONDS = 600;
    public static final int MIN_STABILITY_SECONDS = 30;
    public static final int MAX_STABILITY_SECONDS = 900;
    public static final int MIN_IDLE_SECONDS = 30;
    public static final int MAX_IDLE_SECONDS = 1800;

    public static final int DEFAULT_SWITCHES = 10;
    public static final int DEFAULT_WINDOW_SECONDS = 120;
    public static final int DEFAULT_STABILITY_SECONDS = 180;
    public static final int DEFAULT_IDLE_SECONDS = 120;

    public DetectionSettings {
        if (switches < MIN_SWITCHES || switches > MAX_SWITCHES)
            throw new IllegalArgumentException("switches");
        requireSeconds(window, MIN_WINDOW_SECONDS, MAX_WINDOW_SECONDS, "window");
        requireSeconds(stability, MIN_STABILITY_SECONDS, MAX_STABILITY_SECONDS, "stability");
        requireSeconds(idle, MIN_IDLE_SECONDS, MAX_IDLE_SECONDS, "idle");
    }

    public static DetectionSettings defaults() {
        return new DetectionSettings(DEFAULT_SWITCHES,
                Duration.ofSeconds(DEFAULT_WINDOW_SECONDS),
                Duration.ofSeconds(DEFAULT_STABILITY_SECONDS),
                Duration.ofSeconds(DEFAULT_IDLE_SECONDS));
    }

    /** True when inactivity is evaluated at or before stability is confirmed. */
    public boolean idleAtOrBelowStability() {
        return idle.compareTo(stability) <= 0;
    }

    private static void requireSeconds(Duration value, int min, int max, String name) {
        Objects.requireNonNull(value, name);
        if (value.getNano() != 0 || value.getSeconds() < min || value.getSeconds() > max)
            throw new IllegalArgumentException(name);
    }
}
