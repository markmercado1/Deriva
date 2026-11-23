package com.deriva.coach;

import com.deriva.coach.model.CloseReason;
import com.deriva.coach.model.DriftEpisode;
import com.deriva.coach.model.DriftStatus;
import com.deriva.tracking.model.ContextKey;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class DriftModelTest {
    private static final Instant T0 = Instant.parse("2026-11-22T10:00:00Z");
    private static final Instant T1 = T0.plusSeconds(10);
    private static final ContextKey A = new ContextKey("a.exe", null);
    private static final ContextKey B = new ContextKey("b.exe", null);

    @Test
    void validOpenEpisode() {
        DriftEpisode episode = new DriftEpisode(1, T0, T1, T1, null, null,
                DriftStatus.OPEN, null, 10, A, null, Set.of(A, B));
        assertEquals(DriftStatus.OPEN, episode.status());
    }

    @Test
    void openCannotCarryClosureFields() {
        assertThrows(IllegalArgumentException.class, () -> new DriftEpisode(1, T0, T1, T1, null, T1,
                DriftStatus.OPEN, null, 10, A, null, Set.of(A)));
    }

    @Test
    void stableRequiresStabilityAndTime() {
        assertThrows(IllegalArgumentException.class, () -> new DriftEpisode(1, T0, T1, T1, null, T1,
                DriftStatus.STABLE, null, 10, A, null, Set.of(A)));
        assertDoesNotThrow(() -> new DriftEpisode(1, T0, T1, T1, T0, T1,
                DriftStatus.STABLE, null, 10, A, 0L, Set.of(A)));
    }

    @Test
    void unconfirmedRequiresReason() {
        assertThrows(IllegalArgumentException.class, () -> new DriftEpisode(1, T0, T1, T1, null, T1,
                DriftStatus.UNCONFIRMED, null, 10, A, null, Set.of(A)));
        assertDoesNotThrow(() -> new DriftEpisode(1, T0, T1, T1, null, T1,
                DriftStatus.UNCONFIRMED, CloseReason.IDLE, 10, A, null, Set.of(A)));
    }

    @Test
    void firstContextMustBeIncludedAndThresholdIsTen() {
        assertThrows(IllegalArgumentException.class, () -> new DriftEpisode(1, T0, T1, T1, null, null,
                DriftStatus.OPEN, null, 10, A, null, Set.of(B)));
        assertThrows(IllegalArgumentException.class, () -> new DriftEpisode(1, T0, T1, T1, null, null,
                DriftStatus.OPEN, null, 9, A, null, Set.of(A)));
    }
}
