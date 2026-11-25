package com.deriva.goals;

import com.deriva.goals.model.Goal;
import com.deriva.goals.model.GoalAlert;
import com.deriva.goals.model.GoalMetric;
import com.deriva.goals.model.GoalProgress;
import com.deriva.goals.model.GoalState;
import com.deriva.tracking.model.ContextKey;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GoalAlertPolicyTest {
    private static final Instant NOW = Instant.parse("2026-11-24T10:00:00Z");
    private static final ContextKey CODE = new ContextKey("code.exe", null);

    @Test
    void minuteThresholdsFireAtEightyThenHundredAndNeverAgain() {
        Goal goal = goal(1, 10, null);

        assertTrue(plan(seconds(goal, 474), List.of()).isEmpty());

        List<GoalAlertPolicy.Plan> atEighty = plan(seconds(goal, 480), List.of());
        assertEquals(1, atEighty.size());
        assertEquals(GoalMetric.MINUTES, atEighty.getFirst().metric());
        assertEquals(List.of(80), atEighty.getFirst().thresholds());
        assertEquals(80, atEighty.getFirst().notifyThreshold());

        List<GoalAlertPolicy.Plan> atHundred =
                plan(seconds(goal, 600), List.of(alert(goal, 80, GoalMetric.MINUTES)));
        assertEquals(1, atHundred.size());
        assertEquals(List.of(100), atHundred.getFirst().thresholds());
        assertEquals(100, atHundred.getFirst().notifyThreshold());

        assertTrue(plan(seconds(goal, 600), List.of(
                alert(goal, 80, GoalMetric.MINUTES),
                alert(goal, 100, GoalMetric.MINUTES))).isEmpty());
    }

    @Test
    void entriesLimitOfFourReachesBothThresholdsAtTheSameEntry() {
        Goal goal = goal(2, null, 4);

        assertTrue(plan(entries(goal, 3), List.of()).isEmpty());

        List<GoalAlertPolicy.Plan> plans = plan(entries(goal, 4), List.of());
        assertEquals(1, plans.size());
        assertEquals(GoalMetric.ENTRIES, plans.getFirst().metric());
        assertEquals(List.of(80, 100), plans.getFirst().thresholds());
        assertEquals(100, plans.getFirst().notifyThreshold());
        assertEquals(4, plans.getFirst().used());
        assertEquals(4, plans.getFirst().limit());
    }

    @Test
    void minutesJumpBetweenEvaluationsRegistersBothAndNotifiesOnlyTheHighest() {
        Goal goal = goal(3, 10, null);

        assertTrue(plan(seconds(goal, 420), List.of()).isEmpty());

        List<GoalAlertPolicy.Plan> plans = plan(seconds(goal, 660), List.of());
        assertEquals(1, plans.size());
        assertEquals(List.of(80, 100), plans.getFirst().thresholds());
        assertEquals(100, plans.getFirst().notifyThreshold());
    }

    @Test
    void raisingTheLimitDoesNotRepeatAnAlreadyRecordedThreshold() {
        Goal raised = goal(4, 20, null);
        List<GoalAlert> recorded = List.of(alert(raised, 80, GoalMetric.MINUTES));

        assertTrue(plan(seconds(raised, 960), recorded).isEmpty());

        List<GoalAlertPolicy.Plan> atNewHundred = plan(seconds(raised, 1200), recorded);
        assertEquals(1, atNewHundred.size());
        assertEquals(List.of(100), atNewHundred.getFirst().thresholds());
        assertEquals(100, atNewHundred.getFirst().notifyThreshold());
    }

    @Test
    void eachGoalAndMetricIsAnIndependentLimit() {
        Goal minutes = goal(5, 10, null);
        Goal entries = goal(5, null, 4);

        List<GoalAlertPolicy.Plan> plans = plan(List.of(
                progress(minutes, Duration.ofMinutes(8).toMillis(), 4, GoalState.NEAR),
                progress(entries, Duration.ofMinutes(8).toMillis(), 4, GoalState.EXCEEDED)), List.of());

        assertEquals(2, plans.size());
        assertEquals(List.of(80), plans.get(0).thresholds());
        assertEquals(List.of(80, 100), plans.get(1).thresholds());
    }

    private static List<GoalAlertPolicy.Plan> plan(List<GoalProgress> progress, List<GoalAlert> alerts) {
        return GoalAlertPolicy.plan(progress, alerts);
    }

    private static List<GoalProgress> seconds(Goal goal, long seconds) {
        long limitSeconds = goal.dailyMinutes() * 60L;
        return List.of(progress(goal, Duration.ofSeconds(seconds).toMillis(), 0,
                seconds >= limitSeconds ? GoalState.EXCEEDED
                        : seconds * 100 >= limitSeconds * 80 ? GoalState.NEAR
                        : GoalState.IN_PROGRESS));
    }

    private static List<GoalProgress> entries(Goal goal, int entries) {
        return List.of(progress(goal, 0, entries,
                entries >= goal.dailyEntries() ? GoalState.EXCEEDED
                        : (long) entries * 100 >= (long) goal.dailyEntries() * 80 ? GoalState.NEAR
                        : GoalState.IN_PROGRESS));
    }

    private static GoalProgress progress(Goal goal, long usedMillis, int entries, GoalState state) {
        return new GoalProgress(goal, usedMillis, entries, state, false);
    }

    private static Goal goal(long id, Integer minutes, Integer entries) {
        return new Goal(id, CODE, minutes, entries, false, true, NOW, NOW);
    }

    private static GoalAlert alert(Goal goal, int threshold, GoalMetric metric) {
        return new GoalAlert(1, goal.id(), NOW, threshold, metric, null, null);
    }
}
