package com.deriva.app.usecase;

import com.deriva.goals.OpenSegmentProvider;
import com.deriva.privacy.ObservationKind;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

/**
 * Decides when the capture loop evaluates goal alerts and isolates its failures.
 *
 * <p>It has no thread of its own: the capture thread calls {@link #tick}. A manual
 * pause stops that loop, and only an {@code OBSERVED} sample allows an evaluation, so
 * a locked or suspended machine does not notify. A failing evaluation is logged and
 * retried on the next 30-second cycle without interrupting capture.</p>
 */
public final class GoalAlertMonitor {
    public static final Duration DEFAULT_INTERVAL = Duration.ofSeconds(30);

    private final GoalAlertService service;
    private final Duration interval;
    private Instant nextEvaluation;

    public GoalAlertMonitor(GoalAlertService service) {
        this(service, DEFAULT_INTERVAL);
    }

    public GoalAlertMonitor(GoalAlertService service, Duration interval) {
        this.service = Objects.requireNonNull(service, "service");
        this.interval = Objects.requireNonNull(interval, "interval");
        if (interval.isZero() || interval.isNegative()) throw new IllegalArgumentException("interval");
    }

    /**
     * Runs one capture tick. Returns true when the service evaluated, false when the
     * gate skipped it or the evaluation failed. No exception escapes to the loop.
     */
    public boolean tick(CaptureState state, ObservationKind current, Instant now,
                        OpenSegmentProvider openSegments) {
        Objects.requireNonNull(state, "state");
        Objects.requireNonNull(current, "current");
        Objects.requireNonNull(now, "now");
        Objects.requireNonNull(openSegments, "openSegments");

        if (state != CaptureState.RUNNING || current != ObservationKind.OBSERVED) return false;
        if (nextEvaluation != null && now.isBefore(nextEvaluation)) return false;
        nextEvaluation = now.plus(interval);
        try {
            service.evaluate(now, openSegments);
            return true;
        } catch (RuntimeException e) {
            System.err.println("No se pudieron evaluar los objetivos; se reintentará en "
                    + interval.toSeconds() + " s (" + e.getClass().getSimpleName() + ").");
            return false;
        }
    }
}
