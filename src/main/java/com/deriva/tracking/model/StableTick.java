package com.deriva.tracking.model;

import com.deriva.privacy.ObservationKind;

import java.time.Instant;

/**
 * Context in force after debounce for one tick.
 *
 * <p>{@code pendingChange} is true while an observed candidate is waiting for its
 * three observed seconds. The tick still describes the confirmed current context;
 * when there is no confirmed context yet, it describes the provisional candidate.</p>
 */
public record StableTick(Instant timestamp, ObservationKind kind, ContextKey context,
                         Instant contextSince, boolean pendingChange) {
    public StableTick {
        if (timestamp == null || kind == null || contextSince == null)
            throw new IllegalArgumentException("timestamp/kind/contextSince");
        if (contextSince.isAfter(timestamp)) throw new IllegalArgumentException("contextSince after timestamp");
        if (kind == ObservationKind.OBSERVED) {
            if (context == null) throw new IllegalArgumentException("Missing context");
        } else if (context != null) {
            throw new IllegalArgumentException("Non-observed tick cannot carry context");
        }
    }
}
