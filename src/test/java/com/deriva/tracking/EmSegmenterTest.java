package com.deriva.tracking;

import com.deriva.privacy.EmObservation;
import com.deriva.privacy.EmObservationKind;
import com.deriva.tracking.model.EmSegment;
import com.deriva.tracking.model.EmTickResult;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class EmSegmenterTest {
    private static final Instant BASE = Instant.parse("2026-11-22T10:00:00Z");

    private static Instant at(int second) {
        return BASE.plusSeconds(second);
    }

    private static EmObservation observed(String process, String host, int second) {
        return new EmObservation(at(second), EmObservationKind.OBSERVED, process, host);
    }

    private static EmObservation boundary(EmObservationKind kind, int second) {
        return new EmObservation(at(second), kind, null, null);
    }

    private static void confirmA(EmSegmenter segmenter) {
        segmenter.onObservation(observed("notepad.exe", null, 0));
        segmenter.onObservation(observed("notepad.exe", null, 1));
        segmenter.onObservation(observed("notepad.exe", null, 2));
        EmTickResult confirmed = segmenter.onObservation(observed("notepad.exe", null, 3));
        assertFalse(confirmed.tick().pendingChange());
        assertEquals("notepad.exe", confirmed.tick().context().processName());
        assertEquals(at(0), confirmed.tick().contextSince());
    }

    @Test
    void initialCandidateNeedsThreeObservedSecondsAndStartsAtFirstTick() {
        EmSegmenter segmenter = new EmSegmenter();
        assertTrue(segmenter.onObservation(observed("notepad.exe", null, 0)).tick().pendingChange());
        assertTrue(segmenter.onObservation(observed("notepad.exe", null, 1)).tick().pendingChange());
        assertTrue(segmenter.onObservation(observed("notepad.exe", null, 2)).tick().pendingChange());
        EmTickResult confirmed = segmenter.onObservation(observed("notepad.exe", null, 3));
        assertFalse(confirmed.tick().pendingChange());
        assertEquals(EmObservationKind.OBSERVED, confirmed.tick().kind());
        assertEquals(at(0), confirmed.tick().contextSince());
        assertTrue(confirmed.completedSegments().isEmpty());

        EmTickResult idle = segmenter.onObservation(boundary(EmObservationKind.IDLE, 4));
        assertEquals(List.of(new EmSegment(0, EmObservationKind.OBSERVED,
                        new com.deriva.tracking.model.EmContextKey("notepad.exe", null),
                        null, null, at(0), at(4))),
                idle.completedSegments());
    }

    @Test
    void twoSecondCandidateIsAbsorbed() {
        EmSegmenter segmenter = new EmSegmenter();
        confirmA(segmenter);
        segmenter.onObservation(observed("browser.exe", null, 4));
        segmenter.onObservation(observed("browser.exe", null, 5));
        EmTickResult back = segmenter.onObservation(observed("notepad.exe", null, 6));
        assertFalse(back.tick().pendingChange());
        assertEquals("notepad.exe", back.tick().context().processName());
        assertEquals(at(0), back.tick().contextSince());

        EmTickResult idle = segmenter.onObservation(boundary(EmObservationKind.IDLE, 7));
        assertEquals(1, idle.completedSegments().size());
        assertEquals("notepad.exe", idle.completedSegments().getFirst().context().processName());
        assertEquals(at(7), idle.completedSegments().getFirst().end());
    }

    @Test
    void threeSecondCandidateIsConfirmedOnce() {
        EmSegmenter segmenter = new EmSegmenter();
        confirmA(segmenter);
        segmenter.onObservation(observed("browser.exe", null, 4));
        segmenter.onObservation(observed("browser.exe", null, 5));
        segmenter.onObservation(observed("browser.exe", null, 6));
        EmTickResult changed = segmenter.onObservation(observed("browser.exe", null, 7));
        assertFalse(changed.tick().pendingChange());
        assertEquals("browser.exe", changed.tick().context().processName());
        assertEquals(at(4), changed.tick().contextSince());
        assertEquals(1, changed.completedSegments().size());
        assertEquals(at(4), changed.completedSegments().getFirst().end());

        EmTickResult repeated = segmenter.onObservation(observed("browser.exe", null, 8));
        assertTrue(repeated.completedSegments().isEmpty());

        EmTickResult idle = segmenter.onObservation(boundary(EmObservationKind.IDLE, 9));
        assertEquals(1, idle.completedSegments().size());
        assertEquals("browser.exe", idle.completedSegments().getFirst().context().processName());
        assertEquals(at(4), idle.completedSegments().getFirst().start());
        assertEquals(at(9), idle.completedSegments().getFirst().end());
    }

    @Test
    void aToBToAProducesTwoConfirmedChanges() {
        EmSegmenter segmenter = new EmSegmenter();
        for (int second = 0; second <= 3; second++)
            segmenter.onObservation(observed("notepad.exe", null, second));
        for (int second = 4; second <= 7; second++)
            segmenter.onObservation(observed("browser.exe", null, second));
        EmTickResult backToA = null;
        for (int second = 8; second <= 11; second++)
            backToA = segmenter.onObservation(observed("notepad.exe", null, second));

        assertNotNull(backToA);
        assertEquals("notepad.exe", backToA.tick().context().processName());
        assertEquals(at(8), backToA.tick().contextSince());
        assertEquals(1, backToA.completedSegments().size());
        assertEquals("browser.exe", backToA.completedSegments().getFirst().context().processName());
        assertEquals(at(4), backToA.completedSegments().getFirst().start());
        assertEquals(at(8), backToA.completedSegments().getFirst().end());

        EmTickResult idle = segmenter.onObservation(boundary(EmObservationKind.IDLE, 12));
        assertEquals(1, idle.completedSegments().size());
        assertEquals("notepad.exe", idle.completedSegments().getFirst().context().processName());
        assertEquals(at(8), idle.completedSegments().getFirst().start());
        assertEquals(at(12), idle.completedSegments().getFirst().end());
    }

    @Test
    void boundaryIsImmediateAndBreaksContinuity() {
        EmSegmenter segmenter = new EmSegmenter();
        confirmA(segmenter);
        EmTickResult idle = segmenter.onObservation(boundary(EmObservationKind.IDLE, 4));
        assertEquals(EmObservationKind.IDLE, idle.tick().kind());
        assertEquals(at(4), idle.tick().contextSince());
        assertFalse(idle.tick().pendingChange());
        assertEquals(1, idle.completedSegments().size());

        EmTickResult stillIdle = segmenter.onObservation(boundary(EmObservationKind.IDLE, 5));
        assertTrue(stillIdle.completedSegments().isEmpty());
        assertEquals(at(4), stillIdle.tick().contextSince());

        EmTickResult excluded = segmenter.onObservation(boundary(EmObservationKind.EXCLUDED, 6));
        assertEquals(EmObservationKind.EXCLUDED, excluded.tick().kind());
        assertEquals(at(6), excluded.tick().contextSince());
        assertEquals(1, excluded.completedSegments().size());
        assertEquals(EmObservationKind.IDLE, excluded.completedSegments().getFirst().kind());
        assertEquals(at(4), excluded.completedSegments().getFirst().start());
        assertEquals(at(6), excluded.completedSegments().getFirst().end());
    }

    @Test
    void unavailableBoundaryIsImmediateAndCarriesNoIdentity() {
        EmSegmenter segmenter = new EmSegmenter();
        confirmA(segmenter);
        EmTickResult unavailable = segmenter.onObservation(boundary(EmObservationKind.UNAVAILABLE, 4));
        assertEquals(EmObservationKind.UNAVAILABLE, unavailable.tick().kind());
        assertNull(unavailable.tick().context());
        assertFalse(unavailable.tick().pendingChange());
        assertEquals(1, unavailable.completedSegments().size());
        assertEquals(EmObservationKind.OBSERVED, unavailable.completedSegments().getFirst().kind());
    }

    @Test
    void candidateBeforeBoundaryIsAbsorbedIntoCurrentSegment() {
        EmSegmenter segmenter = new EmSegmenter();
        confirmA(segmenter);
        segmenter.onObservation(observed("browser.exe", null, 4));
        segmenter.onObservation(observed("browser.exe", null, 5));
        EmTickResult idle = segmenter.onObservation(boundary(EmObservationKind.IDLE, 6));
        assertEquals(1, idle.completedSegments().size());
        EmSegment closed = idle.completedSegments().getFirst();
        assertEquals("notepad.exe", closed.context().processName());
        assertEquals(at(0), closed.start());
        assertEquals(at(6), closed.end());
    }

    @Test
    void returningFromBoundaryRequiresThreeObservedSeconds() {
        EmSegmenter segmenter = new EmSegmenter();
        segmenter.onObservation(boundary(EmObservationKind.IDLE, 0));
        segmenter.onObservation(boundary(EmObservationKind.IDLE, 1));
        segmenter.onObservation(observed("notepad.exe", null, 2));
        segmenter.onObservation(observed("notepad.exe", null, 3));
        assertTrue(segmenter.onObservation(observed("notepad.exe", null, 4)).tick().pendingChange());
        EmTickResult confirmed = segmenter.onObservation(observed("notepad.exe", null, 5));
        assertFalse(confirmed.tick().pendingChange());
        assertEquals(EmObservationKind.OBSERVED, confirmed.tick().kind());
        assertEquals(at(2), confirmed.tick().contextSince());
        assertEquals(1, confirmed.completedSegments().size());
        assertEquals(EmObservationKind.IDLE, confirmed.completedSegments().getFirst().kind());
        assertEquals(at(0), confirmed.completedSegments().getFirst().start());
        assertEquals(at(2), confirmed.completedSegments().getFirst().end());
    }

    @Test
    void hostAndProcessArePartOfContextIdentity() {
        EmSegmenter segmenter = new EmSegmenter();
        for (int second = 0; second <= 3; second++)
            segmenter.onObservation(observed("chrome.exe", "docs.google.com", second));
        for (int second = 4; second <= 6; second++)
            segmenter.onObservation(observed("chrome.exe", "mail.google.com", second));
        EmTickResult secondHost = segmenter.onObservation(observed("chrome.exe", "mail.google.com", 7));
        assertEquals("mail.google.com", secondHost.tick().context().siteHost());
        assertEquals(1, secondHost.completedSegments().size());
        assertEquals("docs.google.com", secondHost.completedSegments().getFirst().context().siteHost());

        for (int second = 8; second <= 10; second++)
            segmenter.onObservation(observed("msedge.exe", "mail.google.com", second));
        EmTickResult otherProcess = segmenter.onObservation(observed("msedge.exe", "mail.google.com", 11));
        assertEquals("msedge.exe", otherProcess.tick().context().processName());
        assertEquals(1, otherProcess.completedSegments().size());
        assertEquals("chrome.exe", otherProcess.completedSegments().getFirst().context().processName());
    }

    @Test
    void gapClosesAtLastValidTickAndLeavesAHole() {
        EmSegmenter segmenter = new EmSegmenter();
        confirmA(segmenter);
        EmTickResult afterGap = segmenter.onObservation(observed("notepad.exe", null, 8));
        assertTrue(afterGap.tick().pendingChange());
        assertEquals(1, afterGap.completedSegments().size());
        assertEquals(at(0), afterGap.completedSegments().getFirst().start());
        assertEquals(at(3), afterGap.completedSegments().getFirst().end());

        segmenter.onObservation(observed("notepad.exe", null, 9));
        segmenter.onObservation(observed("notepad.exe", null, 10));
        EmTickResult confirmed = segmenter.onObservation(observed("notepad.exe", null, 11));
        assertFalse(confirmed.tick().pendingChange());
        assertEquals(at(8), confirmed.tick().contextSince());

        EmTickResult idle = segmenter.onObservation(boundary(EmObservationKind.IDLE, 12));
        assertEquals(1, idle.completedSegments().size());
        assertEquals(at(8), idle.completedSegments().getFirst().start());
        assertEquals(at(12), idle.completedSegments().getFirst().end());
    }

    @Test
    void snapshotExposesOpenAndPendingWithoutClosing() {
        EmSegmenter segmenter = new EmSegmenter();
        confirmA(segmenter);

        EmSegmenter.Snapshot open = segmenter.snapshot(at(4));
        assertEquals(at(3), open.at());
        assertEquals(at(0), open.open().start());
        assertEquals(at(3), open.open().end());
        assertNull(open.pending());

        segmenter.onObservation(observed("browser.exe", null, 4));
        segmenter.onObservation(observed("browser.exe", null, 5));
        EmSegmenter.Snapshot mixed = segmenter.snapshot(at(6));
        assertEquals(at(5), mixed.at());
        assertEquals("notepad.exe", mixed.open().context().processName());
        assertEquals(at(0), mixed.open().start());
        assertEquals(at(5), mixed.open().end());
        assertEquals("browser.exe", mixed.pending().context().processName());
        assertEquals(at(4), mixed.pending().start());
        assertEquals(at(5), mixed.pending().end());

        EmTickResult confirmed = segmenter.onObservation(observed("browser.exe", null, 7));
        assertEquals(1, confirmed.completedSegments().size());
        assertEquals(at(0), confirmed.completedSegments().getFirst().start());
        assertEquals(at(4), confirmed.completedSegments().getFirst().end());
        EmSegmenter.Snapshot after = segmenter.snapshot(at(8));
        assertEquals("browser.exe", after.open().context().processName());
        assertEquals(at(4), after.open().start());
        assertEquals(at(7), after.open().end());
        assertNull(after.pending());
    }

    @Test
    void closeFlushesOpenObservedSegmentAndResets() {
        EmSegmenter segmenter = new EmSegmenter();
        confirmA(segmenter);

        List<EmSegment> completed = segmenter.close(at(4));
        assertEquals(1, completed.size());
        assertEquals(EmObservationKind.OBSERVED, completed.getFirst().kind());
        assertEquals(at(0), completed.getFirst().start());
        assertEquals(at(4), completed.getFirst().end());
        assertTrue(segmenter.close(at(5)).isEmpty());

        assertTrue(segmenter.onObservation(observed("notepad.exe", null, 6)).tick().pendingChange());
        List<EmSegment> pendingOnly = segmenter.close(at(7));
        assertEquals(1, pendingOnly.size());
        assertEquals(EmObservationKind.OBSERVED, pendingOnly.getFirst().kind());
        assertEquals("notepad.exe", pendingOnly.getFirst().context().processName());
        assertEquals(at(6), pendingOnly.getFirst().start());
        assertEquals(at(7), pendingOnly.getFirst().end());
    }

    @Test
    void closeFlushesPendingCandidateAfterOpenSegmentWithoutOverlap() {
        EmSegmenter segmenter = new EmSegmenter();
        confirmA(segmenter);
        segmenter.onObservation(observed("browser.exe", null, 4));
        segmenter.onObservation(observed("browser.exe", null, 5));

        List<EmSegment> completed = segmenter.close(at(6));
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
        EmSegmenter segmenter = new EmSegmenter();
        segmenter.onObservation(observed("notepad.exe", null, 0));
        segmenter.onObservation(observed("notepad.exe", null, 1));

        List<EmSegment> completed = segmenter.close(at(2));
        assertEquals(1, completed.size());
        assertEquals(EmObservationKind.OBSERVED, completed.getFirst().kind());
        assertEquals("notepad.exe", completed.getFirst().context().processName());
        assertEquals(at(0), completed.getFirst().start());
        assertEquals(at(2), completed.getFirst().end());
    }

    @Test
    void closeFlushesOpenBoundarySegment() {
        EmSegmenter segmenter = new EmSegmenter();
        segmenter.onObservation(boundary(EmObservationKind.IDLE, 0));
        segmenter.onObservation(boundary(EmObservationKind.IDLE, 1));

        List<EmSegment> completed = segmenter.close(at(2));
        assertEquals(1, completed.size());
        assertEquals(EmObservationKind.IDLE, completed.getFirst().kind());
        assertNull(completed.getFirst().context());
        assertEquals(at(0), completed.getFirst().start());
        assertEquals(at(2), completed.getFirst().end());
    }

    @Test
    void clockBackwardsClosesWithoutNegativeDuration() {
        EmSegmenter segmenter = new EmSegmenter();
        confirmA(segmenter);
        EmTickResult backwards = segmenter.onObservation(observed("notepad.exe", null, 2));
        assertEquals(EmObservationKind.UNAVAILABLE, backwards.tick().kind());
        assertEquals(at(2), backwards.tick().contextSince());
        assertEquals(1, backwards.completedSegments().size());
        EmSegment closed = backwards.completedSegments().getFirst();
        assertFalse(closed.end().isBefore(closed.start()));
        assertEquals(at(3), closed.end());
    }
}
