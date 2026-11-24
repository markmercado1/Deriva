package com.deriva.goals;

import com.deriva.goals.model.Goal;
import com.deriva.privacy.Observation;
import com.deriva.privacy.ObservationKind;
import com.deriva.privacy.PrivacyFilter;
import com.deriva.sensor.ActivitySample;
import com.deriva.tracking.Segmenter;
import com.deriva.tracking.model.ContextKey;
import com.deriva.tracking.model.Segment;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PassiveActivityTest {
    private static final Instant BASE = Instant.parse("2026-11-24T10:00:00Z");
    private static final long OWN_PID = 999_999;
    private static final Duration MAX = Duration.ofMinutes(10);

    private static final ContextKey NOTEPAD = new ContextKey("notepad.exe", null);
    private static final ContextKey CHESS = new ContextKey("brave.exe", "chess.com");

    private static Observation observed(String process, String host) {
        return new Observation(BASE, ObservationKind.OBSERVED, process, host);
    }

    private static Observation boundary(ObservationKind kind) {
        return new Observation(BASE, kind, null, null);
    }

    private static Observation observedAt(String process, String host, int second) {
        return new Observation(BASE.plusSeconds(second), ObservationKind.OBSERVED, process, host);
    }

    private static Observation boundaryAt(ObservationKind kind, int second) {
        return new Observation(BASE.plusSeconds(second), kind, null, null);
    }

    private static Goal passiveGoal(ContextKey context) {
        return Goal.draft(context, 30, null, true);
    }

    private static PassiveActivity activity(Goal... goals) {
        return new PassiveActivity(MAX, List.of(goals));
    }

    private static ActivitySample sample(String process) {
        return sample(process, 0);
    }

    private static ActivitySample sample(String process, int second) {
        return new ActivitySample(BASE.plusSeconds(second), process, 123, 456 + second, false, true);
    }

    @Test
    void fiveMinutesWithoutInputCountAsObservedForPassiveGoal() {
        PassiveActivity passive = activity(passiveGoal(NOTEPAD));

        Observation result = passive.apply(boundary(ObservationKind.IDLE),
                observed("notepad.exe", null), Duration.ofMinutes(5));

        assertEquals(ObservationKind.OBSERVED, result.kind());
        assertEquals("notepad.exe", result.processName());
        assertNull(result.siteHost());
    }

    @Test
    void maximumIsInclusiveAndOneSecondOverReturnsIdle() {
        PassiveActivity passive = activity(passiveGoal(NOTEPAD));
        Observation foreground = observed("notepad.exe", null);

        assertEquals(ObservationKind.OBSERVED, passive.apply(boundary(ObservationKind.IDLE),
                foreground, MAX).kind());
        assertEquals(ObservationKind.IDLE, passive.apply(boundary(ObservationKind.IDLE),
                foreground, MAX.plusSeconds(1)).kind());
    }

    @Test
    void withoutPassiveFlagEveryObservationIsUnchanged() {
        PassiveActivity passive = new PassiveActivity(MAX, List.of());

        for (ObservationKind kind : ObservationKind.values()) {
            Observation input = kind == ObservationKind.OBSERVED
                    ? observed("notepad.exe", null)
                    : boundary(kind);
            assertEquals(input, passive.apply(input, observed("notepad.exe", null),
                    Duration.ofMinutes(1)));
        }
        assertFalse(passive.hasContexts());
    }

    @Test
    void nonPassiveGoalDoesNotApply() {
        Goal activeButNotPassive = Goal.draft(NOTEPAD, 30, null, false);
        PassiveActivity passive = activity(activeButNotPassive);

        Observation result = passive.apply(boundary(ObservationKind.IDLE),
                observed("notepad.exe", null), Duration.ofMinutes(5));

        assertEquals(ObservationKind.IDLE, result.kind());
    }

    @Test
    void inactivePassiveGoalDoesNotApply() {
        Goal inactive = new Goal(1, NOTEPAD, 30, null, true, false, BASE, BASE);
        PassiveActivity passive = activity(inactive);

        Observation result = passive.apply(boundary(ObservationKind.IDLE),
                observed("notepad.exe", null), Duration.ofMinutes(5));

        assertEquals(ObservationKind.IDLE, result.kind());
    }

    @Test
    void changingToNonPassiveContextCutsTheRule() {
        PassiveActivity passive = activity(passiveGoal(NOTEPAD));

        assertEquals(ObservationKind.OBSERVED, passive.apply(boundary(ObservationKind.IDLE),
                observed("notepad.exe", null), Duration.ofMinutes(5)).kind());
        assertEquals(ObservationKind.IDLE, passive.apply(boundary(ObservationKind.IDLE),
                observed("writer.exe", null), Duration.ofMinutes(5)).kind());
    }

    @Test
    void changingToAnotherPassiveContextAlsoCutsUntilActivityResumes() {
        PassiveActivity passive = activity(passiveGoal(NOTEPAD),
                passiveGoal(new ContextKey("writer.exe", null)));

        assertEquals(ObservationKind.OBSERVED, passive.apply(boundary(ObservationKind.IDLE),
                observed("notepad.exe", null), Duration.ofMinutes(5)).kind());
        assertEquals(ObservationKind.IDLE, passive.apply(boundary(ObservationKind.IDLE),
                observed("writer.exe", null), Duration.ofMinutes(5)).kind());

        Observation active = observed("writer.exe", null);
        assertEquals(ObservationKind.OBSERVED,
                passive.apply(active, active, Duration.ZERO).kind());
        assertEquals(ObservationKind.OBSERVED, passive.apply(boundary(ObservationKind.IDLE),
                active, Duration.ofMinutes(1)).kind());
    }

    @Test
    void editingAnUnrelatedGoalDoesNotCutAnOngoingPromotion() {
        PassiveActivity passive = activity(passiveGoal(NOTEPAD));
        assertEquals(ObservationKind.OBSERVED, passive.apply(boundaryAt(ObservationKind.IDLE, 0),
                observedAt("notepad.exe", null, 0), Duration.ofMinutes(1)).kind());

        passive.updateGoals(List.of(passiveGoal(NOTEPAD),
                passiveGoal(new ContextKey("writer.exe", null))));
        assertEquals(ObservationKind.OBSERVED, passive.apply(boundaryAt(ObservationKind.IDLE, 1),
                observedAt("notepad.exe", null, 1), Duration.ofMinutes(1)).kind());

        passive.updateGoals(List.of(Goal.draft(NOTEPAD, 60, 4, true),
                passiveGoal(new ContextKey("writer.exe", null))));
        assertEquals(ObservationKind.OBSERVED, passive.apply(boundaryAt(ObservationKind.IDLE, 2),
                observedAt("notepad.exe", null, 2), Duration.ofMinutes(1)).kind());
    }

    @Test
    void removingThePromotedContextCutsTheRule() {
        PassiveActivity passive = activity(passiveGoal(NOTEPAD));
        assertEquals(ObservationKind.OBSERVED, passive.apply(boundaryAt(ObservationKind.IDLE, 0),
                observedAt("notepad.exe", null, 0), Duration.ofMinutes(1)).kind());

        passive.updateGoals(List.of(passiveGoal(new ContextKey("writer.exe", null))));
        assertEquals(ObservationKind.IDLE, passive.apply(boundaryAt(ObservationKind.IDLE, 1),
                observedAt("notepad.exe", null, 1), Duration.ofMinutes(1)).kind());
    }

    @Test
    void suspensionGapCutsTheRuleUntilActivityResumes() {
        PassiveActivity passive = activity(passiveGoal(NOTEPAD));

        assertEquals(ObservationKind.OBSERVED, passive.apply(boundaryAt(ObservationKind.IDLE, 0),
                observedAt("notepad.exe", null, 0), Duration.ofMinutes(1)).kind());
        assertEquals(ObservationKind.IDLE, passive.apply(boundaryAt(ObservationKind.IDLE, 10),
                observedAt("notepad.exe", null, 10), Duration.ofMinutes(1)).kind());
        assertEquals(ObservationKind.IDLE, passive.apply(boundaryAt(ObservationKind.IDLE, 11),
                observedAt("notepad.exe", null, 11), Duration.ofMinutes(1)).kind());

        Observation active = observedAt("notepad.exe", null, 12);
        assertEquals(ObservationKind.OBSERVED,
                passive.apply(active, active, Duration.ZERO).kind());
        assertEquals(ObservationKind.OBSERVED, passive.apply(boundaryAt(ObservationKind.IDLE, 13),
                observedAt("notepad.exe", null, 13), Duration.ofMinutes(1)).kind());
    }

    @Test
    void lockAndUnavailableWinOverThePassiveRule() {
        PassiveActivity passive = activity(passiveGoal(NOTEPAD));
        PrivacyFilter filter = new PrivacyFilter(OWN_PID, Duration.ofSeconds(120));

        ActivitySample lock = sample("lockapp.exe");
        assertEquals(ObservationKind.UNAVAILABLE,
                filter.filter(lock, Duration.ofSeconds(300)).kind());
        assertEquals(ObservationKind.UNAVAILABLE, passive.apply(
                filter.filter(lock, Duration.ofSeconds(300)),
                filter.filter(lock, Duration.ZERO), Duration.ofSeconds(300)).kind());

        ActivitySample unavailable = new ActivitySample(BASE, null, 0, 0, false, false);
        assertEquals(ObservationKind.UNAVAILABLE, passive.apply(
                filter.filter(unavailable, Duration.ofSeconds(300)),
                filter.filter(unavailable, Duration.ZERO), Duration.ofSeconds(300)).kind());
    }

    @Test
    void appGoalOnlyMatchesForegroundWithoutHost() {
        PassiveActivity passive = activity(passiveGoal(NOTEPAD));

        assertEquals(ObservationKind.OBSERVED, passive.apply(boundary(ObservationKind.IDLE),
                observed("notepad.exe", null), Duration.ofMinutes(1)).kind());
        assertEquals(ObservationKind.IDLE, passive.apply(boundary(ObservationKind.IDLE),
                observed("notepad.exe", "example.com"), Duration.ofMinutes(1)).kind());
    }

    @Test
    void siteGoalMatchesExactProcessAndHostOnly() {
        PassiveActivity passive = activity(passiveGoal(CHESS));

        assertEquals(ObservationKind.OBSERVED, passive.apply(boundary(ObservationKind.IDLE),
                observed("brave.exe", "chess.com"), Duration.ofMinutes(1)).kind());
        assertEquals(ObservationKind.IDLE, passive.apply(boundary(ObservationKind.IDLE),
                observed("brave.exe", "lichess.org"), Duration.ofMinutes(1)).kind());
        assertEquals(ObservationKind.IDLE, passive.apply(boundary(ObservationKind.IDLE),
                observed("chrome.exe", "chess.com"), Duration.ofMinutes(1)).kind());
    }

    @Test
    void approvedSegmentStaysObservedUntilMaximumThenClosesAsIdle() {
        PassiveActivity passive = activity(passiveGoal(NOTEPAD));
        PrivacyFilter filter = new PrivacyFilter(OWN_PID, Duration.ofSeconds(120));
        Segmenter segmenter = new Segmenter();
        List<Segment> completed = new java.util.ArrayList<>();

        for (int second = 0; second <= 600; second++) {
            ActivitySample tick = sample("notepad.exe", second);
            Observation filtered = filter.filter(tick, Duration.ofSeconds(second));
            Observation foreground = filtered.kind() == ObservationKind.IDLE
                    ? filter.filter(tick, Duration.ZERO)
                    : filtered;
            Observation observation = passive.apply(filtered, foreground, Duration.ofSeconds(second));
            completed.addAll(segmenter.onObservation(observation).completedSegments());
        }
        assertEquals(ObservationKind.OBSERVED, segmenter.snapshot(BASE.plusSeconds(600))
                .open().kind());

        ActivitySample finalTick = sample("notepad.exe", 601);
        Observation atMax = filter.filter(finalTick, Duration.ofSeconds(601));
        completed.addAll(segmenter.onObservation(passive.apply(atMax,
                filter.filter(finalTick, Duration.ZERO), Duration.ofSeconds(601)))
                .completedSegments());

        assertEquals(1, completed.size());
        assertEquals(ObservationKind.OBSERVED, completed.getFirst().kind());
        assertEquals(NOTEPAD, completed.getFirst().context());
        assertEquals(BASE.plusSeconds(601), completed.getFirst().end());
        segmenter.onObservation(new Observation(BASE.plusSeconds(602), ObservationKind.IDLE, null, null));
        assertEquals(ObservationKind.IDLE, segmenter.snapshot(BASE.plusSeconds(603))
                .open().kind());
    }
}
