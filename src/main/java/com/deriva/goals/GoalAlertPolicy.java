package com.deriva.goals;

import com.deriva.goals.model.Goal;
import com.deriva.goals.model.GoalAlert;
import com.deriva.goals.model.GoalMetric;
import com.deriva.goals.model.GoalProgress;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Pure decision of which goal thresholds must be recorded and announced.
 *
 * <p>Each threshold (80 % and 100 %) is independent per goal and metric. A threshold
 * already recorded for the local day is never announced again, even after restarting
 * the app or raising the limit during the same day. When both thresholds are reached
 * in the same evaluation, both are recorded but only the highest one is announced.
 * The policy knows nothing about SQLite, clocks, threads or the tray.</p>
 */
public final class GoalAlertPolicy {
    /** Percentage at which a goal reaches its full limit. */
    public static final int FULL_PERCENT = 100;

    /**
     * One notification decision. {@code used} is milliseconds for {@link GoalMetric#MINUTES}
     * and a visit count for {@link GoalMetric#ENTRIES}; {@code limit} uses the same unit.
     */
    public record Plan(Goal goal, GoalMetric metric, List<Integer> thresholds,
                       int notifyThreshold, long used, long limit) {
        public Plan {
            Objects.requireNonNull(goal, "goal");
            Objects.requireNonNull(metric, "metric");
            thresholds = List.copyOf(Objects.requireNonNull(thresholds, "thresholds"));
            if (thresholds.isEmpty()) throw new IllegalArgumentException("thresholds");
            if (!thresholds.contains(notifyThreshold)) throw new IllegalArgumentException("notifyThreshold");
            if (used < 0) throw new IllegalArgumentException("used");
            if (limit <= 0) throw new IllegalArgumentException("limit");
        }
    }

    private GoalAlertPolicy() {
    }

    /**
     * Returns one plan per goal and metric that has at least one new threshold today,
     * preserving the input order. The listed thresholds must be stored before the
     * notification; {@code notifyThreshold} is the highest one.
     */
    public static List<Plan> plan(List<GoalProgress> progress, List<GoalAlert> alertsToday) {
        Objects.requireNonNull(progress, "progress");
        Objects.requireNonNull(alertsToday, "alertsToday");
        Set<Key> recorded = new HashSet<>();
        for (GoalAlert alert : alertsToday) {
            recorded.add(new Key(alert.goalId(), alert.metric(), alert.thresholdPercent()));
        }

        List<Plan> plans = new ArrayList<>();
        for (GoalProgress item : progress) {
            Objects.requireNonNull(item, "progress item");
            Goal goal = item.goal();
            if (goal.dailyMinutes() != null) {
                add(plans, recorded, goal, GoalMetric.MINUTES, item.usedMillis(),
                        goal.dailyMinutes() * 60_000L);
            }
            if (goal.dailyEntries() != null) {
                add(plans, recorded, goal, GoalMetric.ENTRIES, item.entries(),
                        (long) goal.dailyEntries());
            }
        }
        return List.copyOf(plans);
    }

    private static void add(List<Plan> plans, Set<Key> recorded, Goal goal, GoalMetric metric,
                            long used, long limit) {
        List<Integer> thresholds = new ArrayList<>(2);
        int near = GoalProgressCalculator.NEAR_PERCENT;
        if (reached(used, limit, near) && !recorded.contains(new Key(goal.id(), metric, near)))
            thresholds.add(near);
        if (reached(used, limit, FULL_PERCENT)
                && !recorded.contains(new Key(goal.id(), metric, FULL_PERCENT)))
            thresholds.add(FULL_PERCENT);
        if (thresholds.isEmpty()) return;
        plans.add(new Plan(goal, metric, thresholds,
                thresholds.get(thresholds.size() - 1), used, limit));
    }

    private static boolean reached(long used, long limit, int threshold) {
        return used * 100L >= limit * threshold;
    }

    private record Key(long goalId, GoalMetric metric, int threshold) {
    }
}
