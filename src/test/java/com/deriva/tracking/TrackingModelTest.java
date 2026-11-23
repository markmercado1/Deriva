package com.deriva.tracking;

import com.deriva.privacy.ObservationKind;
import com.deriva.tracking.model.ContextKey;
import com.deriva.tracking.model.Segment;
import com.deriva.tracking.model.StableTick;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class TrackingModelTest {
    private static final Instant T = Instant.parse("2026-11-22T10:00:00Z");
    private static final ContextKey CONTEXT = new ContextKey("notepad.exe", null);

    @Test
    void observedSegmentRequiresContextAndValidInterval() {
        assertThrows(IllegalArgumentException.class,
                () -> new Segment(0, ObservationKind.OBSERVED, null, null, null, T, T.plusSeconds(1)));
        assertThrows(IllegalArgumentException.class,
                () -> new Segment(0, ObservationKind.OBSERVED, CONTEXT, null, null, T.plusSeconds(1), T));
    }

    @Test
    void nonObservedSegmentCannotCarryIdentity() {
        assertThrows(IllegalArgumentException.class,
                () -> new Segment(0, ObservationKind.IDLE, CONTEXT, null, null, T, T.plusSeconds(1)));
        assertThrows(IllegalArgumentException.class,
                () -> new Segment(0, ObservationKind.IDLE, null, "label", null, T, T.plusSeconds(1)));
        assertDoesNotThrow(() -> new Segment(0, ObservationKind.IDLE, null, null, null, T, T.plusSeconds(1)));
    }

    @Test
    void stableTickMatchesObservedAndBoundaryKinds() {
        assertThrows(IllegalArgumentException.class,
                () -> new StableTick(T, ObservationKind.OBSERVED, null, T, false));
        assertThrows(IllegalArgumentException.class,
                () -> new StableTick(T, ObservationKind.IDLE, CONTEXT, T, false));
        assertThrows(IllegalArgumentException.class,
                () -> new StableTick(T, ObservationKind.OBSERVED, CONTEXT, T.plusSeconds(1), false));
        assertDoesNotThrow(() -> new StableTick(T, ObservationKind.OBSERVED, CONTEXT, T, true));
        assertDoesNotThrow(() -> new StableTick(T, ObservationKind.EXCLUDED, null, T, true));
    }
}
