package com.deriva.goals.model;

import java.time.Instant;
import java.util.Objects;

/**
 * Record of a goal alert: when a threshold was crossed, for which limit, and the
 * optional later outcome or justification. It never stores URLs, paths or titles.
 *
 * <p>{@code notifiedAt} is when the notice was actually delivered; a null value means
 * the notice is still pending. {@code observedUntil} is the end of the post-notice
 * observation window. Legacy alerts migrated from V3 have {@code notifiedAt} set to
 * their trigger moment and no observation window.</p>
 *
 * <p>{@code outcome} is an observation, never proof of a cause. The stored codes are
 * {@link #OUTCOME_STABLE}, {@link #OUTCOME_NO_DRIFT}, {@link #OUTCOME_NEW_DRIFT},
 * {@link #OUTCOME_STILL_DRIFTING}, {@link #OUTCOME_NOT_OBSERVED} and
 * {@link #OUTCOME_DISCARDED}. {@code outcomeAfterMs} is only set for
 * {@link #OUTCOME_STABLE}.</p>
 *
 * <p>{@code justifiedAt} marks a user justification of the excess of a 100 % alert.
 * {@code justifiedExcess} is the excess covered when it was marked, in the metric's
 * units: milliseconds for {@link EmGoalMetric#MINUTES} and a visit count for
 * {@link EmGoalMetric#ENTRIES}. A later use is not covered.</p>
 */
public record EmGoalAlert(
        long id,
        long goalId,
        Instant triggeredAt,
        int thresholdPercent,
        EmGoalMetric metric,
        String outcome,
        String justification,
        Instant justifiedAt,
        Long justifiedExcess,
        Instant notifiedAt,
        Instant observedUntil,
        Instant outcomeAt,
        Long outcomeAfterMs) {

    public static final int MAX_JUSTIFICATION_LENGTH = 500;

    public static final String OUTCOME_STABLE = "STABLE";
    public static final String OUTCOME_NO_DRIFT = "NO_DRIFT";
    public static final String OUTCOME_NEW_DRIFT = "NEW_DRIFT";
    public static final String OUTCOME_STILL_DRIFTING = "STILL_DRIFTING";
    public static final String OUTCOME_NOT_OBSERVED = "NOT_OBSERVED";
    public static final String OUTCOME_DISCARDED = "DISCARDED";

    public EmGoalAlert {
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
        if (outcomeAfterMs != null && outcomeAfterMs < 0)
            throw new IllegalArgumentException("outcomeAfterMs");
        if (outcome == null && (outcomeAt != null || outcomeAfterMs != null))
            throw new IllegalArgumentException("outcome data without outcome");
    }

    /** Alert as recorded by the alarm, before any delivery or user response. */
    public static EmGoalAlert draft(long goalId, Instant triggeredAt, int thresholdPercent, EmGoalMetric metric) {
        return new EmGoalAlert(0, goalId, triggeredAt, thresholdPercent, metric, null, null, null, null,
                null, null, null, null);
    }

    /** Keeps compatibility with rows recorded before the observation columns existed. */
    public EmGoalAlert(long id, long goalId, Instant triggeredAt, int thresholdPercent,
                     EmGoalMetric metric, String outcome, String justification) {
        this(id, goalId, triggeredAt, thresholdPercent, metric, outcome, justification,
                null, null, null, null, null, null);
    }

    /** Keeps compatibility with the V3 model (justification without observation). */
    public EmGoalAlert(long id, long goalId, Instant triggeredAt, int thresholdPercent,
                     EmGoalMetric metric, String outcome, String justification,
                     Instant justifiedAt, Long justifiedExcess) {
        this(id, goalId, triggeredAt, thresholdPercent, metric, outcome, justification,
                justifiedAt, justifiedExcess, null, null, null, null);
    }

    /** Returns a copy with the delivery moment and the end of its observation window. */
    public EmGoalAlert notified(Instant at, Instant until) {
        Objects.requireNonNull(at, "at");
        Objects.requireNonNull(until, "until");
        if (until.isBefore(at)) throw new IllegalArgumentException("until");
        return new EmGoalAlert(id, goalId, triggeredAt, thresholdPercent, metric, outcome,
                justification, justifiedAt, justifiedExcess, at, until, outcomeAt, outcomeAfterMs);
    }

    /** Returns a copy with the observed outcome and no further observation. */
    public EmGoalAlert observed(String observedOutcome, Instant at, Long afterMs) {
        if (observedOutcome == null || observedOutcome.isBlank())
            throw new IllegalArgumentException("outcome");
        Objects.requireNonNull(at, "at");
        if (afterMs != null && afterMs < 0) throw new IllegalArgumentException("afterMs");
        return new EmGoalAlert(id, goalId, triggeredAt, thresholdPercent, metric,
                observedOutcome, justification, justifiedAt, justifiedExcess,
                notifiedAt, observedUntil, at, afterMs);
    }

    /** Returns a copy with the user justification; the reason is stored exactly as given. */
    public EmGoalAlert justified(long coveredExcess, Instant at, String reason) {
        if (thresholdPercent != 100) throw new IllegalArgumentException("only a 100 % alert");
        if (coveredExcess < 0) throw new IllegalArgumentException("coveredExcess");
        Objects.requireNonNull(at, "at");
        return new EmGoalAlert(id, goalId, triggeredAt, thresholdPercent, metric,
                outcome, reason, at, coveredExcess, notifiedAt, observedUntil, outcomeAt,
                outcomeAfterMs);
    }

    /** Returns a copy without the user justification; the alert itself is kept. */
    public EmGoalAlert unjustified() {
        return new EmGoalAlert(id, goalId, triggeredAt, thresholdPercent, metric,
                outcome, null, null, null, notifiedAt, observedUntil, outcomeAt, outcomeAfterMs);
    }

    public boolean isJustified() {
        return justifiedAt != null;
    }

    /** True while the notice has not been delivered yet. */
    public boolean isPendingNotice() {
        return notifiedAt == null && outcome == null;
    }

    /** True while a delivered notice is still waiting for its observation to finish. */
    public boolean isObserving() {
        return notifiedAt != null && observedUntil != null && outcome == null;
    }
}
