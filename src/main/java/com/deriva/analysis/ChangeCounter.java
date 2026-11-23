package com.deriva.analysis;

import com.deriva.privacy.ObservationKind;
import com.deriva.tracking.model.ContextKey;
import com.deriva.tracking.model.StableTick;

import java.util.Objects;
import java.util.Optional;

/**
 * Counts confirmed changes between consecutive observed contexts. Boundaries reset
 * continuity; pending candidates are not changes until the segmenter confirms them.
 */
public final class ChangeCounter {
    private ContextKey lastConfirmed;

    public Optional<Change> onTick(StableTick tick) {
        Objects.requireNonNull(tick, "tick");
        if (tick.kind() != ObservationKind.OBSERVED) {
            lastConfirmed = null;
            return Optional.empty();
        }
        if (tick.pendingChange()) return Optional.empty();

        ContextKey context = tick.context();
        if (lastConfirmed == null) {
            lastConfirmed = context;
            return Optional.empty();
        }
        if (lastConfirmed.equals(context)) return Optional.empty();

        lastConfirmed = context;
        return Optional.of(new Change(tick.timestamp(), context));
    }
}
