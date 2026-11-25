package com.deriva.coach;

import com.deriva.coach.model.EmCloseReason;
import com.deriva.coach.model.EmDriftEpisode;
import com.deriva.coach.model.EmDriftEvent;
import com.deriva.coach.model.EmDriftStatus;
import com.deriva.tracking.model.EmContextKey;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class EmDriftModelTest {
    private static final Instant T0 = Instant.parse("2026-11-22T10:00:00Z");
    private static final Instant T1 = T0.plusSeconds(10);
    private static final EmContextKey A = new EmContextKey("a.exe", null);
    private static final EmContextKey B = new EmContextKey("b.exe", null);

    @Test
    void validOpenEpisode() {
        EmDriftEpisode episode = new EmDriftEpisode(1, T0, T1, T1, null, null,
                EmDriftStatus.OPEN, null, 10, A, null, Set.of(A, B));
        assertEquals(EmDriftStatus.OPEN, episode.status());
    }

    @Test
    void openCannotCarryClosureFields() {
        assertThrows(IllegalArgumentException.class, () -> new EmDriftEpisode(1, T0, T1, T1, null, T1,
                EmDriftStatus.OPEN, null, 10, A, null, Set.of(A)));
    }

    @Test
    void stableRequiresStabilityAndTime() {
        assertThrows(IllegalArgumentException.class, () -> new EmDriftEpisode(1, T0, T1, T1, null, T1,
                EmDriftStatus.STABLE, null, 10, A, null, Set.of(A)));
        assertDoesNotThrow(() -> new EmDriftEpisode(1, T0, T1, T1, T0, T1,
                EmDriftStatus.STABLE, null, 10, A, 0L, Set.of(A)));
    }

    @Test
    void unconfirmedRequiresReason() {
        assertThrows(IllegalArgumentException.class, () -> new EmDriftEpisode(1, T0, T1, T1, null, T1,
                EmDriftStatus.UNCONFIRMED, null, 10, A, null, Set.of(A)));
        assertDoesNotThrow(() -> new EmDriftEpisode(1, T0, T1, T1, null, T1,
                EmDriftStatus.UNCONFIRMED, EmCloseReason.IDLE, 10, A, null, Set.of(A)));
    }

    @Test
    void firstContextMustBeIncludedAndSwitchCountMustBePositive() {
        assertThrows(IllegalArgumentException.class, () -> new EmDriftEpisode(1, T0, T1, T1, null, null,
                EmDriftStatus.OPEN, null, 10, A, null, Set.of(B)));
        assertThrows(IllegalArgumentException.class, () -> new EmDriftEpisode(1, T0, T1, T1, null, null,
                EmDriftStatus.OPEN, null, 0, A, null, Set.of(A)));
        assertDoesNotThrow(() -> new EmDriftEpisode(1, T0, T1, T1, null, null,
                EmDriftStatus.OPEN, null, 4, A, null, Set.of(A)));
    }

    @Test
    void persistedEventAlsoAcceptsSwitchCountBelowTen() {
        assertDoesNotThrow(() -> new EmDriftEvent(1, T0, T1, T1, null, null,
                EmDriftStatus.OPEN, null, 4, A, null, null, false, null, Set.of(A)));
        assertThrows(IllegalArgumentException.class, () -> new EmDriftEvent(1, T0, T1, T1, null, null,
                EmDriftStatus.OPEN, null, 0, A, null, null, false, null, Set.of(A)));
    }
}
