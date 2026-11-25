package com.deriva.tracking;

import com.deriva.privacy.EmObservationKind;
import com.deriva.tracking.model.EmContextKey;
import com.deriva.tracking.model.EmSegment;
import com.deriva.tracking.model.EmStableTick;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class EmTrackingModelTest {
    private static final Instant T = Instant.parse("2026-11-22T10:00:00Z");
    private static final EmContextKey CONTEXT = new EmContextKey("notepad.exe", null);

    @Test
    void observedSegmentRequiresContextAndValidInterval() {
        assertThrows(IllegalArgumentException.class,
                () -> new EmSegment(0, EmObservationKind.OBSERVED, null, null, null, T, T.plusSeconds(1)));
        assertThrows(IllegalArgumentException.class,
                () -> new EmSegment(0, EmObservationKind.OBSERVED, CONTEXT, null, null, T.plusSeconds(1), T));
    }

    @Test
    void nonObservedSegmentCannotCarryIdentity() {
        assertThrows(IllegalArgumentException.class,
                () -> new EmSegment(0, EmObservationKind.IDLE, CONTEXT, null, null, T, T.plusSeconds(1)));
        assertThrows(IllegalArgumentException.class,
                () -> new EmSegment(0, EmObservationKind.IDLE, null, "label", null, T, T.plusSeconds(1)));
        assertDoesNotThrow(() -> new EmSegment(0, EmObservationKind.IDLE, null, null, null, T, T.plusSeconds(1)));
    }

    @Test
    void stableTickMatchesObservedAndBoundaryKinds() {
        assertThrows(IllegalArgumentException.class,
                () -> new EmStableTick(T, EmObservationKind.OBSERVED, null, T, false));
        assertThrows(IllegalArgumentException.class,
                () -> new EmStableTick(T, EmObservationKind.IDLE, CONTEXT, T, false));
        assertThrows(IllegalArgumentException.class,
                () -> new EmStableTick(T, EmObservationKind.OBSERVED, CONTEXT, T.plusSeconds(1), false));
        assertDoesNotThrow(() -> new EmStableTick(T, EmObservationKind.OBSERVED, CONTEXT, T, true));
        assertDoesNotThrow(() -> new EmStableTick(T, EmObservationKind.EXCLUDED, null, T, true));
    }
}
