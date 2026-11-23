package com.deriva.coach;

import com.deriva.coach.model.CloseReason;
import com.deriva.coach.model.DriftEpisode;
import com.deriva.coach.model.DriftStatus;
import com.deriva.coach.model.DriftUpdate;
import com.deriva.privacy.ObservationKind;
import com.deriva.tracking.model.ContextKey;
import com.deriva.tracking.model.StableTick;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DriftStateMachineTest {
    private static final Instant BASE = Instant.parse("2026-11-22T10:00:00Z");

    private static Instant at(int second) {
        return BASE.plusSeconds(second);
    }

    private static StableTick observed(String process, int second, int since, boolean pending) {
        return new StableTick(at(second), ObservationKind.OBSERVED,
                new ContextKey(process, null), at(since), pending);
    }

    private static StableTick boundary(ObservationKind kind, int second) {
        return new StableTick(at(second), kind, null, at(second), false);
    }

    private static DriftUpdate openTenChanges(DriftStateMachine detector) {
        detector.onTick(observed("a.exe", 0, 0, false));
        DriftUpdate opened = null;
        for (int i = 1; i <= 10; i++) {
            String process = i % 2 == 1 ? "b.exe" : "a.exe";
            List<DriftUpdate> updates = detector.onTick(observed(process, i, i, false));
            if (!updates.isEmpty()) opened = updates.getLast();
        }
        assertNotNull(opened);
        return opened;
    }

    @Test
    void nineChangesDoNotOpenAndTenthOpensOneEpisode() {
        DriftStateMachine detector = new DriftStateMachine();
        detector.onTick(observed("a.exe", 0, 0, false));
        for (int i = 1; i <= 9; i++)
            assertTrue(detector.onTick(observed(i % 2 == 0 ? "a.exe" : "b.exe", i, i, false)).isEmpty());

        DriftUpdate opened = detector.onTick(observed("a.exe", 10, 10, false)).getFirst();
        assertEquals(DriftUpdate.Type.OPENED, opened.type());
        DriftEpisode episode = opened.episode();
        assertEquals(1, episode.id());
        assertEquals(DriftStatus.OPEN, episode.status());
        assertEquals(at(1), episode.startTs());
        assertEquals(at(10), episode.detectedAt());
        assertEquals(10, episode.switchCount());
        assertEquals(new ContextKey("b.exe", null), episode.firstChangeContext());
        assertEquals(2, episode.contexts().size());
    }

    @Test
    void windowUsesStrictlyNewerThan120Seconds() {
        DriftStateMachine detector = new DriftStateMachine();
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
        DriftStateMachine detector = new DriftStateMachine();
        DriftUpdate opened = openTenChanges(detector);
        long id = opened.episode().id();

        List<DriftUpdate> updates = detector.onTick(observed("c.exe", 11, 11, false));
        assertEquals(1, updates.size());
        assertEquals(DriftUpdate.Type.UPDATED, updates.getFirst().type());
        assertEquals(id, updates.getFirst().episode().id());
        assertEquals(11, updates.getFirst().episode().switchCount());
        assertTrue(updates.getFirst().episode().contexts().contains(new ContextKey("c.exe", null)));
    }

    @Test
    void stabilityClosesAt180SecondsWithoutClosingSegment() {
        DriftStateMachine detector = new DriftStateMachine();
        openTenChanges(detector);

        assertTrue(detector.onTick(observed("a.exe", 189, 10, false)).isEmpty());
        DriftUpdate closed = detector.onTick(observed("a.exe", 190, 10, false)).getFirst();
        assertEquals(DriftUpdate.Type.CLOSED, closed.type());
        assertEquals(DriftStatus.STABLE, closed.episode().status());
        assertEquals(at(10), closed.episode().stabilityStartTs());
        assertEquals(at(190), closed.episode().closedAt());
        assertEquals(9_000L, closed.episode().timeToStabilityMs());
    }

    @Test
    void pendingCandidateBlocksStability() {
        DriftStateMachine detector = new DriftStateMachine();
        openTenChanges(detector);

        assertTrue(detector.onTick(observed("a.exe", 190, 10, true)).isEmpty());
        DriftUpdate closed = detector.onTick(observed("a.exe", 191, 10, false)).getFirst();
        assertEquals(DriftStatus.STABLE, closed.episode().status());
        assertEquals(at(191), closed.episode().closedAt());
    }

    @Test
    void boundariesCloseUnconfirmedWithExplicitReason() {
        for (ObservationKind kind : List.of(ObservationKind.IDLE, ObservationKind.EXCLUDED,
                ObservationKind.UNAVAILABLE)) {
            DriftStateMachine detector = new DriftStateMachine();
            openTenChanges(detector);
            DriftUpdate closed = detector.onTick(boundary(kind, 20)).getFirst();
            assertEquals(DriftUpdate.Type.CLOSED, closed.type());
            assertEquals(DriftStatus.UNCONFIRMED, closed.episode().status());
            assertEquals(at(20), closed.episode().closedAt());
            assertEquals(at(10), closed.episode().lastObservedTs());
            assertNull(closed.episode().stabilityStartTs());
            assertNull(closed.episode().timeToStabilityMs());
            assertNotNull(closed.episode().closeReason());
        }
    }

    @Test
    void boundaryClearsThePendingChangeWindow() {
        DriftStateMachine detector = new DriftStateMachine();
        detector.onTick(observed("a.exe", 0, 0, false));
        for (int i = 1; i <= 9; i++)
            detector.onTick(observed(i % 2 == 0 ? "a.exe" : "b.exe", i, i, false));
        assertTrue(detector.onTick(boundary(ObservationKind.IDLE, 10)).isEmpty());

        detector.onTick(observed("a.exe", 11, 0, false));
        for (int i = 12; i <= 20; i++)
            assertTrue(detector.onTick(observed(i % 2 == 0 ? "b.exe" : "a.exe", i, i, false)).isEmpty());

        DriftUpdate opened = detector.onTick(observed("a.exe", 21, 21, false)).getFirst();
        assertEquals(DriftUpdate.Type.OPENED, opened.type());
        assertEquals(at(12), opened.episode().startTs());
    }

    @Test
    void closeOnExitMarksOpenEpisodeUnconfirmed() {
        DriftStateMachine detector = new DriftStateMachine();
        openTenChanges(detector);

        List<DriftUpdate> updates = detector.close(at(20), CloseReason.EXIT);
        assertEquals(1, updates.size());
        assertEquals(DriftUpdate.Type.CLOSED, updates.getFirst().type());
        assertEquals(DriftStatus.UNCONFIRMED, updates.getFirst().episode().status());
        assertEquals(CloseReason.EXIT, updates.getFirst().episode().closeReason());
        assertEquals(at(20), updates.getFirst().episode().closedAt());
        assertTrue(detector.close(at(21), CloseReason.EXIT).isEmpty());
    }

    @Test
    void closeOnPauseMarksOpenEpisodeUnconfirmedWithPauseReason() {
        DriftStateMachine detector = new DriftStateMachine();
        openTenChanges(detector);

        List<DriftUpdate> updates = detector.close(at(20), CloseReason.PAUSE);
        assertEquals(1, updates.size());
        assertEquals(DriftUpdate.Type.CLOSED, updates.getFirst().type());
        assertEquals(DriftStatus.UNCONFIRMED, updates.getFirst().episode().status());
        assertEquals(CloseReason.PAUSE, updates.getFirst().episode().closeReason());
        assertEquals(at(20), updates.getFirst().episode().closedAt());
        assertTrue(detector.close(at(21), CloseReason.PAUSE).isEmpty());
    }

    @Test
    void stableEpisodeDoesNotReuseOldChanges() {
        DriftStateMachine detector = new DriftStateMachine();
        openTenChanges(detector);
        detector.onTick(observed("a.exe", 190, 10, false));

        for (int i = 191; i <= 199; i++)
            assertTrue(detector.onTick(observed(i % 2 == 0 ? "a.exe" : "b.exe", i, i, false)).isEmpty());

        DriftUpdate opened = detector.onTick(observed("a.exe", 200, 200, false)).getFirst();
        assertEquals(DriftUpdate.Type.OPENED, opened.type());
        assertEquals(2, opened.episode().id());
        assertEquals(at(191), opened.episode().startTs());
    }
}
