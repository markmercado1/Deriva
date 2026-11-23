package com.deriva.analysis;

import com.deriva.privacy.ObservationKind;
import com.deriva.tracking.model.ContextKey;
import com.deriva.tracking.model.StableTick;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class ChangeCounterTest {
    private static final Instant BASE = Instant.parse("2026-11-22T10:00:00Z");

    private static Instant at(int second) {
        return BASE.plusSeconds(second);
    }

    private static StableTick observed(String process, int second, boolean pending) {
        return new StableTick(at(second), ObservationKind.OBSERVED,
                new ContextKey(process, null), at(second), pending);
    }

    private static StableTick boundary(ObservationKind kind, int second) {
        return new StableTick(at(second), kind, null, at(second), false);
    }

    @Test
    void firstObservedAndRepeatedContextAreNotChanges() {
        ChangeCounter counter = new ChangeCounter();
        assertTrue(counter.onTick(observed("a.exe", 0, false)).isEmpty());
        assertTrue(counter.onTick(observed("a.exe", 1, false)).isEmpty());
    }

    @Test
    void confirmedDifferentContextCountsOnce() {
        ChangeCounter counter = new ChangeCounter();
        counter.onTick(observed("a.exe", 0, false));
        Change change = counter.onTick(observed("b.exe", 1, false)).orElseThrow();
        assertEquals(at(1), change.timestamp());
        assertEquals(new ContextKey("b.exe", null), change.context());
        assertTrue(counter.onTick(observed("b.exe", 2, false)).isEmpty());
    }

    @Test
    void pendingCandidateDoesNotCountUntilConfirmed() {
        ChangeCounter counter = new ChangeCounter();
        counter.onTick(observed("a.exe", 0, false));
        assertTrue(counter.onTick(observed("b.exe", 1, true)).isEmpty());
        Change change = counter.onTick(observed("b.exe", 2, false)).orElseThrow();
        assertEquals(at(2), change.timestamp());
    }

    @Test
    void boundaryResetsContinuity() {
        ChangeCounter counter = new ChangeCounter();
        counter.onTick(observed("a.exe", 0, false));
        counter.onTick(boundary(ObservationKind.IDLE, 1));
        assertTrue(counter.onTick(observed("b.exe", 2, false)).isEmpty());
    }

    @Test
    void aToBToAcountsTwoChanges() {
        ChangeCounter counter = new ChangeCounter();
        counter.onTick(observed("a.exe", 0, false));
        assertTrue(counter.onTick(observed("b.exe", 1, false)).isPresent());
        assertTrue(counter.onTick(observed("a.exe", 2, false)).isPresent());
    }
}
