package com.deriva.goals.model;

import java.time.Instant;
import java.util.Objects;

/**
 * Record of a goal alert: when a threshold was crossed, for which limit, and the
 * optional later outcome or justification. It never stores URLs, paths or titles.
 */
public record GoalAlert(
        long id,
        long goalId,
        Instant triggeredAt,
        int thresholdPercent,
        GoalMetric metric,
        String outcome,
        String justification) {

    public static final int MAX_JUSTIFICATION_LENGTH = 500;

    public GoalAlert {
        if (id < 0) throw new IllegalArgumentException("id");
        if (goalId <= 0) throw new IllegalArgumentException("goalId");
        Objects.requireNonNull(triggeredAt, "triggeredAt");
        if (thresholdPercent < 1 || thresholdPercent > 100)
            throw new IllegalArgumentException("thresholdPercent");
        Objects.requireNonNull(metric, "metric");
        if (justification != null && justification.length() > MAX_JUSTIFICATION_LENGTH)
            throw new IllegalArgumentException("justification");
    }

    /** New alert: no id yet, no outcome or justification until it is reviewed. */
    public static GoalAlert draft(long goalId, Instant triggeredAt, int thresholdPercent, GoalMetric metric) {
        return new GoalAlert(0, goalId, triggeredAt, thresholdPercent, metric, null, null);
    }
}
