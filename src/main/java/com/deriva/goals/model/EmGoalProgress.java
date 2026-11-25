package com.deriva.goals.model;

import java.util.List;
import java.util.Objects;

/**
 * Measured progress for one active goal on one local day.
 *
 * <p>{@code usedMillis} is clipped to the day and counts each observed interval once.
 * {@code entries} counts visits separated by at least the debounce gap. When
 * {@code unmeasurable} is true, some excluded time could belong to this context but
 * carries no identity, so the measured time may be incomplete. {@code justifications}
 * contains only the user justifications applied to this goal on this day; raw usage is
 * never rewritten.</p>
 */
public record EmGoalProgress(
        EmGoal goal,
        long usedMillis,
        int entries,
        EmGoalState state,
        boolean unmeasurable,
        List<EmGoalJustification> justifications) {

    public EmGoalProgress {
        Objects.requireNonNull(goal, "goal");
        if (usedMillis < 0) throw new IllegalArgumentException("usedMillis");
        if (entries < 0) throw new IllegalArgumentException("entries");
        Objects.requireNonNull(state, "state");
        justifications = List.copyOf(Objects.requireNonNull(justifications, "justifications"));
    }

    /** Progress without any applied justification. */
    public EmGoalProgress(EmGoal goal, long usedMillis, int entries, EmGoalState state,
                        boolean unmeasurable) {
        this(goal, usedMillis, entries, state, unmeasurable, List.of());
    }

    /** Whole minutes used; callers that need exact thresholds use {@link #usedMillis()}. */
    public long usedMinutes() {
        return usedMillis / 60_000L;
    }

    /** Covered excess in milliseconds, from the applied minute justifications. */
    public long justifiedMillis() {
        return justifications.stream()
                .filter(item -> item.metric() == EmGoalMetric.MINUTES)
                .mapToLong(EmGoalJustification::covered)
                .sum();
    }

    /** Covered excess in entries, from the applied entry justifications. */
    public int justifiedEntries() {
        return justifications.stream()
                .filter(item -> item.metric() == EmGoalMetric.ENTRIES)
                .mapToInt(item -> Math.toIntExact(item.covered()))
                .sum();
    }

    public boolean isJustified() {
        return !justifications.isEmpty();
    }
}
