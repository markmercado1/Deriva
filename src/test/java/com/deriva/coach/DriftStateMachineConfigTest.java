package com.deriva.coach;

import com.deriva.coach.model.DetectionSettings;
import com.deriva.coach.model.DriftEpisode;
import com.deriva.coach.model.DriftStatus;
import com.deriva.coach.model.DriftUpdate;
import com.deriva.privacy.ObservationKind;
import com.deriva.tracking.model.ContextKey;
import com.deriva.tracking.model.StableTick;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DriftStateMachineConfigTest {
    private static final Instant BASE = Instant.parse("2026-11-23T10:00:00Z");

    private static Instant at(int second) {
        return BASE.plusSeconds(second);
    }

    private static StableTick observed(String process, int second, int since, boolean pending) {
        return new StableTick(at(second), ObservationKind.OBSERVED,
                new ContextKey(process, null), at(since), pending);
    }

    private static DriftStateMachine detector(int switches, int window, int stability) {
        return new DriftStateMachine(new DetectionSettings(switches,
                Duration.ofSeconds(window), Duration.ofSeconds(stability), Duration.ofSeconds(120)));
    }

    @Test
    void opensAtConfiguredFourChangesInsteadOfTen() {
        DriftStateMachine detector = detector(4, 120, 180);
        detector.onTick(observed("a.exe", 0, 0, false));
        assertTrue(detector.onTick(observed("b.exe", 1, 1, false)).isEmpty());
        assertTrue(detector.onTick(observed("a.exe", 2, 2, false)).isEmpty());
        assertTrue(detector.onTick(observed("b.exe", 3, 3, false)).isEmpty());

        DriftUpdate opened = detector.onTick(observed("a.exe", 4, 4, false)).getFirst();
        assertEquals(DriftUpdate.Type.OPENED, opened.type());
        assertEquals(4, opened.episode().switchCount());
        assertEquals(DriftStatus.OPEN, opened.episode().status());
    }

    @Test
    void configuredWindowKeepsStrictBoundary() {
        DriftStateMachine detector = detector(4, 30, 180);
        detector.onTick(observed("a.exe", -1, -1, false));
        detector.onTick(observed("b.exe", 0, 0, false));
        detector.onTick(observed("a.exe", 1, 1, false));
        detector.onTick(observed("b.exe", 2, 2, false));
        assertTrue(detector.onTick(observed("a.exe", 30, 30, false)).isEmpty(),
                "the change exactly 30 s old must not count");
        assertTrue(detector.onTick(observed("b.exe", 31, 31, false)).isEmpty());
        assertTrue(detector.onTick(observed("a.exe", 32, 32, false)).isEmpty());
        DriftUpdate opened = detector.onTick(observed("b.exe", 33, 33, false)).getFirst();
        assertEquals(4, opened.episode().switchCount());
    }

    @Test
    void configuredStabilityClosesAtThirtySeconds() {
        DriftStateMachine detector = detector(4, 120, 30);
        detector.onTick(observed("a.exe", 0, 0, false));
        detector.onTick(observed("b.exe", 1, 1, false));
        detector.onTick(observed("a.exe", 2, 2, false));
        detector.onTick(observed("b.exe", 3, 3, false));
        DriftUpdate opened = detector.onTick(observed("a.exe", 4, 4, false)).getFirst();
        assertEquals(DriftStatus.OPEN, opened.episode().status());

        assertTrue(detector.onTick(observed("a.exe", 33, 4, false)).isEmpty());
        DriftUpdate closed = detector.onTick(observed("a.exe", 34, 4, false)).getFirst();
        assertEquals(DriftUpdate.Type.CLOSED, closed.type());
        assertEquals(DriftStatus.STABLE, closed.episode().status());
    }

    @Test
    void pendingCandidateStillBlocksConfiguredStability() {
        DriftStateMachine detector = detector(4, 120, 30);
        detector.onTick(observed("a.exe", 0, 0, false));
        for (int i = 1; i <= 4; i++)
            detector.onTick(observed(i % 2 == 1 ? "b.exe" : "a.exe", i, i, false));
        assertTrue(detector.onTick(observed("a.exe", 35, 4, true)).isEmpty());
        DriftUpdate closed = detector.onTick(observed("a.exe", 36, 4, false)).getFirst();
        assertEquals(DriftStatus.STABLE, closed.episode().status());
    }
}
