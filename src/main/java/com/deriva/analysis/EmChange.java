package com.deriva.analysis;

import com.deriva.tracking.model.EmContextKey;

import java.time.Instant;

/** One confirmed transition between consecutive observed contexts. */
public record EmChange(Instant timestamp, EmContextKey context) {
    public EmChange {
        if (timestamp == null || context == null) throw new IllegalArgumentException("timestamp/context");
    }
}
