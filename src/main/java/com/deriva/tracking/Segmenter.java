package com.deriva.tracking;

import com.deriva.privacy.Observation;
import com.deriva.privacy.ObservationKind;
import com.deriva.tracking.model.ContextKey;
import com.deriva.tracking.model.Segment;
import com.deriva.tracking.model.StableTick;
import com.deriva.tracking.model.TickResult;

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
public final class Segmenter {
    private static final Duration DEBOUNCE = Duration.ofSeconds(3);
    private static final Duration MAX_GAP = Duration.ofSeconds(3);

    private ObservationKind currentKind;
    private ContextKey currentContext;
    private Instant currentSince;
    private Candidate candidate;
    private Instant lastTimestamp;

    public TickResult onObservation(Observation observation) {
        Objects.requireNonNull(observation, "observation");
        Instant now = observation.timestamp();
        List<Segment> completed = new ArrayList<>();

        if (lastTimestamp == null) {
            lastTimestamp = now;
            if (observation.kind() == ObservationKind.OBSERVED) {
                candidate = new Candidate(contextOf(observation), now);
                return result(new StableTick(now, ObservationKind.OBSERVED,
                        candidate.context, candidate.start, true), completed);
            }
            startBoundary(observation.kind(), now);
            return result(tickForCurrent(now), completed);
        }

        if (now.isBefore(lastTimestamp)) {
            closeCurrent(lastTimestamp, completed);
            candidate = null;
            currentKind = ObservationKind.UNAVAILABLE;
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
        if (observation.kind() != ObservationKind.OBSERVED) {
            applyBoundary(observation.kind(), now, completed);
            return result(tickForCurrent(now), completed);
        }

        ContextKey context = contextOf(observation);
        if (currentKind == ObservationKind.OBSERVED && context.equals(currentContext)) {
            candidate = null;
            return result(tickForCurrent(now), completed);
        }

        if (candidate == null || !candidate.context.equals(context)) {
            candidate = new Candidate(context, now);
            return result(tickForCurrent(now), completed);
        }

        if (Duration.between(candidate.start, now).compareTo(DEBOUNCE) >= 0) {
            Instant candidateStart = candidate.start;
            ContextKey candidateContext = candidate.context;
            closeSegment(currentSince, candidateStart, completed);
            currentKind = ObservationKind.OBSERVED;
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
        Segment open = currentKind != null && currentSince != null && currentSince.isBefore(at)
                ? new Segment(0, currentKind, currentContext, null, null, currentSince, at)
                : null;
        Segment pending = candidate != null && candidate.start.isBefore(at)
                ? new Segment(0, ObservationKind.OBSERVED, candidate.context, null, null,
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
    public List<Segment> close(Instant end) {
        Objects.requireNonNull(end, "end");
        List<Segment> completed = new ArrayList<>();
        Instant effectiveEnd = lastTimestamp != null && end.isBefore(lastTimestamp) ? lastTimestamp : end;
        boolean candidateAvailable = candidate != null && candidate.start.isBefore(effectiveEnd)
                && (currentKind == null || currentSince == null
                || !candidate.start.isBefore(currentSince));
        if (candidateAvailable) {
            Instant candidateStart = candidate.start;
            if (currentKind != null && currentSince != null && currentSince.isBefore(candidateStart))
                completed.add(new Segment(0, currentKind, currentContext, null, null,
                        currentSince, candidateStart));
            completed.add(new Segment(0, ObservationKind.OBSERVED, candidate.context, null, null,
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

    private TickResult startFresh(Observation observation, Instant now, List<Segment> completed) {
        if (observation.kind() == ObservationKind.OBSERVED) {
            candidate = new Candidate(contextOf(observation), now);
            return result(new StableTick(now, ObservationKind.OBSERVED,
                    candidate.context, candidate.start, true), completed);
        }
        startBoundary(observation.kind(), now);
        return result(tickForCurrent(now), completed);
    }

    private void applyBoundary(ObservationKind kind, Instant now, List<Segment> completed) {
        if (currentKind == ObservationKind.OBSERVED) {
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

    private void startBoundary(ObservationKind kind, Instant now) {
        currentKind = kind;
        currentContext = null;
        currentSince = now;
    }

    private void closeCurrent(Instant end, List<Segment> completed) {
        closeSegment(currentSince, end, completed);
    }

    private void closeSegment(Instant start, Instant end, List<Segment> completed) {
        if (currentKind == null || start == null || !start.isBefore(end)) return;
        completed.add(new Segment(0, currentKind, currentContext, null, null, start, end));
    }

    private StableTick tickForCurrent(Instant now) {
        if (currentKind == ObservationKind.OBSERVED && currentContext != null)
            return new StableTick(now, ObservationKind.OBSERVED, currentContext, currentSince, candidate != null);
        if (currentKind != null)
            return new StableTick(now, currentKind, null, currentSince, candidate != null);
        if (candidate != null)
            return new StableTick(now, ObservationKind.OBSERVED, candidate.context, candidate.start, true);
        return new StableTick(now, ObservationKind.UNAVAILABLE, null, now, false);
    }

    private static ContextKey contextOf(Observation observation) {
        return new ContextKey(observation.processName(), observation.siteHost());
    }

    private static TickResult result(StableTick tick, List<Segment> completed) {
        return new TickResult(tick, completed);
    }

    private record Candidate(ContextKey context, Instant start) {}

    /** Immutable live view; open and pending may be null. */
    public record Snapshot(Instant at, Segment open, Segment pending) {
        public Snapshot {
            if (at == null) throw new IllegalArgumentException("at");
        }
    }
}
