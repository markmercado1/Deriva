package com.deriva.tracking.model;

import com.deriva.privacy.EmObservationKind;

import java.time.Instant;

/**
 * One continuous segment in the local record. Intervals are semi-open {@code [start, end)}.
 *
 * <p>Only observed segments carry a context. Idle, excluded and unavailable segments
 * keep every identity field null; the SQL adapter maps them to the plan sentinels.</p>
 */
public record EmSegment(long id, EmObservationKind kind, EmContextKey context,
                      String siteLabel, String siteSource, Instant start, Instant end) {
    public EmSegment {
        if (id < 0) throw new IllegalArgumentException("id");
        if (kind == null || start == null || end == null) throw new IllegalArgumentException("kind/timestamps");
        if (end.isBefore(start)) throw new IllegalArgumentException("end before start");

        if (kind == EmObservationKind.OBSERVED) {
            if (context == null) throw new IllegalArgumentException("Missing context");
            if (siteSource != null && !siteSource.equals("EXTENSION"))
                throw new IllegalArgumentException("siteSource");
            if (context.siteHost() == null && (siteLabel != null || siteSource != null))
                throw new IllegalArgumentException("Site fields without host");
        } else if (context != null || siteLabel != null || siteSource != null) {
            throw new IllegalArgumentException("Non-observed segment cannot carry identity");
        }
    }
}
