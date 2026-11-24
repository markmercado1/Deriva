package com.deriva.app.usecase;

import com.deriva.analysis.DayWindow;
import com.deriva.goals.GoalProgressCalculator;
import com.deriva.goals.PassiveGoalProvider;
import com.deriva.goals.PassiveGoalSnapshot;
import com.deriva.goals.model.Goal;
import com.deriva.goals.model.GoalProgress;
import com.deriva.storage.port.GoalRepository;
import com.deriva.storage.port.SegmentRepository;
import com.deriva.tracking.model.ContextKey;
import com.deriva.tracking.model.Segment;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Objects;

/**
 * Application use case for daily goals. It publishes an immutable active-goal
 * snapshot so the capture thread can reload passive goals without touching SQL
 * or the UI.
 */
public final class GoalsService implements PassiveGoalProvider {
    private final GoalRepository goals;
    private final SegmentRepository segments;
    private final Clock clock;
    private volatile PassiveGoalSnapshot passiveSnapshot;

    public GoalsService(GoalRepository goals, SegmentRepository segments, Clock clock) {
        this.goals = Objects.requireNonNull(goals, "goals");
        this.segments = Objects.requireNonNull(segments, "segments");
        this.clock = Objects.requireNonNull(clock, "clock");
        this.passiveSnapshot = new PassiveGoalSnapshot(1, goals.listActive());
    }

    @Override
    public PassiveGoalSnapshot snapshot() {
        return passiveSnapshot;
    }

    public List<Goal> listAll() {
        return goals.listAll();
    }

    public List<ContextKey> observedContexts() {
        return segments.observedContexts();
    }

    public synchronized Goal create(Goal goal) {
        Objects.requireNonNull(goal, "goal");
        if (goal.id() != 0) throw new IllegalArgumentException("new goal must not have an id");
        Goal created = goals.create(goal);
        publish();
        return created;
    }

    public synchronized void update(Goal goal) {
        Objects.requireNonNull(goal, "goal");
        goals.update(goal);
        publish();
    }

    public synchronized void setActive(long id, boolean active) {
        Goal goal = goals.find(id).orElseThrow(
                () -> new IllegalArgumentException("Unknown goal: " + id));
        if (goal.active() == active) return;
        goals.update(new Goal(goal.id(), goal.context(), goal.dailyMinutes(), goal.dailyEntries(),
                goal.passive(), active, goal.createdAt(), goal.updatedAt()));
        publish();
    }

    public synchronized boolean delete(long id) {
        boolean deleted = goals.delete(id);
        if (deleted) publish();
        return deleted;
    }

    public List<GoalProgress> progressForDay(LocalDate day, ZoneId zone) {
        Objects.requireNonNull(day, "day");
        Objects.requireNonNull(zone, "zone");
        DayWindow window = DayWindow.of(day, zone);
        List<Segment> segmentsInDay = segments.between(window.start(), window.end());
        return GoalProgressCalculator.calculate(day, zone, segmentsInDay,
                passiveSnapshot.activeGoals());
    }

    public LocalDate today(ZoneId zone) {
        return clock.instant().atZone(zone).toLocalDate();
    }

    private void publish() {
        passiveSnapshot = new PassiveGoalSnapshot(passiveSnapshot.revision() + 1,
                goals.listActive());
    }
}
