package com.deriva.goals;

import com.deriva.goals.model.EmGoal;
import com.deriva.goals.model.EmGoalAlert;
import com.deriva.goals.model.EmGoalMetric;
import com.deriva.goals.model.EmGoalProgress;
import com.deriva.goals.model.EmGoalState;
import com.deriva.tracking.model.EmContextKey;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EmGoalAlertPolicyTest {
    private static final Instant NOW = Instant.parse("2026-11-24T10:00:00Z");
    private static final EmContextKey CODE = new EmContextKey("code.exe", null);

    @Test
    void minuteThresholdsFireAtEightyThenHundredAndNeverAgain() {
        EmGoal goal = goal(1, 10, null);

        assertTrue(plan(seconds(goal, 474), List.of()).isEmpty());

        List<EmGoalAlertPolicy.Plan> atEighty = plan(seconds(goal, 480), List.of());
        assertEquals(1, atEighty.size());
        assertEquals(EmGoalMetric.MINUTES, atEighty.getFirst().metric());
        assertEquals(List.of(80), atEighty.getFirst().thresholds());
        assertEquals(80, atEighty.getFirst().notifyThreshold());

        List<EmGoalAlertPolicy.Plan> atHundred =
                plan(seconds(goal, 600), List.of(alert(goal, 80, EmGoalMetric.MINUTES)));
        assertEquals(1, atHundred.size());
        assertEquals(List.of(100), atHundred.getFirst().thresholds());
        assertEquals(100, atHundred.getFirst().notifyThreshold());

        assertTrue(plan(seconds(goal, 600), List.of(
                alert(goal, 80, EmGoalMetric.MINUTES),
                alert(goal, 100, EmGoalMetric.MINUTES))).isEmpty());
    }

    @Test
    void entriesLimitOfFourReachesBothThresholdsAtTheSameEntry() {
        EmGoal goal = goal(2, null, 4);

        assertTrue(plan(entries(goal, 3), List.of()).isEmpty());

        List<EmGoalAlertPolicy.Plan> plans = plan(entries(goal, 4), List.of());
        assertEquals(1, plans.size());
        assertEquals(EmGoalMetric.ENTRIES, plans.getFirst().metric());
        assertEquals(List.of(80, 100), plans.getFirst().thresholds());
        assertEquals(100, plans.getFirst().notifyThreshold());
        assertEquals(4, plans.getFirst().used());
        assertEquals(4, plans.getFirst().limit());
    }

    @Test
    void minutesJumpBetweenEvaluationsRegistersBothAndNotifiesOnlyTheHighest() {
        EmGoal goal = goal(3, 10, null);

        assertTrue(plan(seconds(goal, 420), List.of()).isEmpty());

        List<EmGoalAlertPolicy.Plan> plans = plan(seconds(goal, 660), List.of());
        assertEquals(1, plans.size());
        assertEquals(List.of(80, 100), plans.getFirst().thresholds());
        assertEquals(100, plans.getFirst().notifyThreshold());
    }

    @Test
    void raisingTheLimitDoesNotRepeatAnAlreadyRecordedThreshold() {
        EmGoal raised = goal(4, 20, null);
        List<EmGoalAlert> recorded = List.of(alert(raised, 80, EmGoalMetric.MINUTES));

        assertTrue(plan(seconds(raised, 960), recorded).isEmpty());

        List<EmGoalAlertPolicy.Plan> atNewHundred = plan(seconds(raised, 1200), recorded);
        assertEquals(1, atNewHundred.size());
        assertEquals(List.of(100), atNewHundred.getFirst().thresholds());
        assertEquals(100, atNewHundred.getFirst().notifyThreshold());
    }

    @Test
    void eachGoalAndMetricIsAnIndependentLimit() {
        EmGoal minutes = goal(5, 10, null);
        EmGoal entries = goal(5, null, 4);

        List<EmGoalAlertPolicy.Plan> plans = plan(List.of(
                progress(minutes, Duration.ofMinutes(8).toMillis(), 4, EmGoalState.NEAR),
                progress(entries, Duration.ofMinutes(8).toMillis(), 4, EmGoalState.EXCEEDED)), List.of());

        assertEquals(2, plans.size());
        assertEquals(List.of(80), plans.get(0).thresholds());
        assertEquals(List.of(80, 100), plans.get(1).thresholds());
    }

    private static List<EmGoalAlertPolicy.Plan> plan(List<EmGoalProgress> progress, List<EmGoalAlert> alerts) {
        return EmGoalAlertPolicy.plan(progress, alerts);
    }

    private static List<EmGoalProgress> seconds(EmGoal goal, long seconds) {
        long limitSeconds = goal.dailyMinutes() * 60L;
        return List.of(progress(goal, Duration.ofSeconds(seconds).toMillis(), 0,
                seconds >= limitSeconds ? EmGoalState.EXCEEDED
                        : seconds * 100 >= limitSeconds * 80 ? EmGoalState.NEAR
                        : EmGoalState.IN_PROGRESS));
    }

    private static List<EmGoalProgress> entries(EmGoal goal, int entries) {
        return List.of(progress(goal, 0, entries,
                entries >= goal.dailyEntries() ? EmGoalState.EXCEEDED
                        : (long) entries * 100 >= (long) goal.dailyEntries() * 80 ? EmGoalState.NEAR
                        : EmGoalState.IN_PROGRESS));
    }

    private static EmGoalProgress progress(EmGoal goal, long usedMillis, int entries, EmGoalState state) {
        return new EmGoalProgress(goal, usedMillis, entries, state, false);
    }

    private static EmGoal goal(long id, Integer minutes, Integer entries) {
        return new EmGoal(id, CODE, minutes, entries, false, true, NOW, NOW);
    }

    private static EmGoalAlert alert(EmGoal goal, int threshold, EmGoalMetric metric) {
        return new EmGoalAlert(1, goal.id(), NOW, threshold, metric, null, null);
    }
}
