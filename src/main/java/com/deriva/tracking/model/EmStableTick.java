package com.deriva.tracking.model;

import com.deriva.privacy.EmObservationKind;

import java.time.Instant;

/**
 * Context in force after debounce for one tick.
 *
 * <p>{@code pendingChange} is true while an observed candidate is waiting for its
 * three observed seconds. The tick still describes the confirmed current context;
 * when there is no confirmed context yet, it describes the provisional candidate.</p>
 */
public record EmStableTick(Instant timestamp, EmObservationKind kind, EmContextKey context,
                         Instant contextSince, boolean pendingChange) {
    public EmStableTick {
        if (timestamp == null || kind == null || contextSince == null)
            throw new IllegalArgumentException("timestamp/kind/contextSince");
        if (contextSince.isAfter(timestamp)) throw new IllegalArgumentException("contextSince after timestamp");
        if (kind == EmObservationKind.OBSERVED) {
            if (context == null) throw new IllegalArgumentException("Missing context");
        } else if (context != null) {
            throw new IllegalArgumentException("Non-observed tick cannot carry context");
        }
    }
}
