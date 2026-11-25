package com.deriva.app.usecase;

import com.deriva.goals.EmDriftStateProvider;
import com.deriva.goals.EmOpenSegmentProvider;
import com.deriva.privacy.EmObservationKind;

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
public final class EmGoalAlertMonitor {
    public static final Duration DEFAULT_INTERVAL = Duration.ofSeconds(30);

    private final EmGoalAlertService service;
    private final Duration interval;
    private Instant nextEvaluation;

    public EmGoalAlertMonitor(EmGoalAlertService service) {
        this(service, DEFAULT_INTERVAL);
    }

    public EmGoalAlertMonitor(EmGoalAlertService service, Duration interval) {
        this.service = Objects.requireNonNull(service, "service");
        this.interval = Objects.requireNonNull(interval, "interval");
        if (interval.isZero() || interval.isNegative()) throw new IllegalArgumentException("interval");
    }

    /** Compatibility overload without a drift signal; it keeps the pre-F6B-5 behavior. */
    public boolean tick(EmCaptureState state, EmObservationKind current, Instant now,
                        EmOpenSegmentProvider openSegments) {
        return tick(state, current, now, openSegments, EmDriftStateProvider.inactive(),
                EmGoalAlertService.DEFAULT_DEFERRAL_MAX);
    }

    /**
     * Runs one capture tick. Returns true when the service evaluated, false when the
     * gate skipped it or the evaluation failed. No exception escapes to the loop.
     */
    public boolean tick(EmCaptureState state, EmObservationKind current, Instant now,
                        EmOpenSegmentProvider openSegments, EmDriftStateProvider drift,
                        Duration deferralMax) {
        Objects.requireNonNull(state, "state");
        Objects.requireNonNull(current, "current");
        Objects.requireNonNull(now, "now");
        Objects.requireNonNull(openSegments, "openSegments");
        Objects.requireNonNull(drift, "drift");
        Objects.requireNonNull(deferralMax, "deferralMax");

        if (state != EmCaptureState.RUNNING || current != EmObservationKind.OBSERVED) {
            interrupt(now);
            return false;
        }
        if (nextEvaluation != null && now.isBefore(nextEvaluation)) return false;
        nextEvaluation = now.plus(interval);
        try {
            service.evaluate(now, openSegments, drift, deferralMax);
            return true;
        } catch (RuntimeException e) {
            System.err.println("No se pudieron evaluar los objetivos; se reintentará en "
                    + interval.toSeconds() + " s (" + e.getClass().getSimpleName() + ").");
            return false;
        }
    }

    /**
     * Closes any open post-notice observation without touching notices that are
     * still pending. Failures are ignored: this is auxiliary to the capture loop.
     */
    public int interrupt(Instant now) {
        try {
            return service.interruptObservations(now);
        } catch (RuntimeException ignored) {
            return 0;
        }
    }
}
