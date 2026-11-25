package com.deriva.app.usecase;

import com.deriva.analysis.DayWindow;
import com.deriva.privacy.ObservationKind;
import com.deriva.storage.port.SegmentRepository;
import com.deriva.tracking.model.Segment;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Computes persisted daily foreground totals from closed segments. */
public final class DailyUsageService {
    private final SegmentRepository repository;
    private final Clock clock;

    public DailyUsageService(SegmentRepository repository, Clock clock) {
        this.repository = Objects.requireNonNull(repository, "repository");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    public DailyUsage forDay(LocalDate day, ZoneId zone) {
        return forDay(day, zone, CaptureSnapshot.stopped());
    }

    /**
     * Adds the confirmed open segment to the persisted totals. A pending candidate
     * is deliberately not counted until it is confirmed, so it cannot be duplicated.
     */
    public DailyUsage forDay(LocalDate day, ZoneId zone, CaptureSnapshot snapshot) {
        return forRange(day, day, zone, snapshot);
    }

    public DailyUsage forRange(LocalDate day, LocalDate through, ZoneId zone, CaptureSnapshot snapshot) {
        Objects.requireNonNull(day, "day");
        Objects.requireNonNull(through, "through");
        if (through.isBefore(day)) throw new IllegalArgumentException("Desde debe ser anterior o igual a Hasta.");
        Objects.requireNonNull(zone, "zone");
        CaptureSnapshot live = snapshot == null ? CaptureSnapshot.stopped() : snapshot;
        LocalDate today = clock.instant().atZone(zone).toLocalDate();
        if (today.isBefore(day) || today.isAfter(through)) live = CaptureSnapshot.stopped();
        DayWindow window = new DayWindow(day.atStartOfDay(zone).toInstant(),
                through.plusDays(1).atStartOfDay(zone).toInstant());
        Instant dayStart = window.start();
        Instant dayEnd = window.end();
        List<Segment> persisted = repository.between(dayStart, dayEnd);

        Totals totals = new Totals();
        boolean hasSegments = false;
        for (Segment segment : persisted)
            hasSegments |= totals.add(segment, window) > 0;

        Segment open = live.open();
        if (open != null && !overlapsAny(open, persisted))
            hasSegments |= totals.add(open, window) > 0;
        if (live.captureRunning() && live.pending() != null) hasSegments = true;

        Instant now = clock.instant();
        Instant elapsedEnd = dayEnd.isBefore(now) ? dayEnd : now;
        long elapsed = Math.max(0L, Duration.between(dayStart, elapsedEnd).toMillis());
        long recorded = totals.observed + totals.idle + totals.excluded + totals.unavailable;
        return new DailyUsage(day, totals.observed, totals.idle, totals.excluded,
                totals.unavailable, recorded, elapsed, totals.apps, totals.sites,
                hasSegments, live);
    }

    private static boolean overlapsAny(Segment segment, List<Segment> persisted) {
        return persisted.stream().anyMatch(other ->
                segment.start().isBefore(other.end()) && other.start().isBefore(segment.end()));
    }

    private static final class Totals {
        private long observed;
        private long idle;
        private long excluded;
        private long unavailable;
        private final Map<String, Long> apps = new HashMap<>();
        private final Map<String, Long> sites = new HashMap<>();

        private long add(Segment segment, DayWindow window) {
            long millis = window.clippedMillis(segment);
            if (millis <= 0) return 0;

            switch (segment.kind()) {
                case OBSERVED -> {
                    observed += millis;
                    String process = segment.context().processName();
                    apps.merge(process, millis, Long::sum);
                    if (segment.context().siteHost() != null)
                        sites.merge(segment.context().siteHost(), millis, Long::sum);
                }
                case IDLE -> idle += millis;
                case EXCLUDED -> excluded += millis;
                case UNAVAILABLE -> unavailable += millis;
            }
            return millis;
        }
    }
}
