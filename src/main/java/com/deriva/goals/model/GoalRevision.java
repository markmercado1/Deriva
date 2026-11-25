package com.deriva.goals.model;

import java.time.Instant;
import java.util.Objects;

/** A persisted configuration event; migration baselines do not prove earlier state. */
public record GoalRevision(Goal goal, Instant recordedAt, boolean archived, boolean baseline) {
    public GoalRevision {
        Objects.requireNonNull(goal);
        Objects.requireNonNull(recordedAt);
    }
}
