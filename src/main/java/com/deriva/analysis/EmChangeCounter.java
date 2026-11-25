package com.deriva.analysis;

import com.deriva.privacy.EmObservationKind;
import com.deriva.tracking.model.EmContextKey;
import com.deriva.tracking.model.EmStableTick;

import java.util.Objects;
import java.util.Optional;

/**
 * Counts confirmed changes between consecutive observed contexts. Boundaries reset
 * continuity; pending candidates are not changes until the segmenter confirms them.
 */
public final class EmChangeCounter {
    private EmContextKey lastConfirmed;

    public Optional<EmChange> onTick(EmStableTick tick) {
        Objects.requireNonNull(tick, "tick");
        if (tick.kind() != EmObservationKind.OBSERVED) {
            lastConfirmed = null;
            return Optional.empty();
        }
        if (tick.pendingChange()) return Optional.empty();

        EmContextKey context = tick.context();
        if (lastConfirmed == null) {
            lastConfirmed = context;
            return Optional.empty();
        }
        if (lastConfirmed.equals(context)) return Optional.empty();

        lastConfirmed = context;
        return Optional.of(new EmChange(tick.timestamp(), context));
    }
}
