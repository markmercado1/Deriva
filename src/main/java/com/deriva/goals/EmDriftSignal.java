package com.deriva.goals;

import java.time.Instant;

/**
 * Read-only view of the capture's drift state. {@code generation} increases every
 * time a new episode opens, so an observer can tell a new episode from the one that
 * was already active. {@code changedAt} is the last open/close moment, if any.
 */
public record EmDriftSignal(boolean active, long generation, Instant changedAt) {
    public EmDriftSignal {
        if (generation < 0) throw new IllegalArgumentException("generation");
    }

    public static EmDriftSignal inactive() {
        return new EmDriftSignal(false, 0, null);
    }

    public static EmDriftSignal activeOf(long generation, Instant changedAt) {
        return new EmDriftSignal(true, generation, changedAt);
    }
}
