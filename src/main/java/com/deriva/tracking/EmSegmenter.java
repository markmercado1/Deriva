package com.deriva.tracking;

import com.deriva.privacy.EmObservation;
import com.deriva.privacy.EmObservationKind;
import com.deriva.tracking.model.EmContextKey;
import com.deriva.tracking.model.EmSegment;
import com.deriva.tracking.model.EmStableTick;
import com.deriva.tracking.model.EmTickResult;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Pure segmentation state machine. It applies debounce to observed candidates,
 * closes segments on confirmed changes and applies exclusions, idle and unavailable
 * boundaries immediately. It knows nothing about threads, SQL, HTTP or JavaFX.
 */
public final class EmSegmenter {
    private static final Duration DEBOUNCE = Duration.ofSeconds(3);
    private static final Duration MAX_GAP = Duration.ofSeconds(3);

    private EmObservationKind currentKind;
    private EmContextKey currentContext;
    private Instant currentSince;
    private Candidate candidate;
    private Instant lastTimestamp;

    public EmTickResult onObservation(EmObservation observation) {
        Objects.requireNonNull(observation, "observation");
        Instant now = observation.timestamp();
        List<EmSegment> completed = new ArrayList<>();

        if (lastTimestamp == null) {
            lastTimestamp = now;
            if (observation.kind() == EmObservationKind.OBSERVED) {
                candidate = new Candidate(contextOf(observation), now);
                return result(new EmStableTick(now, EmObservationKind.OBSERVED,
                        candidate.context, candidate.start, true), completed);
            }
            startBoundary(observation.kind(), now);
            return result(tickForCurrent(now), completed);
        }

        if (now.isBefore(lastTimestamp)) {
            closeCurrent(lastTimestamp, completed);
            candidate = null;
            currentKind = EmObservationKind.UNAVAILABLE;
            currentContext = null;
            currentSince = now;
            lastTimestamp = now;
            return result(tickForCurrent(now), completed);
        }

        if (Duration.between(lastTimestamp, now).compareTo(MAX_GAP) > 0) {
            closeCurrent(lastTimestamp, completed);
            candidate = null;
            currentKind = null;
            currentContext = null;
            currentSince = null;
            lastTimestamp = now;
            return startFresh(observation, now, completed);
        }

        lastTimestamp = now;
        if (observation.kind() != EmObservationKind.OBSERVED) {
            applyBoundary(observation.kind(), now, completed);
            return result(tickForCurrent(now), completed);
        }

        EmContextKey context = contextOf(observation);
        if (currentKind == EmObservationKind.OBSERVED && context.equals(currentContext)) {
            candidate = null;
            return result(tickForCurrent(now), completed);
        }

        if (candidate == null || !candidate.context.equals(context)) {
            candidate = new Candidate(context, now);
            return result(tickForCurrent(now), completed);
        }

        if (Duration.between(candidate.start, now).compareTo(DEBOUNCE) >= 0) {
            Instant candidateStart = candidate.start;
            EmContextKey candidateContext = candidate.context;
            closeSegment(currentSince, candidateStart, completed);
            currentKind = EmObservationKind.OBSERVED;
            currentContext = candidateContext;
            currentSince = candidateStart;
            candidate = null;
        }
        return result(tickForCurrent(now), completed);
    }

    /**
     * Returns the confirmed open segment and the candidate pending confirmation
     * without closing or mutating the state machine.
     */
    public Snapshot snapshot(Instant now) {
        Objects.requireNonNull(now, "now");
        Instant at = lastTimestamp == null ? now : lastTimestamp;
        EmSegment open = currentKind != null && currentSince != null && currentSince.isBefore(at)
                ? new EmSegment(0, currentKind, currentContext, null, null, currentSince, at)
                : null;
        EmSegment pending = candidate != null && candidate.start.isBefore(at)
                ? new EmSegment(0, EmObservationKind.OBSERVED, candidate.context, null, null,
                        candidate.start, at)
                : null;
        return new Snapshot(at, open, pending);
    }

