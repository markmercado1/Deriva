package com.deriva.analysis;

import com.deriva.tracking.model.EmSegment;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Objects;

/**
 * Half-open local day {@code [start, end)} and the clipping rules shared by daily
 * totals and goal progress. Clipping never mutates a segment and never counts time
 * outside the selected day.
 */
public record EmDayWindow(Instant start, Instant end) {
    public EmDayWindow {
        Objects.requireNonNull(start, "start");
        Objects.requireNonNull(end, "end");
        if (!start.isBefore(end)) throw new IllegalArgumentException("start/end");
    }

    /** Boundaries of {@code day} in the requested zone, including DST changes. */
    public static EmDayWindow of(LocalDate day, ZoneId zone) {
        Objects.requireNonNull(day, "day");
        Objects.requireNonNull(zone, "zone");
        return new EmDayWindow(day.atStartOfDay(zone).toInstant(),
                day.plusDays(1).atStartOfDay(zone).toInstant());
    }

    public Instant clipStart(Instant value) {
        Objects.requireNonNull(value, "value");
        return value.isBefore(start) ? start : value;
    }

    public Instant clipEnd(Instant value) {
        Objects.requireNonNull(value, "value");
        return value.isAfter(end) ? end : value;
    }

    /** Milliseconds of the interval inside this window; zero when it does not overlap. */
    public long clippedMillis(Instant from, Instant to) {
        Objects.requireNonNull(from, "from");
        Objects.requireNonNull(to, "to");
        Instant clippedStart = clipStart(from);
        Instant clippedEnd = clipEnd(to);
        return clippedEnd.isAfter(clippedStart) ? Duration.between(clippedStart, clippedEnd).toMillis() : 0L;
    }

    public long clippedMillis(EmSegment segment) {
        Objects.requireNonNull(segment, "segment");
        return clippedMillis(segment.start(), segment.end());
    }

    /** Semi-open overlap, so a segment ending exactly at {@code start} is outside. */
    public boolean overlaps(Instant from, Instant to) {
        Objects.requireNonNull(from, "from");
        Objects.requireNonNull(to, "to");
        return from.isBefore(end) && start.isBefore(to);
    }

    public boolean overlaps(EmSegment segment) {
        Objects.requireNonNull(segment, "segment");
        return overlaps(segment.start(), segment.end());
    }
}
