package com.deriva.coach.model;

import com.deriva.tracking.model.EmContextKey;

import java.time.Instant;
import java.util.Set;

/** Persisted drift episode including user annotations. */
public record EmDriftEvent(
        long id,
        Instant startTs,
        Instant detectedAt,
        Instant lastObservedTs,
        Instant stabilityStartTs,
        Instant closedAt,
        EmDriftStatus status,
        EmCloseReason closeReason,
        int switchCount,
        EmContextKey firstChangeContext,
        Long timeToStabilityMs,
        String reason,
        boolean justified,
        Instant answeredAt,
        Set<EmContextKey> contexts) {

    private static final Set<String> REASONS =
            Set.of("SEARCHING_INFO", "MESSAGES", "BREAK", "BOREDOM", "OTHER");

    public EmDriftEvent {
        if (id <= 0 || startTs == null || detectedAt == null || lastObservedTs == null
                || status == null || switchCount < 1 || firstChangeContext == null
                || contexts == null || contexts.isEmpty())
            throw new IllegalArgumentException("Invalid drift event");
        contexts = Set.copyOf(contexts);
        if (!contexts.contains(firstChangeContext))
            throw new IllegalArgumentException("Missing first change context");
        if (closedAt != null && lastObservedTs.isAfter(closedAt))
            throw new IllegalArgumentException("lastObservedTs after closedAt");
        if (reason != null && !REASONS.contains(reason))
            throw new IllegalArgumentException("Invalid reason");

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
                if (stabilityStartTs != null || closedAt == null || closeReason == null
                        || timeToStabilityMs != null)
                    throw new IllegalArgumentException("UNCONFIRMED closure fields");
            }
        }
    }

    public static boolean isValidReason(String reason) {
        return reason != null && REASONS.contains(reason);
    }
}
