package com.deriva.analysis;

import com.deriva.privacy.EmObservationKind;
import com.deriva.tracking.model.EmContextKey;
import com.deriva.tracking.model.EmStableTick;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class EmChangeCounterTest {
    private static final Instant BASE = Instant.parse("2026-11-22T10:00:00Z");

    private static Instant at(int second) {
        return BASE.plusSeconds(second);
    }

    private static EmStableTick observed(String process, int second, boolean pending) {
        return new EmStableTick(at(second), EmObservationKind.OBSERVED,
                new EmContextKey(process, null), at(second), pending);
    }

    private static EmStableTick boundary(EmObservationKind kind, int second) {
        return new EmStableTick(at(second), kind, null, at(second), false);
    }

    @Test
    void firstObservedAndRepeatedContextAreNotChanges() {
        EmChangeCounter counter = new EmChangeCounter();
        assertTrue(counter.onTick(observed("a.exe", 0, false)).isEmpty());
        assertTrue(counter.onTick(observed("a.exe", 1, false)).isEmpty());
    }

    @Test
    void confirmedDifferentContextCountsOnce() {
        EmChangeCounter counter = new EmChangeCounter();
        counter.onTick(observed("a.exe", 0, false));
        EmChange change = counter.onTick(observed("b.exe", 1, false)).orElseThrow();
        assertEquals(at(1), change.timestamp());
        assertEquals(new EmContextKey("b.exe", null), change.context());
        assertTrue(counter.onTick(observed("b.exe", 2, false)).isEmpty());
    }

    @Test
    void pendingCandidateDoesNotCountUntilConfirmed() {
        EmChangeCounter counter = new EmChangeCounter();
        counter.onTick(observed("a.exe", 0, false));
        assertTrue(counter.onTick(observed("b.exe", 1, true)).isEmpty());
        EmChange change = counter.onTick(observed("b.exe", 2, false)).orElseThrow();
        assertEquals(at(2), change.timestamp());
    }

    @Test
    void boundaryResetsContinuity() {
        EmChangeCounter counter = new EmChangeCounter();
        counter.onTick(observed("a.exe", 0, false));
        counter.onTick(boundary(EmObservationKind.IDLE, 1));
        assertTrue(counter.onTick(observed("b.exe", 2, false)).isEmpty());
    }

    @Test
    void aToBToAcountsTwoChanges() {
        EmChangeCounter counter = new EmChangeCounter();
        counter.onTick(observed("a.exe", 0, false));
        assertTrue(counter.onTick(observed("b.exe", 1, false)).isPresent());
        assertTrue(counter.onTick(observed("a.exe", 2, false)).isPresent());
    }
}
