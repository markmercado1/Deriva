package com.deriva.goals.model;

import java.util.Objects;

/**
 * Measured progress for one active goal on one local day.
 *
 * <p>{@code usedMillis} is clipped to the day and counts each observed interval once.
 * {@code entries} counts visits separated by at least the debounce gap. When
 * {@code unmeasurable} is true, some excluded time could belong to this context but
 * carries no identity, so the measured time may be incomplete.</p>
 */
public record GoalProgress(
        Goal goal,
        long usedMillis,
        int entries,
        GoalState state,
        boolean unmeasurable) {

    public GoalProgress {
        Objects.requireNonNull(goal, "goal");
        if (usedMillis < 0) throw new IllegalArgumentException("usedMillis");
        if (entries < 0) throw new IllegalArgumentException("entries");
        Objects.requireNonNull(state, "state");
    }

    /** Whole minutes used; callers that need exact thresholds use {@link #usedMillis()}. */
    public long usedMinutes() {
        return usedMillis / 60_000L;
    }
}
