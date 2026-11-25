package com.deriva.goals.model;

import java.time.Instant;
import java.util.Objects;

/**
 * A user justification for the excess of one goal limit.
 *
 * <p>{@code covered} is expressed in the metric's own units: milliseconds for
 * {@link GoalMetric#MINUTES} and a visit count for {@link GoalMetric#ENTRIES}. It is
 * the excess that was accumulated when the user marked the justification; later use
 * counts again. The justification belongs to the local day of {@code justifiedAt},
 * so the same alert cannot cover another day's excess.</p>
 */
public record GoalJustification(
        long goalId,
        GoalMetric metric,
        long covered,
        Instant justifiedAt) {

    public GoalJustification {
        if (goalId <= 0) throw new IllegalArgumentException("goalId");
        Objects.requireNonNull(metric, "metric");
        if (covered < 0) throw new IllegalArgumentException("covered");
        Objects.requireNonNull(justifiedAt, "justifiedAt");
    }
}