    /**
     * Flushes the open segment and any pending candidate at session end, then resets
     * the state machine. A candidate cannot be confirmed after the session closes, so
     * it is persisted as OBSERVED from its first sample; the previous open segment is
     * closed at the candidate start to keep both intervals contiguous and non-overlapping.
     */
    public List<EmSegment> close(Instant end) {
        Objects.requireNonNull(end, "end");
        List<EmSegment> completed = new ArrayList<>();
        Instant effectiveEnd = lastTimestamp != null && end.isBefore(lastTimestamp) ? lastTimestamp : end;
        boolean candidateAvailable = candidate != null && candidate.start.isBefore(effectiveEnd)
                && (currentKind == null || currentSince == null
                || !candidate.start.isBefore(currentSince));
        if (candidateAvailable) {
            Instant candidateStart = candidate.start;
            if (currentKind != null && currentSince != null && currentSince.isBefore(candidateStart))
                completed.add(new EmSegment(0, currentKind, currentContext, null, null,
                        currentSince, candidateStart));
            completed.add(new EmSegment(0, EmObservationKind.OBSERVED, candidate.context, null, null,
                    candidateStart, effectiveEnd));
        } else {
            closeCurrent(effectiveEnd, completed);
        }
        candidate = null;
        currentKind = null;
        currentContext = null;
        currentSince = null;
        lastTimestamp = null;
        return List.copyOf(completed);
    }

    private EmTickResult startFresh(EmObservation observation, Instant now, List<EmSegment> completed) {
        if (observation.kind() == EmObservationKind.OBSERVED) {
            candidate = new Candidate(contextOf(observation), now);
            return result(new EmStableTick(now, EmObservationKind.OBSERVED,
                    candidate.context, candidate.start, true), completed);
        }
        startBoundary(observation.kind(), now);
        return result(tickForCurrent(now), completed);
    }

    private void applyBoundary(EmObservationKind kind, Instant now, List<EmSegment> completed) {
        if (currentKind == EmObservationKind.OBSERVED) {
            closeSegment(currentSince, now, completed);
            currentKind = null;
            currentContext = null;
            currentSince = null;
        } else if (currentKind != null && currentKind != kind) {
            closeSegment(currentSince, now, completed);
            currentSince = null;
        }
        if (currentSince == null) currentSince = now;
        currentKind = kind;
        currentContext = null;
        candidate = null;
    }

    private void startBoundary(EmObservationKind kind, Instant now) {
        currentKind = kind;
        currentContext = null;
        currentSince = now;
    }

    private void closeCurrent(Instant end, List<EmSegment> completed) {
        closeSegment(currentSince, end, completed);
    }

    private void closeSegment(Instant start, Instant end, List<EmSegment> completed) {
        if (currentKind == null || start == null || !start.isBefore(end)) return;
        completed.add(new EmSegment(0, currentKind, currentContext, null, null, start, end));
    }

    private EmStableTick tickForCurrent(Instant now) {
        if (currentKind == EmObservationKind.OBSERVED && currentContext != null)
            return new EmStableTick(now, EmObservationKind.OBSERVED, currentContext, currentSince, candidate != null);
        if (currentKind != null)
            return new EmStableTick(now, currentKind, null, currentSince, candidate != null);
        if (candidate != null)
            return new EmStableTick(now, EmObservationKind.OBSERVED, candidate.context, candidate.start, true);
        return new EmStableTick(now, EmObservationKind.UNAVAILABLE, null, now, false);
    }

    private static EmContextKey contextOf(EmObservation observation) {
        return new EmContextKey(observation.processName(), observation.siteHost());
    }

    private static EmTickResult result(EmStableTick tick, List<EmSegment> completed) {
        return new EmTickResult(tick, completed);
    }

    private record Candidate(EmContextKey context, Instant start) {}

    /** Immutable live view; open and pending may be null. */
    public record Snapshot(Instant at, EmSegment open, EmSegment pending) {
        public Snapshot {
            if (at == null) throw new IllegalArgumentException("at");
        }
    }
}
