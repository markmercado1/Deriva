package com.deriva.coach;

import com.deriva.coach.model.EmDetectionSettings;
import com.deriva.coach.model.EmDriftEpisode;
import com.deriva.coach.model.EmDriftStatus;
import com.deriva.coach.model.EmDriftUpdate;
import com.deriva.privacy.EmObservationKind;
import com.deriva.tracking.model.EmContextKey;
import com.deriva.tracking.model.EmStableTick;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class EmDriftStateMachineConfigTest {
    private static final Instant BASE = Instant.parse("2026-11-23T10:00:00Z");

    private static Instant at(int second) {
        return BASE.plusSeconds(second);
    }

    private static EmStableTick observed(String process, int second, int since, boolean pending) {
        return new EmStableTick(at(second), EmObservationKind.OBSERVED,
                new EmContextKey(process, null), at(since), pending);
    }

    private static EmDriftStateMachine detector(int switches, int window, int stability) {
        return new EmDriftStateMachine(new EmDetectionSettings(switches,
                Duration.ofSeconds(window), Duration.ofSeconds(stability), Duration.ofSeconds(120)));
    }

    @Test
    void opensAtConfiguredFourChangesInsteadOfTen() {
        EmDriftStateMachine detector = detector(4, 120, 180);
        detector.onTick(observed("a.exe", 0, 0, false));
        assertTrue(detector.onTick(observed("b.exe", 1, 1, false)).isEmpty());
        assertTrue(detector.onTick(observed("a.exe", 2, 2, false)).isEmpty());
        assertTrue(detector.onTick(observed("b.exe", 3, 3, false)).isEmpty());

        EmDriftUpdate opened = detector.onTick(observed("a.exe", 4, 4, false)).getFirst();
        assertEquals(EmDriftUpdate.Type.OPENED, opened.type());
        assertEquals(4, opened.episode().switchCount());
        assertEquals(EmDriftStatus.OPEN, opened.episode().status());
    }

    @Test
    void configuredWindowKeepsStrictBoundary() {
        EmDriftStateMachine detector = detector(4, 30, 180);
        detector.onTick(observed("a.exe", -1, -1, false));
        detector.onTick(observed("b.exe", 0, 0, false));
        detector.onTick(observed("a.exe", 1, 1, false));
        detector.onTick(observed("b.exe", 2, 2, false));
        assertTrue(detector.onTick(observed("a.exe", 30, 30, false)).isEmpty(),
                "the change exactly 30 s old must not count");
        assertTrue(detector.onTick(observed("b.exe", 31, 31, false)).isEmpty());
        assertTrue(detector.onTick(observed("a.exe", 32, 32, false)).isEmpty());
        EmDriftUpdate opened = detector.onTick(observed("b.exe", 33, 33, false)).getFirst();
        assertEquals(4, opened.episode().switchCount());
    }

    @Test
    void configuredStabilityClosesAtThirtySeconds() {
        EmDriftStateMachine detector = detector(4, 120, 30);
        detector.onTick(observed("a.exe", 0, 0, false));
        detector.onTick(observed("b.exe", 1, 1, false));
        detector.onTick(observed("a.exe", 2, 2, false));
        detector.onTick(observed("b.exe", 3, 3, false));
        EmDriftUpdate opened = detector.onTick(observed("a.exe", 4, 4, false)).getFirst();
        assertEquals(EmDriftStatus.OPEN, opened.episode().status());

        assertTrue(detector.onTick(observed("a.exe", 33, 4, false)).isEmpty());
        EmDriftUpdate closed = detector.onTick(observed("a.exe", 34, 4, false)).getFirst();
        assertEquals(EmDriftUpdate.Type.CLOSED, closed.type());
        assertEquals(EmDriftStatus.STABLE, closed.episode().status());
    }

    @Test
    void pendingCandidateStillBlocksConfiguredStability() {
        EmDriftStateMachine detector = detector(4, 120, 30);
        detector.onTick(observed("a.exe", 0, 0, false));
        for (int i = 1; i <= 4; i++)
            detector.onTick(observed(i % 2 == 1 ? "b.exe" : "a.exe", i, i, false));
        assertTrue(detector.onTick(observed("a.exe", 35, 4, true)).isEmpty());
        EmDriftUpdate closed = detector.onTick(observed("a.exe", 36, 4, false)).getFirst();
        assertEquals(EmDriftStatus.STABLE, closed.episode().status());
    }
}
