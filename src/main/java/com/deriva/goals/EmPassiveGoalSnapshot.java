package com.deriva.goals;

import com.deriva.goals.model.EmGoal;

import java.util.List;
import java.util.Objects;

/**
 * Coherent snapshot of the active goals used by the passive-activity rule.
 * The revision changes whenever a goal is created, edited, activated, deactivated or deleted.
 */
public record EmPassiveGoalSnapshot(long revision, List<EmGoal> activeGoals) {
    public EmPassiveGoalSnapshot {
        if (revision < 0) throw new IllegalArgumentException("revision");
        activeGoals = List.copyOf(Objects.requireNonNull(activeGoals, "activeGoals"));
    }
}
