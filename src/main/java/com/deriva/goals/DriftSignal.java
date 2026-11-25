package com.deriva.goals;

import java.time.Instant;

/**
 * Read-only view of the capture's drift state. {@code generation} increases every
 * time a new episode opens, so an observer can tell a new episode from the one that
 * was already active. {@code changedAt} is the last open/close moment, if any.
 */
public record DriftSignal(boolean active, long generation, Instant changedAt) {
    public DriftSignal {
        if (generation < 0) throw new IllegalArgumentException("generation");
    }

    public static DriftSignal inactive() {
        return new DriftSignal(false, 0, null);
    }

    public static DriftSignal activeOf(long generation, Instant changedAt) {
        return new DriftSignal(true, generation, changedAt);
    }
}
