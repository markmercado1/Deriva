package com.deriva.app.usecase;

import com.deriva.goals.EmOpenSegmentProvider;
import com.deriva.goals.model.EmGoal;
import com.deriva.goals.model.EmGoalAlert;
import com.deriva.privacy.EmObservationKind;
import com.deriva.storage.port.EmGoalAlertRepository;
import com.deriva.storage.port.EmGoalRepository;
import com.deriva.storage.port.EmSegmentRepository;
import com.deriva.tracking.model.EmContextKey;
import com.deriva.tracking.model.EmSegment;
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

class EmGoalAlertMonitorTest {
    private static final Instant T0 = Instant.parse("2026-11-24T10:00:00Z");
    private static final EmOpenSegmentProvider NO_OPEN = now -> Optional.empty();

    @Test
    void doesNotEvaluateWhilePausedIdleLockedOrSuspended() {
        CountingGoals goals = new CountingGoals();
        EmGoalAlertMonitor monitor = monitor(goals, Duration.ofSeconds(30));

        assertFalse(monitor.tick(EmCaptureState.PAUSED, EmObservationKind.OBSERVED, T0, NO_OPEN));
        assertFalse(monitor.tick(EmCaptureState.RUNNING, EmObservationKind.UNAVAILABLE, T0, NO_OPEN));
        assertFalse(monitor.tick(EmCaptureState.RUNNING, EmObservationKind.IDLE, T0, NO_OPEN));
        assertFalse(monitor.tick(EmCaptureState.RUNNING, EmObservationKind.EXCLUDED, T0, NO_OPEN));
        assertEquals(0, goals.calls);

        assertTrue(monitor.tick(EmCaptureState.RUNNING, EmObservationKind.OBSERVED, T0, NO_OPEN));
        assertEquals(1, goals.calls);
    }

    @Test
    void evaluatesAtMostOnceEveryThirtySeconds() {
        CountingGoals goals = new CountingGoals();
        EmGoalAlertMonitor monitor = monitor(goals, Duration.ofSeconds(30));

        assertTrue(monitor.tick(EmCaptureState.RUNNING, EmObservationKind.OBSERVED, T0, NO_OPEN));
        assertFalse(monitor.tick(EmCaptureState.RUNNING, EmObservationKind.OBSERVED,
                T0.plusSeconds(29), NO_OPEN));
        assertEquals(1, goals.calls);

        assertTrue(monitor.tick(EmCaptureState.RUNNING, EmObservationKind.OBSERVED,
                T0.plusSeconds(30), NO_OPEN));
        assertEquals(2, goals.calls);
    }

    @Test
    void repositoryFailureDoesNotEscapeTheLoopAndRetriesOnTheNextCycle() {
        CountingGoals goals = new CountingGoals();
        goals.failure = new IllegalStateException("database unavailable");
        EmGoalAlertMonitor monitor = monitor(goals, Duration.ofSeconds(30));

        assertFalse(monitor.tick(EmCaptureState.RUNNING, EmObservationKind.OBSERVED, T0, NO_OPEN));
        assertEquals(1, goals.calls);

        goals.failure = null;
        assertFalse(monitor.tick(EmCaptureState.RUNNING, EmObservationKind.OBSERVED,
                T0.plusSeconds(29), NO_OPEN));
        assertEquals(1, goals.calls);

        assertTrue(monitor.tick(EmCaptureState.RUNNING, EmObservationKind.OBSERVED,
                T0.plusSeconds(30), NO_OPEN));
        assertEquals(2, goals.calls);
    }

    private static EmGoalAlertMonitor monitor(CountingGoals goals, Duration interval) {
        EmGoalAlertService service = new EmGoalAlertService(goals, new NoSegments(),
                new NoAlerts(), notice -> { }, Clock.fixed(T0, ZoneOffset.UTC), ZoneOffset.UTC);
        return new EmGoalAlertMonitor(service, interval);
    }

    private static final class CountingGoals implements EmGoalRepository {
        private int calls;
        private RuntimeException failure;

        @Override
        public List<EmGoal> listActive() {
            calls++;
            if (failure != null) throw failure;
            return List.of();
        }

        @Override
        public EmGoal create(EmGoal goal) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Optional<EmGoal> find(long id) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void update(EmGoal goal) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean delete(long id) {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<EmGoal> listAll() {
            throw new UnsupportedOperationException();
        }
    }

    private static final class NoSegments implements EmSegmentRepository {
        @Override
        public void saveAll(List<EmSegment> segments) {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<EmSegment> between(Instant from, Instant to) {
            return List.of();
        }

        @Override
        public List<EmContextKey> observedContexts() {
            return List.of();
        }
    }

    private static final class NoAlerts implements EmGoalAlertRepository {
        @Override
        public EmGoalAlert create(EmGoalAlert alert) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Optional<EmGoalAlert> find(long id) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void update(EmGoalAlert alert) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void updateAll(List<EmGoalAlert> alerts) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean delete(long id) {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<EmGoalAlert> listByGoal(long goalId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<EmGoalAlert> listPending() {
            return List.of();
        }

        @Override
        public List<EmGoalAlert> listBetween(java.time.Instant from, java.time.Instant to) {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<EmGoalAlert> listJustifiedBetween(java.time.Instant from, java.time.Instant to) {
            throw new UnsupportedOperationException();
        }
    }
}
