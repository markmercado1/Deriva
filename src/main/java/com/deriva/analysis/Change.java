package com.deriva.analysis;

import com.deriva.tracking.model.ContextKey;

import java.time.Instant;

/** One confirmed transition between consecutive observed contexts. */
public record Change(Instant timestamp, ContextKey context) {
    public Change {
        if (timestamp == null || context == null) throw new IllegalArgumentException("timestamp/context");
    }
}
