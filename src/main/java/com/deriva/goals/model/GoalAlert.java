package com.deriva.goals.model;

import java.time.Instant;
import java.util.Objects;

/**
 * Record of a goal alert: when a threshold was crossed, for which limit, and the
 * optional later outcome or justification. It never stores URLs, paths or titles.
 *
 * <p>{@code justifiedAt} marks a user justification of the excess of a 100 % alert.
 * {@code justifiedExcess} is the excess covered when it was marked, in the metric's
 * units: milliseconds for {@link GoalMetric#MINUTES} and a visit count for
 * {@link GoalMetric#ENTRIES}. A later use is not covered.</p>
 */
public record GoalAlert(
        long id,
        long goalId,
        Instant triggeredAt,
        int thresholdPercent,
        GoalMetric metric,
        String outcome,
        String justification,
        Instant justifiedAt,
        Long justifiedExcess) {

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
        if ((justifiedAt == null) != (justifiedExcess == null))
            throw new IllegalArgumentException("justification needs both moment and covered excess");
        if (justifiedExcess != null && justifiedExcess < 0)
            throw new IllegalArgumentException("justifiedExcess");
        if (justifiedAt != null && thresholdPercent != 100)
            throw new IllegalArgumentException("only a 100 % alert can be justified");
    }

    /** Alert as recorded by the alarm, before any user response. */
    public static GoalAlert draft(long goalId, Instant triggeredAt, int thresholdPercent, GoalMetric metric) {
        return new GoalAlert(0, goalId, triggeredAt, thresholdPercent, metric, null, null, null, null);
    }

    /** Keeps compatibility with alerts recorded before the justification columns existed. */
    public GoalAlert(long id, long goalId, Instant triggeredAt, int thresholdPercent,
                     GoalMetric metric, String outcome, String justification) {
        this(id, goalId, triggeredAt, thresholdPercent, metric, outcome, justification, null, null);
    }

    /** Returns a copy with the user justification; the reason is stored exactly as given. */
    public GoalAlert justified(long coveredExcess, Instant at, String reason) {
        if (thresholdPercent != 100) throw new IllegalArgumentException("only a 100 % alert");
        if (coveredExcess < 0) throw new IllegalArgumentException("coveredExcess");
        Objects.requireNonNull(at, "at");
        return new GoalAlert(id, goalId, triggeredAt, thresholdPercent, metric,
                outcome, reason, at, coveredExcess);
    }

    /** Returns a copy without the user justification; the alert itself is kept. */
    public GoalAlert unjustified() {
        return new GoalAlert(id, goalId, triggeredAt, thresholdPercent, metric,
                outcome, null, null, null);
    }

    public boolean isJustified() {
        return justifiedAt != null;
    }
}
