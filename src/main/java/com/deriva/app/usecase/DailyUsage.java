package com.deriva.app.usecase;

import java.time.LocalDate;
import java.util.Map;

/**
 * Persisted foreground totals for one local date. Sites are a subset of the
 * browser application's time; this object keeps them separate so callers never
 * add both. Non-observed segments are recorded only for coverage.
 */
public record DailyUsage(
        LocalDate date,
        long observedMillis,
        long idleMillis,
        long excludedMillis,
        long unavailableMillis,
        long recordedMillis,
        long elapsedMillis,
        Map<String, Long> appMillis,
        Map<String, Long> siteMillis,
        boolean hasSegments,
        CaptureSnapshot snapshot) {

    public DailyUsage {
        if (date == null || observedMillis < 0 || idleMillis < 0 || excludedMillis < 0
                || unavailableMillis < 0 || recordedMillis < 0 || elapsedMillis < 0)
            throw new IllegalArgumentException("Invalid daily usage");
        appMillis = Map.copyOf(appMillis);
        siteMillis = Map.copyOf(siteMillis);
        snapshot = snapshot == null ? CaptureSnapshot.stopped() : snapshot;
    }

    public long unrecordedMillis() {
        return Math.max(0L, elapsedMillis - recordedMillis);
    }
}
