package com.deriva.goals;

import com.deriva.goals.model.EmGoal;
import com.deriva.privacy.EmObservation;
import com.deriva.privacy.EmObservationKind;
import com.deriva.privacy.EmPrivacyFilter;
import com.deriva.sensor.EmActivitySample;
import com.deriva.tracking.EmSegmenter;
import com.deriva.tracking.model.EmContextKey;
import com.deriva.tracking.model.EmSegment;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class EmPassiveActivityTest {
    private static final Instant BASE = Instant.parse("2026-11-24T10:00:00Z");
    private static final long OWN_PID = 999_999;
    private static final Duration MAX = Duration.ofMinutes(10);

    private static final EmContextKey NOTEPAD = new EmContextKey("notepad.exe", null);
    private static final EmContextKey CHESS = new EmContextKey("brave.exe", "chess.com");

    private static EmObservation observed(String process, String host) {
        return new EmObservation(BASE, EmObservationKind.OBSERVED, process, host);
    }

    private static EmObservation boundary(EmObservationKind kind) {
        return new EmObservation(BASE, kind, null, null);
    }

    private static EmObservation observedAt(String process, String host, int second) {
        return new EmObservation(BASE.plusSeconds(second), EmObservationKind.OBSERVED, process, host);
    }

    private static EmObservation boundaryAt(EmObservationKind kind, int second) {
        return new EmObservation(BASE.plusSeconds(second), kind, null, null);
    }

    private static EmGoal passiveGoal(EmContextKey context) {
        return EmGoal.draft(context, 30, null, true);
    }

    private static EmPassiveActivity activity(EmGoal... goals) {
        return new EmPassiveActivity(MAX, List.of(goals));
    }

    private static EmActivitySample sample(String process) {
        return sample(process, 0);
    }

    private static EmActivitySample sample(String process, int second) {
        return new EmActivitySample(BASE.plusSeconds(second), process, 123, 456 + second, false, true);
    }

    @Test
    void fiveMinutesWithoutInputCountAsObservedForPassiveGoal() {
        EmPassiveActivity passive = activity(passiveGoal(NOTEPAD));

        EmObservation result = passive.apply(boundary(EmObservationKind.IDLE),
                observed("notepad.exe", null), Duration.ofMinutes(5));

        assertEquals(EmObservationKind.OBSERVED, result.kind());
        assertEquals("notepad.exe", result.processName());
        assertNull(result.siteHost());
    }

    @Test
    void maximumIsInclusiveAndOneSecondOverReturnsIdle() {
        EmPassiveActivity passive = activity(passiveGoal(NOTEPAD));
        EmObservation foreground = observed("notepad.exe", null);

        assertEquals(EmObservationKind.OBSERVED, passive.apply(boundary(EmObservationKind.IDLE),
                foreground, MAX).kind());
        assertEquals(EmObservationKind.IDLE, passive.apply(boundary(EmObservationKind.IDLE),
                foreground, MAX.plusSeconds(1)).kind());
    }

    @Test
    void withoutPassiveFlagEveryObservationIsUnchanged() {
        EmPassiveActivity passive = new EmPassiveActivity(MAX, List.of());

        for (EmObservationKind kind : EmObservationKind.values()) {
            EmObservation input = kind == EmObservationKind.OBSERVED
                    ? observed("notepad.exe", null)
                    : boundary(kind);
            assertEquals(input, passive.apply(input, observed("notepad.exe", null),
                    Duration.ofMinutes(1)));
        }
        assertFalse(passive.hasContexts());
    }

    @Test
    void nonPassiveGoalDoesNotApply() {
        EmGoal activeButNotPassive = EmGoal.draft(NOTEPAD, 30, null, false);
        EmPassiveActivity passive = activity(activeButNotPassive);

        EmObservation result = passive.apply(boundary(EmObservationKind.IDLE),
                observed("notepad.exe", null), Duration.ofMinutes(5));

        assertEquals(EmObservationKind.IDLE, result.kind());
    }

    @Test
    void inactivePassiveGoalDoesNotApply() {
        EmGoal inactive = new EmGoal(1, NOTEPAD, 30, null, true, false, BASE, BASE);
        EmPassiveActivity passive = activity(inactive);

        EmObservation result = passive.apply(boundary(EmObservationKind.IDLE),
                observed("notepad.exe", null), Duration.ofMinutes(5));

        assertEquals(EmObservationKind.IDLE, result.kind());
    }

    @Test
    void changingToNonPassiveContextCutsTheRule() {
        EmPassiveActivity passive = activity(passiveGoal(NOTEPAD));

        assertEquals(EmObservationKind.OBSERVED, passive.apply(boundary(EmObservationKind.IDLE),
                observed("notepad.exe", null), Duration.ofMinutes(5)).kind());
        assertEquals(EmObservationKind.IDLE, passive.apply(boundary(EmObservationKind.IDLE),
                observed("writer.exe", null), Duration.ofMinutes(5)).kind());
    }

    @Test
    void changingToAnotherPassiveContextAlsoCutsUntilActivityResumes() {
        EmPassiveActivity passive = activity(passiveGoal(NOTEPAD),
                passiveGoal(new EmContextKey("writer.exe", null)));

        assertEquals(EmObservationKind.OBSERVED, passive.apply(boundary(EmObservationKind.IDLE),
                observed("notepad.exe", null), Duration.ofMinutes(5)).kind());
        assertEquals(EmObservationKind.IDLE, passive.apply(boundary(EmObservationKind.IDLE),
                observed("writer.exe", null), Duration.ofMinutes(5)).kind());

        EmObservation active = observed("writer.exe", null);
        assertEquals(EmObservationKind.OBSERVED,
                passive.apply(active, active, Duration.ZERO).kind());
        assertEquals(EmObservationKind.OBSERVED, passive.apply(boundary(EmObservationKind.IDLE),
                active, Duration.ofMinutes(1)).kind());
    }

    @Test
    void editingAnUnrelatedGoalDoesNotCutAnOngoingPromotion() {
        EmPassiveActivity passive = activity(passiveGoal(NOTEPAD));
        assertEquals(EmObservationKind.OBSERVED, passive.apply(boundaryAt(EmObservationKind.IDLE, 0),
                observedAt("notepad.exe", null, 0), Duration.ofMinutes(1)).kind());

        passive.updateGoals(List.of(passiveGoal(NOTEPAD),
                passiveGoal(new EmContextKey("writer.exe", null))));
        assertEquals(EmObservationKind.OBSERVED, passive.apply(boundaryAt(EmObservationKind.IDLE, 1),
                observedAt("notepad.exe", null, 1), Duration.ofMinutes(1)).kind());

        passive.updateGoals(List.of(EmGoal.draft(NOTEPAD, 60, 4, true),
                passiveGoal(new EmContextKey("writer.exe", null))));
        assertEquals(EmObservationKind.OBSERVED, passive.apply(boundaryAt(EmObservationKind.IDLE, 2),
                observedAt("notepad.exe", null, 2), Duration.ofMinutes(1)).kind());
    }

    @Test
    void removingThePromotedContextCutsTheRule() {
        EmPassiveActivity passive = activity(passiveGoal(NOTEPAD));
        assertEquals(EmObservationKind.OBSERVED, passive.apply(boundaryAt(EmObservationKind.IDLE, 0),
                observedAt("notepad.exe", null, 0), Duration.ofMinutes(1)).kind());

        passive.updateGoals(List.of(passiveGoal(new EmContextKey("writer.exe", null))));
        assertEquals(EmObservationKind.IDLE, passive.apply(boundaryAt(EmObservationKind.IDLE, 1),
                observedAt("notepad.exe", null, 1), Duration.ofMinutes(1)).kind());
    }

    @Test
    void suspensionGapCutsTheRuleUntilActivityResumes() {
        EmPassiveActivity passive = activity(passiveGoal(NOTEPAD));

        assertEquals(EmObservationKind.OBSERVED, passive.apply(boundaryAt(EmObservationKind.IDLE, 0),
                observedAt("notepad.exe", null, 0), Duration.ofMinutes(1)).kind());
        assertEquals(EmObservationKind.IDLE, passive.apply(boundaryAt(EmObservationKind.IDLE, 10),
                observedAt("notepad.exe", null, 10), Duration.ofMinutes(1)).kind());
        assertEquals(EmObservationKind.IDLE, passive.apply(boundaryAt(EmObservationKind.IDLE, 11),
                observedAt("notepad.exe", null, 11), Duration.ofMinutes(1)).kind());

        EmObservation active = observedAt("notepad.exe", null, 12);
        assertEquals(EmObservationKind.OBSERVED,
                passive.apply(active, active, Duration.ZERO).kind());
        assertEquals(EmObservationKind.OBSERVED, passive.apply(boundaryAt(EmObservationKind.IDLE, 13),
                observedAt("notepad.exe", null, 13), Duration.ofMinutes(1)).kind());
    }

    @Test
    void lockAndUnavailableWinOverThePassiveRule() {
        EmPassiveActivity passive = activity(passiveGoal(NOTEPAD));
        EmPrivacyFilter filter = new EmPrivacyFilter(OWN_PID, Duration.ofSeconds(120));

        EmActivitySample lock = sample("lockapp.exe");
        assertEquals(EmObservationKind.UNAVAILABLE,
                filter.filter(lock, Duration.ofSeconds(300)).kind());
        assertEquals(EmObservationKind.UNAVAILABLE, passive.apply(
                filter.filter(lock, Duration.ofSeconds(300)),
                filter.filter(lock, Duration.ZERO), Duration.ofSeconds(300)).kind());

        EmActivitySample unavailable = new EmActivitySample(BASE, null, 0, 0, false, false);
        assertEquals(EmObservationKind.UNAVAILABLE, passive.apply(
                filter.filter(unavailable, Duration.ofSeconds(300)),
                filter.filter(unavailable, Duration.ZERO), Duration.ofSeconds(300)).kind());
    }

    @Test
    void appGoalOnlyMatchesForegroundWithoutHost() {
        EmPassiveActivity passive = activity(passiveGoal(NOTEPAD));

        assertEquals(EmObservationKind.OBSERVED, passive.apply(boundary(EmObservationKind.IDLE),
                observed("notepad.exe", null), Duration.ofMinutes(1)).kind());
        assertEquals(EmObservationKind.IDLE, passive.apply(boundary(EmObservationKind.IDLE),
                observed("notepad.exe", "example.com"), Duration.ofMinutes(1)).kind());
    }

    @Test
    void siteGoalMatchesExactProcessAndHostOnly() {
        EmPassiveActivity passive = activity(passiveGoal(CHESS));

        assertEquals(EmObservationKind.OBSERVED, passive.apply(boundary(EmObservationKind.IDLE),
                observed("brave.exe", "chess.com"), Duration.ofMinutes(1)).kind());
        assertEquals(EmObservationKind.IDLE, passive.apply(boundary(EmObservationKind.IDLE),
                observed("brave.exe", "lichess.org"), Duration.ofMinutes(1)).kind());
        assertEquals(EmObservationKind.IDLE, passive.apply(boundary(EmObservationKind.IDLE),
                observed("chrome.exe", "chess.com"), Duration.ofMinutes(1)).kind());
    }

    @Test
    void approvedSegmentStaysObservedUntilMaximumThenClosesAsIdle() {
        EmPassiveActivity passive = activity(passiveGoal(NOTEPAD));
        EmPrivacyFilter filter = new EmPrivacyFilter(OWN_PID, Duration.ofSeconds(120));
        EmSegmenter segmenter = new EmSegmenter();
        List<EmSegment> completed = new java.util.ArrayList<>();

        for (int second = 0; second <= 600; second++) {
            EmActivitySample tick = sample("notepad.exe", second);
            EmObservation filtered = filter.filter(tick, Duration.ofSeconds(second));
            EmObservation foreground = filtered.kind() == EmObservationKind.IDLE
                    ? filter.filter(tick, Duration.ZERO)
                    : filtered;
            EmObservation observation = passive.apply(filtered, foreground, Duration.ofSeconds(second));
            completed.addAll(segmenter.onObservation(observation).completedSegments());
        }
        assertEquals(EmObservationKind.OBSERVED, segmenter.snapshot(BASE.plusSeconds(600))
                .open().kind());

        EmActivitySample finalTick = sample("notepad.exe", 601);
        EmObservation atMax = filter.filter(finalTick, Duration.ofSeconds(601));
        completed.addAll(segmenter.onObservation(passive.apply(atMax,
                filter.filter(finalTick, Duration.ZERO), Duration.ofSeconds(601)))
                .completedSegments());

        assertEquals(1, completed.size());
        assertEquals(EmObservationKind.OBSERVED, completed.getFirst().kind());
        assertEquals(NOTEPAD, completed.getFirst().context());
        assertEquals(BASE.plusSeconds(601), completed.getFirst().end());
        segmenter.onObservation(new EmObservation(BASE.plusSeconds(602), EmObservationKind.IDLE, null, null));
        assertEquals(EmObservationKind.IDLE, segmenter.snapshot(BASE.plusSeconds(603))
                .open().kind());
    }
}
