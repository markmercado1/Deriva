package com.deriva.app.usecase;

import com.deriva.goals.OpenSegmentProvider;
import com.deriva.goals.model.Goal;
import com.deriva.goals.model.GoalAlert;
import com.deriva.privacy.ObservationKind;
import com.deriva.storage.port.GoalAlertRepository;
import com.deriva.storage.port.GoalRepository;
import com.deriva.storage.port.SegmentRepository;
import com.deriva.tracking.model.ContextKey;
import com.deriva.tracking.model.Segment;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GoalAlertMonitorTest {
    private static final Instant T0 = Instant.parse("2026-11-24T10:00:00Z");
    private static final OpenSegmentProvider NO_OPEN = now -> Optional.empty();

    @Test
    void doesNotEvaluateWhilePausedIdleLockedOrSuspended() {
        CountingGoals goals = new CountingGoals();
        GoalAlertMonitor monitor = monitor(goals, Duration.ofSeconds(30));

        assertFalse(monitor.tick(CaptureState.PAUSED, ObservationKind.OBSERVED, T0, NO_OPEN));
        assertFalse(monitor.tick(CaptureState.RUNNING, ObservationKind.UNAVAILABLE, T0, NO_OPEN));
        assertFalse(monitor.tick(CaptureState.RUNNING, ObservationKind.IDLE, T0, NO_OPEN));
        assertFalse(monitor.tick(CaptureState.RUNNING, ObservationKind.EXCLUDED, T0, NO_OPEN));
        assertEquals(0, goals.calls);

        assertTrue(monitor.tick(CaptureState.RUNNING, ObservationKind.OBSERVED, T0, NO_OPEN));
        assertEquals(1, goals.calls);
    }

    @Test
    void evaluatesAtMostOnceEveryThirtySeconds() {
        CountingGoals goals = new CountingGoals();
        GoalAlertMonitor monitor = monitor(goals, Duration.ofSeconds(30));

        assertTrue(monitor.tick(CaptureState.RUNNING, ObservationKind.OBSERVED, T0, NO_OPEN));
        assertFalse(monitor.tick(CaptureState.RUNNING, ObservationKind.OBSERVED,
                T0.plusSeconds(29), NO_OPEN));
        assertEquals(1, goals.calls);

        assertTrue(monitor.tick(CaptureState.RUNNING, ObservationKind.OBSERVED,
                T0.plusSeconds(30), NO_OPEN));
        assertEquals(2, goals.calls);
    }

    @Test
    void repositoryFailureDoesNotEscapeTheLoopAndRetriesOnTheNextCycle() {
        CountingGoals goals = new CountingGoals();
        goals.failure = new IllegalStateException("database unavailable");
        GoalAlertMonitor monitor = monitor(goals, Duration.ofSeconds(30));

        assertFalse(monitor.tick(CaptureState.RUNNING, ObservationKind.OBSERVED, T0, NO_OPEN));
        assertEquals(1, goals.calls);

        goals.failure = null;
        assertFalse(monitor.tick(CaptureState.RUNNING, ObservationKind.OBSERVED,
                T0.plusSeconds(29), NO_OPEN));
        assertEquals(1, goals.calls);

        assertTrue(monitor.tick(CaptureState.RUNNING, ObservationKind.OBSERVED,
                T0.plusSeconds(30), NO_OPEN));
        assertEquals(2, goals.calls);
    }

    private static GoalAlertMonitor monitor(CountingGoals goals, Duration interval) {
        GoalAlertService service = new GoalAlertService(goals, new NoSegments(),
                new NoAlerts(), notice -> { }, Clock.fixed(T0, ZoneOffset.UTC), ZoneOffset.UTC);
        return new GoalAlertMonitor(service, interval);
    }

    private static final class CountingGoals implements GoalRepository {
        private int calls;
        private RuntimeException failure;

        @Override
        public List<Goal> listActive() {
            calls++;
            if (failure != null) throw failure;
            return List.of();
        }

        @Override
        public Goal create(Goal goal) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Optional<Goal> find(long id) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void update(Goal goal) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean delete(long id) {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<Goal> listAll() {
            throw new UnsupportedOperationException();
        }
    }

    private static final class NoSegments implements SegmentRepository {
        @Override
        public void saveAll(List<Segment> segments) {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<Segment> between(Instant from, Instant to) {
            return List.of();
        }

        @Override
        public List<ContextKey> observedContexts() {
            return List.of();
        }
    }

    private static final class NoAlerts implements GoalAlertRepository {
        @Override
        public GoalAlert create(GoalAlert alert) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Optional<GoalAlert> find(long id) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void update(GoalAlert alert) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean delete(long id) {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<GoalAlert> listByGoal(long goalId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<GoalAlert> listPending() {
            throw new UnsupportedOperationException();
        }
    }
}
