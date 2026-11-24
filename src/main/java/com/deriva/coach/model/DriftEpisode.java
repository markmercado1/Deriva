package com.deriva.coach.model;

import com.deriva.tracking.model.ContextKey;

import java.time.Instant;
import java.util.Set;

/**
 * Immutable snapshot of a drift episode. Only STABLE has a stability start and a
 * non-null time to stability; only UNCONFIRMED has a close reason.
 */
public record DriftEpisode(
        long id,
        Instant startTs,
        Instant detectedAt,
        Instant lastObservedTs,
        Instant stabilityStartTs,
        Instant closedAt,
        DriftStatus status,
        CloseReason closeReason,
        int switchCount,
        ContextKey firstChangeContext,
        Long timeToStabilityMs,
        Set<ContextKey> contexts) {

    public DriftEpisode {
        if (id <= 0 || startTs == null || detectedAt == null || lastObservedTs == null
                || status == null || switchCount < 1 || firstChangeContext == null
                || contexts == null || contexts.isEmpty())
            throw new IllegalArgumentException("Invalid drift episode");
        contexts = Set.copyOf(contexts);
        if (!contexts.contains(firstChangeContext))
            throw new IllegalArgumentException("Missing first change context");
        if (closedAt != null && lastObservedTs.isAfter(closedAt))
            throw new IllegalArgumentException("lastObservedTs after closedAt");

        switch (status) {
            case OPEN -> {
                if (stabilityStartTs != null || closedAt != null || closeReason != null || timeToStabilityMs != null)
                    throw new IllegalArgumentException("OPEN with closure fields");
            }
            case STABLE -> {
                if (stabilityStartTs == null || closedAt == null || closeReason != null
                        || timeToStabilityMs == null || timeToStabilityMs < 0)
                    throw new IllegalArgumentException("STABLE without stable closure");
                if (stabilityStartTs.isBefore(startTs) || stabilityStartTs.isAfter(closedAt))
                    throw new IllegalArgumentException("Invalid stability start");
            }
            case UNCONFIRMED -> {
                if (stabilityStartTs != null || closedAt == null || closeReason == null || timeToStabilityMs != null)
                    throw new IllegalArgumentException("UNCONFIRMED closure fields");
            }
        }
    }
}
