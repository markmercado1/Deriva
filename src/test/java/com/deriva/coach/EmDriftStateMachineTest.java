package com.deriva.coach;

import com.deriva.coach.model.EmCloseReason;
import com.deriva.coach.model.EmDriftEpisode;
import com.deriva.coach.model.EmDriftStatus;
import com.deriva.coach.model.EmDriftUpdate;
import com.deriva.privacy.EmObservationKind;
import com.deriva.tracking.model.EmContextKey;
import com.deriva.tracking.model.EmStableTick;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class EmDriftStateMachineTest {
    private static final Instant BASE = Instant.parse("2026-11-22T10:00:00Z");

    private static Instant at(int second) {
        return BASE.plusSeconds(second);
    }

    private static EmStableTick observed(String process, int second, int since, boolean pending) {
        return new EmStableTick(at(second), EmObservationKind.OBSERVED,
                new EmContextKey(process, null), at(since), pending);
    }

    private static EmStableTick boundary(EmObservationKind kind, int second) {
        return new EmStableTick(at(second), kind, null, at(second), false);
    }

    private static EmDriftUpdate openTenChanges(EmDriftStateMachine detector) {
        detector.onTick(observed("a.exe", 0, 0, false));
        EmDriftUpdate opened = null;
        for (int i = 1; i <= 10; i++) {
            String process = i % 2 == 1 ? "b.exe" : "a.exe";
            List<EmDriftUpdate> updates = detector.onTick(observed(process, i, i, false));
            if (!updates.isEmpty()) opened = updates.getLast();
        }
        assertNotNull(opened);
        return opened;
    }

    @Test
    void nineChangesDoNotOpenAndTenthOpensOneEpisode() {
        EmDriftStateMachine detector = new EmDriftStateMachine();
        detector.onTick(observed("a.exe", 0, 0, false));
        for (int i = 1; i <= 9; i++)
            assertTrue(detector.onTick(observed(i % 2 == 0 ? "a.exe" : "b.exe", i, i, false)).isEmpty());

        EmDriftUpdate opened = detector.onTick(observed("a.exe", 10, 10, false)).getFirst();
        assertEquals(EmDriftUpdate.Type.OPENED, opened.type());
        EmDriftEpisode episode = opened.episode();
        assertEquals(1, episode.id());
        assertEquals(EmDriftStatus.OPEN, episode.status());
        assertEquals(at(1), episode.startTs());
        assertEquals(at(10), episode.detectedAt());
        assertEquals(10, episode.switchCount());
        assertEquals(new EmContextKey("b.exe", null), episode.firstChangeContext());
        assertEquals(2, episode.contexts().size());
    }

    @Test
    void windowUsesStrictlyNewerThan120Seconds() {
        EmDriftStateMachine detector = new EmDriftStateMachine();
        detector.onTick(observed("a.exe", 0, 0, false));
        detector.onTick(observed("a.exe", 1, 0, false));
        for (int i = 2; i <= 10; i++)
            detector.onTick(observed(i % 2 == 0 ? "b.exe" : "a.exe", i, i, false));
        for (int i = 11; i <= 121; i++)
            detector.onTick(observed("b.exe", i, 10, false));

        assertTrue(detector.onTick(observed("a.exe", 122, 122, false)).isEmpty(),
                "a change exactly 120 s old must not count");
    }

    @Test
    void laterChangesUpdateSameEpisode() {
        EmDriftStateMachine detector = new EmDriftStateMachine();
        EmDriftUpdate opened = openTenChanges(detector);
        long id = opened.episode().id();

        List<EmDriftUpdate> updates = detector.onTick(observed("c.exe", 11, 11, false));
        assertEquals(1, updates.size());
        assertEquals(EmDriftUpdate.Type.UPDATED, updates.getFirst().type());
        assertEquals(id, updates.getFirst().episode().id());
        assertEquals(11, updates.getFirst().episode().switchCount());
        assertTrue(updates.getFirst().episode().contexts().contains(new EmContextKey("c.exe", null)));
    }

    @Test
    void stabilityClosesAt180SecondsWithoutClosingSegment() {
        EmDriftStateMachine detector = new EmDriftStateMachine();
        openTenChanges(detector);

        assertTrue(detector.onTick(observed("a.exe", 189, 10, false)).isEmpty());
        EmDriftUpdate closed = detector.onTick(observed("a.exe", 190, 10, false)).getFirst();
        assertEquals(EmDriftUpdate.Type.CLOSED, closed.type());
        assertEquals(EmDriftStatus.STABLE, closed.episode().status());
        assertEquals(at(10), closed.episode().stabilityStartTs());
        assertEquals(at(190), closed.episode().closedAt());
        assertEquals(9_000L, closed.episode().timeToStabilityMs());
    }

    @Test
    void pendingCandidateBlocksStability() {
        EmDriftStateMachine detector = new EmDriftStateMachine();
        openTenChanges(detector);

        assertTrue(detector.onTick(observed("a.exe", 190, 10, true)).isEmpty());
        EmDriftUpdate closed = detector.onTick(observed("a.exe", 191, 10, false)).getFirst();
        assertEquals(EmDriftStatus.STABLE, closed.episode().status());
        assertEquals(at(191), closed.episode().closedAt());
    }

    @Test
    void boundariesCloseUnconfirmedWithExplicitReason() {
        for (EmObservationKind kind : List.of(EmObservationKind.IDLE, EmObservationKind.EXCLUDED,
                EmObservationKind.UNAVAILABLE)) {
            EmDriftStateMachine detector = new EmDriftStateMachine();
            openTenChanges(detector);
            EmDriftUpdate closed = detector.onTick(boundary(kind, 20)).getFirst();
            assertEquals(EmDriftUpdate.Type.CLOSED, closed.type());
            assertEquals(EmDriftStatus.UNCONFIRMED, closed.episode().status());
            assertEquals(at(20), closed.episode().closedAt());
            assertEquals(at(10), closed.episode().lastObservedTs());
            assertNull(closed.episode().stabilityStartTs());
            assertNull(closed.episode().timeToStabilityMs());
            assertNotNull(closed.episode().closeReason());
        }
    }

    @Test
    void boundaryClearsThePendingChangeWindow() {
        EmDriftStateMachine detector = new EmDriftStateMachine();
        detector.onTick(observed("a.exe", 0, 0, false));
        for (int i = 1; i <= 9; i++)
            detector.onTick(observed(i % 2 == 0 ? "a.exe" : "b.exe", i, i, false));
        assertTrue(detector.onTick(boundary(EmObservationKind.IDLE, 10)).isEmpty());

        detector.onTick(observed("a.exe", 11, 0, false));
        for (int i = 12; i <= 20; i++)
            assertTrue(detector.onTick(observed(i % 2 == 0 ? "b.exe" : "a.exe", i, i, false)).isEmpty());

        EmDriftUpdate opened = detector.onTick(observed("a.exe", 21, 21, false)).getFirst();
        assertEquals(EmDriftUpdate.Type.OPENED, opened.type());
        assertEquals(at(12), opened.episode().startTs());
    }

    @Test
    void closeOnExitMarksOpenEpisodeUnconfirmed() {
        EmDriftStateMachine detector = new EmDriftStateMachine();
        openTenChanges(detector);

        List<EmDriftUpdate> updates = detector.close(at(20), EmCloseReason.EXIT);
        assertEquals(1, updates.size());
        assertEquals(EmDriftUpdate.Type.CLOSED, updates.getFirst().type());
        assertEquals(EmDriftStatus.UNCONFIRMED, updates.getFirst().episode().status());
        assertEquals(EmCloseReason.EXIT, updates.getFirst().episode().closeReason());
        assertEquals(at(20), updates.getFirst().episode().closedAt());
        assertTrue(detector.close(at(21), EmCloseReason.EXIT).isEmpty());
    }

    @Test
    void closeOnPauseMarksOpenEpisodeUnconfirmedWithPauseReason() {
        EmDriftStateMachine detector = new EmDriftStateMachine();
        openTenChanges(detector);

        List<EmDriftUpdate> updates = detector.close(at(20), EmCloseReason.PAUSE);
        assertEquals(1, updates.size());
        assertEquals(EmDriftUpdate.Type.CLOSED, updates.getFirst().type());
        assertEquals(EmDriftStatus.UNCONFIRMED, updates.getFirst().episode().status());
        assertEquals(EmCloseReason.PAUSE, updates.getFirst().episode().closeReason());
        assertEquals(at(20), updates.getFirst().episode().closedAt());
        assertTrue(detector.close(at(21), EmCloseReason.PAUSE).isEmpty());
    }

    @Test
    void stableEpisodeDoesNotReuseOldChanges() {
        EmDriftStateMachine detector = new EmDriftStateMachine();
        openTenChanges(detector);
        detector.onTick(observed("a.exe", 190, 10, false));

        for (int i = 191; i <= 199; i++)
            assertTrue(detector.onTick(observed(i % 2 == 0 ? "a.exe" : "b.exe", i, i, false)).isEmpty());

        EmDriftUpdate opened = detector.onTick(observed("a.exe", 200, 200, false)).getFirst();
        assertEquals(EmDriftUpdate.Type.OPENED, opened.type());
        assertEquals(2, opened.episode().id());
        assertEquals(at(191), opened.episode().startTs());
    }
}
