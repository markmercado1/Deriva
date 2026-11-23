package com.deriva.tracking;

import com.deriva.privacy.Observation;
import com.deriva.privacy.ObservationKind;
import com.deriva.tracking.model.Segment;
import com.deriva.tracking.model.TickResult;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SegmenterTest {
    private static final Instant BASE = Instant.parse("2026-11-22T10:00:00Z");

    private static Instant at(int second) {
        return BASE.plusSeconds(second);
    }

    private static Observation observed(String process, String host, int second) {
        return new Observation(at(second), ObservationKind.OBSERVED, process, host);
    }

    private static Observation boundary(ObservationKind kind, int second) {
        return new Observation(at(second), kind, null, null);
    }

    private static void confirmA(Segmenter segmenter) {
        segmenter.onObservation(observed("notepad.exe", null, 0));
        segmenter.onObservation(observed("notepad.exe", null, 1));
        segmenter.onObservation(observed("notepad.exe", null, 2));
        TickResult confirmed = segmenter.onObservation(observed("notepad.exe", null, 3));
        assertFalse(confirmed.tick().pendingChange());
        assertEquals("notepad.exe", confirmed.tick().context().processName());
        assertEquals(at(0), confirmed.tick().contextSince());
    }

    @Test
    void initialCandidateNeedsThreeObservedSecondsAndStartsAtFirstTick() {
        Segmenter segmenter = new Segmenter();
        assertTrue(segmenter.onObservation(observed("notepad.exe", null, 0)).tick().pendingChange());
        assertTrue(segmenter.onObservation(observed("notepad.exe", null, 1)).tick().pendingChange());
        assertTrue(segmenter.onObservation(observed("notepad.exe", null, 2)).tick().pendingChange());
        TickResult confirmed = segmenter.onObservation(observed("notepad.exe", null, 3));
        assertFalse(confirmed.tick().pendingChange());
        assertEquals(ObservationKind.OBSERVED, confirmed.tick().kind());
        assertEquals(at(0), confirmed.tick().contextSince());
        assertTrue(confirmed.completedSegments().isEmpty());

        TickResult idle = segmenter.onObservation(boundary(ObservationKind.IDLE, 4));
        assertEquals(List.of(new Segment(0, ObservationKind.OBSERVED,
                        new com.deriva.tracking.model.ContextKey("notepad.exe", null),
                        null, null, at(0), at(4))),
                idle.completedSegments());
    }

    @Test
    void twoSecondCandidateIsAbsorbed() {
        Segmenter segmenter = new Segmenter();
        confirmA(segmenter);
        segmenter.onObservation(observed("browser.exe", null, 4));
        segmenter.onObservation(observed("browser.exe", null, 5));
        TickResult back = segmenter.onObservation(observed("notepad.exe", null, 6));
        assertFalse(back.tick().pendingChange());
        assertEquals("notepad.exe", back.tick().context().processName());
        assertEquals(at(0), back.tick().contextSince());

        TickResult idle = segmenter.onObservation(boundary(ObservationKind.IDLE, 7));
        assertEquals(1, idle.completedSegments().size());
        assertEquals("notepad.exe", idle.completedSegments().getFirst().context().processName());
        assertEquals(at(7), idle.completedSegments().getFirst().end());
    }

    @Test
    void threeSecondCandidateIsConfirmedOnce() {
        Segmenter segmenter = new Segmenter();
        confirmA(segmenter);
        segmenter.onObservation(observed("browser.exe", null, 4));
        segmenter.onObservation(observed("browser.exe", null, 5));
        segmenter.onObservation(observed("browser.exe", null, 6));
        TickResult changed = segmenter.onObservation(observed("browser.exe", null, 7));
        assertFalse(changed.tick().pendingChange());
        assertEquals("browser.exe", changed.tick().context().processName());
        assertEquals(at(4), changed.tick().contextSince());
        assertEquals(1, changed.completedSegments().size());
        assertEquals(at(4), changed.completedSegments().getFirst().end());

        TickResult repeated = segmenter.onObservation(observed("browser.exe", null, 8));
        assertTrue(repeated.completedSegments().isEmpty());

        TickResult idle = segmenter.onObservation(boundary(ObservationKind.IDLE, 9));
        assertEquals(1, idle.completedSegments().size());
        assertEquals("browser.exe", idle.completedSegments().getFirst().context().processName());
        assertEquals(at(4), idle.completedSegments().getFirst().start());
        assertEquals(at(9), idle.completedSegments().getFirst().end());
    }

    @Test
    void aToBToAProducesTwoConfirmedChanges() {
        Segmenter segmenter = new Segmenter();
        for (int second = 0; second <= 3; second++)
            segmenter.onObservation(observed("notepad.exe", null, second));
        for (int second = 4; second <= 7; second++)
            segmenter.onObservation(observed("browser.exe", null, second));
        TickResult backToA = null;
        for (int second = 8; second <= 11; second++)
            backToA = segmenter.onObservation(observed("notepad.exe", null, second));

        assertNotNull(backToA);
        assertEquals("notepad.exe", backToA.tick().context().processName());
        assertEquals(at(8), backToA.tick().contextSince());
        assertEquals(1, backToA.completedSegments().size());
        assertEquals("browser.exe", backToA.completedSegments().getFirst().context().processName());
        assertEquals(at(4), backToA.completedSegments().getFirst().start());
        assertEquals(at(8), backToA.completedSegments().getFirst().end());

        TickResult idle = segmenter.onObservation(boundary(ObservationKind.IDLE, 12));
        assertEquals(1, idle.completedSegments().size());
        assertEquals("notepad.exe", idle.completedSegments().getFirst().context().processName());
        assertEquals(at(8), idle.completedSegments().getFirst().start());
        assertEquals(at(12), idle.completedSegments().getFirst().end());
    }

    @Test
    void boundaryIsImmediateAndBreaksContinuity() {
        Segmenter segmenter = new Segmenter();
        confirmA(segmenter);
        TickResult idle = segmenter.onObservation(boundary(ObservationKind.IDLE, 4));
        assertEquals(ObservationKind.IDLE, idle.tick().kind());
        assertEquals(at(4), idle.tick().contextSince());
        assertFalse(idle.tick().pendingChange());
        assertEquals(1, idle.completedSegments().size());

        TickResult stillIdle = segmenter.onObservation(boundary(ObservationKind.IDLE, 5));
        assertTrue(stillIdle.completedSegments().isEmpty());
        assertEquals(at(4), stillIdle.tick().contextSince());

        TickResult excluded = segmenter.onObservation(boundary(ObservationKind.EXCLUDED, 6));
        assertEquals(ObservationKind.EXCLUDED, excluded.tick().kind());
        assertEquals(at(6), excluded.tick().contextSince());
        assertEquals(1, excluded.completedSegments().size());
        assertEquals(ObservationKind.IDLE, excluded.completedSegments().getFirst().kind());
        assertEquals(at(4), excluded.completedSegments().getFirst().start());
        assertEquals(at(6), excluded.completedSegments().getFirst().end());
    }

    @Test
    void unavailableBoundaryIsImmediateAndCarriesNoIdentity() {
        Segmenter segmenter = new Segmenter();
        confirmA(segmenter);
        TickResult unavailable = segmenter.onObservation(boundary(ObservationKind.UNAVAILABLE, 4));
        assertEquals(ObservationKind.UNAVAILABLE, unavailable.tick().kind());
        assertNull(unavailable.tick().context());
        assertFalse(unavailable.tick().pendingChange());
        assertEquals(1, unavailable.completedSegments().size());
        assertEquals(ObservationKind.OBSERVED, unavailable.completedSegments().getFirst().kind());
    }

    @Test
    void candidateBeforeBoundaryIsAbsorbedIntoCurrentSegment() {
        Segmenter segmenter = new Segmenter();
        confirmA(segmenter);
        segmenter.onObservation(observed("browser.exe", null, 4));
        segmenter.onObservation(observed("browser.exe", null, 5));
        TickResult idle = segmenter.onObservation(boundary(ObservationKind.IDLE, 6));
        assertEquals(1, idle.completedSegments().size());
        Segment closed = idle.completedSegments().getFirst();
        assertEquals("notepad.exe", closed.context().processName());
        assertEquals(at(0), closed.start());
        assertEquals(at(6), closed.end());
    }

    @Test
    void returningFromBoundaryRequiresThreeObservedSeconds() {
        Segmenter segmenter = new Segmenter();
        segmenter.onObservation(boundary(ObservationKind.IDLE, 0));
        segmenter.onObservation(boundary(ObservationKind.IDLE, 1));
        segmenter.onObservation(observed("notepad.exe", null, 2));
        segmenter.onObservation(observed("notepad.exe", null, 3));
        assertTrue(segmenter.onObservation(observed("notepad.exe", null, 4)).tick().pendingChange());
        TickResult confirmed = segmenter.onObservation(observed("notepad.exe", null, 5));
        assertFalse(confirmed.tick().pendingChange());
        assertEquals(ObservationKind.OBSERVED, confirmed.tick().kind());
        assertEquals(at(2), confirmed.tick().contextSince());
        assertEquals(1, confirmed.completedSegments().size());
        assertEquals(ObservationKind.IDLE, confirmed.completedSegments().getFirst().kind());
        assertEquals(at(0), confirmed.completedSegments().getFirst().start());
        assertEquals(at(2), confirmed.completedSegments().getFirst().end());
    }

    @Test
    void hostAndProcessArePartOfContextIdentity() {
        Segmenter segmenter = new Segmenter();
        for (int second = 0; second <= 3; second++)
            segmenter.onObservation(observed("chrome.exe", "docs.google.com", second));
        for (int second = 4; second <= 6; second++)
            segmenter.onObservation(observed("chrome.exe", "mail.google.com", second));
        TickResult secondHost = segmenter.onObservation(observed("chrome.exe", "mail.google.com", 7));
        assertEquals("mail.google.com", secondHost.tick().context().siteHost());
        assertEquals(1, secondHost.completedSegments().size());
        assertEquals("docs.google.com", secondHost.completedSegments().getFirst().context().siteHost());

        for (int second = 8; second <= 10; second++)
            segmenter.onObservation(observed("msedge.exe", "mail.google.com", second));
        TickResult otherProcess = segmenter.onObservation(observed("msedge.exe", "mail.google.com", 11));
        assertEquals("msedge.exe", otherProcess.tick().context().processName());
        assertEquals(1, otherProcess.completedSegments().size());
        assertEquals("chrome.exe", otherProcess.completedSegments().getFirst().context().processName());
    }

    @Test
    void gapClosesAtLastValidTickAndLeavesAHole() {
        Segmenter segmenter = new Segmenter();
        confirmA(segmenter);
        TickResult afterGap = segmenter.onObservation(observed("notepad.exe", null, 8));
        assertTrue(afterGap.tick().pendingChange());
        assertEquals(1, afterGap.completedSegments().size());
        assertEquals(at(0), afterGap.completedSegments().getFirst().start());
        assertEquals(at(3), afterGap.completedSegments().getFirst().end());

        segmenter.onObservation(observed("notepad.exe", null, 9));
        segmenter.onObservation(observed("notepad.exe", null, 10));
        TickResult confirmed = segmenter.onObservation(observed("notepad.exe", null, 11));
        assertFalse(confirmed.tick().pendingChange());
        assertEquals(at(8), confirmed.tick().contextSince());

        TickResult idle = segmenter.onObservation(boundary(ObservationKind.IDLE, 12));
        assertEquals(1, idle.completedSegments().size());
        assertEquals(at(8), idle.completedSegments().getFirst().start());
        assertEquals(at(12), idle.completedSegments().getFirst().end());
    }

    @Test
    void snapshotExposesOpenAndPendingWithoutClosing() {
        Segmenter segmenter = new Segmenter();
        confirmA(segmenter);

        Segmenter.Snapshot open = segmenter.snapshot(at(4));
        assertEquals(at(3), open.at());
        assertEquals(at(0), open.open().start());
        assertEquals(at(3), open.open().end());
        assertNull(open.pending());

        segmenter.onObservation(observed("browser.exe", null, 4));
        segmenter.onObservation(observed("browser.exe", null, 5));
        Segmenter.Snapshot mixed = segmenter.snapshot(at(6));
        assertEquals(at(5), mixed.at());
        assertEquals("notepad.exe", mixed.open().context().processName());
        assertEquals(at(0), mixed.open().start());
        assertEquals(at(5), mixed.open().end());
        assertEquals("browser.exe", mixed.pending().context().processName());
        assertEquals(at(4), mixed.pending().start());
        assertEquals(at(5), mixed.pending().end());

        TickResult confirmed = segmenter.onObservation(observed("browser.exe", null, 7));
        assertEquals(1, confirmed.completedSegments().size());
        assertEquals(at(0), confirmed.completedSegments().getFirst().start());
        assertEquals(at(4), confirmed.completedSegments().getFirst().end());
        Segmenter.Snapshot after = segmenter.snapshot(at(8));
        assertEquals("browser.exe", after.open().context().processName());
        assertEquals(at(4), after.open().start());
        assertEquals(at(7), after.open().end());
        assertNull(after.pending());
    }

    @Test
    void closeFlushesOpenObservedSegmentAndResets() {
        Segmenter segmenter = new Segmenter();
        confirmA(segmenter);

        List<Segment> completed = segmenter.close(at(4));
        assertEquals(1, completed.size());
        assertEquals(ObservationKind.OBSERVED, completed.getFirst().kind());
        assertEquals(at(0), completed.getFirst().start());
        assertEquals(at(4), completed.getFirst().end());
        assertTrue(segmenter.close(at(5)).isEmpty());

        assertTrue(segmenter.onObservation(observed("notepad.exe", null, 6)).tick().pendingChange());
        List<Segment> pendingOnly = segmenter.close(at(7));
        assertEquals(1, pendingOnly.size());
        assertEquals(ObservationKind.OBSERVED, pendingOnly.getFirst().kind());
        assertEquals("notepad.exe", pendingOnly.getFirst().context().processName());
        assertEquals(at(6), pendingOnly.getFirst().start());
        assertEquals(at(7), pendingOnly.getFirst().end());
    }

    @Test
    void closeFlushesPendingCandidateAfterOpenSegmentWithoutOverlap() {
        Segmenter segmenter = new Segmenter();
        confirmA(segmenter);
        segmenter.onObservation(observed("browser.exe", null, 4));
        segmenter.onObservation(observed("browser.exe", null, 5));

        List<Segment> completed = segmenter.close(at(6));
        assertEquals(2, completed.size());
        assertEquals("notepad.exe", completed.get(0).context().processName());
        assertEquals(at(0), completed.get(0).start());
        assertEquals(at(4), completed.get(0).end());
        assertEquals("browser.exe", completed.get(1).context().processName());
        assertEquals(at(4), completed.get(1).start());
        assertEquals(at(6), completed.get(1).end());
        assertFalse(completed.get(0).end().isAfter(completed.get(1).start()));
    }

    @Test
    void closeFlushesPendingCandidateWithoutOpenSegment() {
        Segmenter segmenter = new Segmenter();
        segmenter.onObservation(observed("notepad.exe", null, 0));
        segmenter.onObservation(observed("notepad.exe", null, 1));

        List<Segment> completed = segmenter.close(at(2));
        assertEquals(1, completed.size());
        assertEquals(ObservationKind.OBSERVED, completed.getFirst().kind());
        assertEquals("notepad.exe", completed.getFirst().context().processName());
        assertEquals(at(0), completed.getFirst().start());
        assertEquals(at(2), completed.getFirst().end());
    }

    @Test
    void closeFlushesOpenBoundarySegment() {
        Segmenter segmenter = new Segmenter();
        segmenter.onObservation(boundary(ObservationKind.IDLE, 0));
        segmenter.onObservation(boundary(ObservationKind.IDLE, 1));

        List<Segment> completed = segmenter.close(at(2));
        assertEquals(1, completed.size());
        assertEquals(ObservationKind.IDLE, completed.getFirst().kind());
        assertNull(completed.getFirst().context());
        assertEquals(at(0), completed.getFirst().start());
        assertEquals(at(2), completed.getFirst().end());
    }

    @Test
    void clockBackwardsClosesWithoutNegativeDuration() {
        Segmenter segmenter = new Segmenter();
        confirmA(segmenter);
        TickResult backwards = segmenter.onObservation(observed("notepad.exe", null, 2));
        assertEquals(ObservationKind.UNAVAILABLE, backwards.tick().kind());
        assertEquals(at(2), backwards.tick().contextSince());
        assertEquals(1, backwards.completedSegments().size());
        Segment closed = backwards.completedSegments().getFirst();
        assertFalse(closed.end().isBefore(closed.start()));
        assertEquals(at(3), closed.end());
    }
}
