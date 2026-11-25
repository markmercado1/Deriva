package com.deriva.goals.model;

import com.deriva.tracking.model.ContextKey;

import java.time.Instant;
import java.util.Objects;

/**
 * A user-defined daily limit for one context: a process, or a process plus site host.
 *
 * <p>At least one limit is mandatory. A goal never classifies a context as good or
 * bad; it only fixes a limit the user chose. Stored goals have an id and timestamps;
 * a new goal uses {@link #draft} and receives both from the repository.</p>
 */
public record Goal(
        long id,
        ContextKey context,
        Integer dailyMinutes,
        Integer dailyEntries,
        boolean passive,
        boolean active,
        Instant createdAt,
        Instant updatedAt) {

    public Goal {
        if (id < 0) throw new IllegalArgumentException("id");
        Objects.requireNonNull(context, "context");
        if (dailyMinutes == null && dailyEntries == null)
            throw new IllegalArgumentException("goal must define at least one limit");
        if (dailyMinutes != null && dailyMinutes <= 0)
            throw new IllegalArgumentException("dailyMinutes");
        if (dailyEntries != null && dailyEntries <= 0)
            throw new IllegalArgumentException("dailyEntries");
        if (id == 0) {
            if (createdAt != null || updatedAt != null)
                throw new IllegalArgumentException("new goal has no timestamps");
        } else if (createdAt == null || updatedAt == null) {
            throw new IllegalArgumentException("stored goal needs timestamps");
        }
    }

    /** New goal: no id yet, active and without timestamps until the repository stores it. */
    public static Goal draft(ContextKey context, Integer dailyMinutes, Integer dailyEntries, boolean passive) {
        return new Goal(0, context, dailyMinutes, dailyEntries, passive, true, null, null);
    }
}
