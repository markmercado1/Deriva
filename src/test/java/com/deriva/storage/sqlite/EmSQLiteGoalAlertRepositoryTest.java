package com.deriva.storage.sqlite;

import com.deriva.goals.model.EmGoal;
import com.deriva.goals.model.EmGoalAlert;
import com.deriva.goals.model.EmGoalMetric;
import com.deriva.storage.EmStorageException;
import com.deriva.tracking.model.EmContextKey;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class EmSQLiteGoalAlertRepositoryTest {
    private static final Instant T0 = Instant.parse("2026-11-24T09:00:00Z");
    private static final Clock CLOCK = Clock.fixed(T0, ZoneOffset.UTC);

    @TempDir Path tempDirectory;

    private Path database() {
        return tempDirectory.resolve("alerts.db");
    }

    private EmGoal goal(String process, String host) {
        return new EmSQLiteGoalRepository(database(), CLOCK)
                .create(EmGoal.draft(new EmContextKey(process, host), 30, null, false));
    }

    @Test
    void createsUpdatesListsAndReopensAlerts() {
        EmGoal siteGoal = goal("chrome.exe", "youtube.com");
        EmGoal appGoal = goal("code.exe", null);
        EmSQLiteGoalAlertRepository alerts = new EmSQLiteGoalAlertRepository(database());

        EmGoalAlert first = alerts.create(EmGoalAlert.draft(siteGoal.id(), T0, 80, EmGoalMetric.MINUTES));
        EmGoalAlert second = alerts.create(EmGoalAlert.draft(siteGoal.id(), T0.plusSeconds(600), 100, EmGoalMetric.MINUTES));
        alerts.create(EmGoalAlert.draft(appGoal.id(), T0.plusSeconds(60), 100, EmGoalMetric.ENTRIES));

        assertTrue(first.id() > 0);
        assertEquals(List.of(first.id(), second.id()),
                alerts.listByGoal(siteGoal.id()).stream().map(EmGoalAlert::id).toList());
        assertEquals(3, alerts.listPending().size());

        EmGoalAlert reviewed = new EmGoalAlert(first.id(), first.goalId(), first.triggeredAt(),
                first.thresholdPercent(), first.metric(), "IGNORED", "Revisado con el usuario");
        alerts.update(reviewed);

        assertEquals(reviewed, alerts.find(first.id()).orElseThrow());
        assertEquals(2, alerts.listPending().size());
        assertEquals(reviewed, new EmSQLiteGoalAlertRepository(database()).find(first.id()).orElseThrow());
    }

    @Test
    void deletingGoalRemovesItFromActiveAndPendingButPreservesItsAlerts() {
        EmGoal removed = goal("chrome.exe", "youtube.com");
        EmGoal kept = goal("code.exe", null);
        EmSQLiteGoalAlertRepository alerts = new EmSQLiteGoalAlertRepository(database());
        alerts.create(EmGoalAlert.draft(removed.id(), T0, 100, EmGoalMetric.MINUTES));
        alerts.create(EmGoalAlert.draft(removed.id(), T0.plusSeconds(60), 100, EmGoalMetric.ENTRIES));
        alerts.create(EmGoalAlert.draft(kept.id(), T0, 80, EmGoalMetric.MINUTES));
        EmSQLiteGoalRepository goals = new EmSQLiteGoalRepository(database(), CLOCK);

        assertTrue(goals.delete(removed.id()));

        assertTrue(goals.find(removed.id()).isEmpty());
        assertTrue(goals.listActive().stream().noneMatch(goal -> goal.id() == removed.id()));
        assertEquals(2, alerts.listByGoal(removed.id()).size());
        assertEquals(1, alerts.listByGoal(kept.id()).size());
        assertEquals(1, alerts.listPending().size());
        assertEquals(kept.id(), alerts.listPending().getFirst().goalId());
    }

    @Test
    void deletesSingleAlert() {
        EmGoal siteGoal = goal("chrome.exe", "youtube.com");
        EmSQLiteGoalAlertRepository alerts = new EmSQLiteGoalAlertRepository(database());
        EmGoalAlert alert = alerts.create(EmGoalAlert.draft(siteGoal.id(), T0, 100, EmGoalMetric.MINUTES));

        assertTrue(alerts.delete(alert.id()));
        assertTrue(alerts.find(alert.id()).isEmpty());
        assertFalse(alerts.delete(alert.id()));
    }

    @Test
    void rejectsAlertForUnknownGoal() {
        EmSQLiteGoalAlertRepository alerts = new EmSQLiteGoalAlertRepository(database());
        assertThrows(EmStorageException.class,
                () -> alerts.create(EmGoalAlert.draft(42, T0, 100, EmGoalMetric.ENTRIES)));
    }

    @Test
    void rejectsJustificationLongerThanFiveHundredCharacters() throws Exception {
        EmGoal siteGoal = goal("chrome.exe", "youtube.com");
        EmSQLiteGoalAlertRepository alerts = new EmSQLiteGoalAlertRepository(database());
        EmGoalAlert alert = alerts.create(EmGoalAlert.draft(siteGoal.id(), T0, 100, EmGoalMetric.MINUTES));

        String tooLong = "x".repeat(EmGoalAlert.MAX_JUSTIFICATION_LENGTH + 1);
        assertThrows(IllegalArgumentException.class,
                () -> new EmGoalAlert(alert.id(), alert.goalId(), alert.triggeredAt(),
                        alert.thresholdPercent(), alert.metric(), null, tooLong));

        try (Connection connection = EmSQLiteSchema.open(database());
             PreparedStatement statement = connection.prepareStatement(
                     "UPDATE goal_alerts SET justification = ? WHERE id = ?")) {
            statement.setString(1, tooLong);
            statement.setLong(2, alert.id());
            assertThrows(SQLException.class, statement::executeUpdate);
        }
    }

    @Test
    void storesReopensAndClearsJustification() {
        EmGoal goal = goal("code.exe", null);
        EmSQLiteGoalAlertRepository alerts = new EmSQLiteGoalAlertRepository(database());
        EmGoalAlert alert = alerts.create(EmGoalAlert.draft(goal.id(), T0, 100, EmGoalMetric.MINUTES));
        Instant justAt = T0.plusSeconds(60);

        alerts.update(alert.justified(600_000, justAt, "Motivo tal cual"));

        EmGoalAlert stored = new EmSQLiteGoalAlertRepository(database()).find(alert.id()).orElseThrow();
        assertTrue(stored.isJustified());
        assertEquals(600_000L, stored.justifiedExcess().longValue());
        assertEquals(justAt, stored.justifiedAt());
        assertEquals("Motivo tal cual", stored.justification());
        assertEquals(List.of(stored), alerts.listBetween(T0, T0.plusSeconds(1)));
        assertEquals(List.of(stored),
                alerts.listJustifiedBetween(justAt, justAt.plusSeconds(1)));

        alerts.updateAll(List.of(stored.unjustified()));
        EmGoalAlert cleared = alerts.find(alert.id()).orElseThrow();
        assertFalse(cleared.isJustified());
        assertNull(cleared.justifiedAt());
        assertNull(cleared.justifiedExcess());
        assertNull(cleared.justification());
        assertEquals(1, alerts.listBetween(T0, T0.plusSeconds(1)).size());
        assertTrue(alerts.listJustifiedBetween(justAt, justAt.plusSeconds(1)).isEmpty());
    }

    @Test
    void rejectsIncompleteOrNegativeJustificationData() {
        EmGoal goal = goal("code.exe", null);
        assertThrows(IllegalArgumentException.class,
                () -> new EmGoalAlert(1, goal.id(), T0, 100, EmGoalMetric.MINUTES,
                        null, null, T0, null));
        assertThrows(IllegalArgumentException.class,
                () -> new EmGoalAlert(1, goal.id(), T0, 100, EmGoalMetric.MINUTES,
                        null, null, null, 1L));
        assertThrows(IllegalArgumentException.class,
                () -> new EmGoalAlert(1, goal.id(), T0, 100, EmGoalMetric.MINUTES,
                        null, null, T0, -1L));
        assertThrows(IllegalArgumentException.class,
                () -> new EmGoalAlert(1, goal.id(), T0, 80, EmGoalMetric.MINUTES,
                        null, null, T0, 1L));
    }

    @Test
    void storesReopensAndResolvesDeliveryAndObservationData() {
        EmGoal goal = goal("code.exe", null);
        EmSQLiteGoalAlertRepository alerts = new EmSQLiteGoalAlertRepository(database());
        EmGoalAlert alert = alerts.create(EmGoalAlert.draft(goal.id(), T0, 80, EmGoalMetric.MINUTES));

        EmGoalAlert delivered = alert.notified(T0.plusSeconds(30), T0.plusSeconds(150));
        alerts.update(delivered);
        EmGoalAlert stored = new EmSQLiteGoalAlertRepository(database()).find(alert.id()).orElseThrow();
        assertEquals(T0.plusSeconds(30), stored.notifiedAt());
        assertEquals(T0.plusSeconds(150), stored.observedUntil());
        assertTrue(stored.isObserving());

        EmGoalAlert resolved = stored.observed(EmGoalAlert.OUTCOME_STABLE, T0.plusSeconds(150), 120_000L);
        alerts.update(resolved);
        EmGoalAlert reopened = new EmSQLiteGoalAlertRepository(database()).find(alert.id()).orElseThrow();
        assertEquals(EmGoalAlert.OUTCOME_STABLE, reopened.outcome());
        assertEquals(T0.plusSeconds(150), reopened.outcomeAt());
        assertEquals(120_000L, reopened.outcomeAfterMs());
        assertFalse(reopened.isObserving());
        assertFalse(reopened.isPendingNotice());
    }

    @Test
    void rejectsInvalidThresholdInTheDatabase() throws Exception {
        EmGoal siteGoal = goal("chrome.exe", "youtube.com");
        try (Connection connection = EmSQLiteSchema.open(database());
             PreparedStatement statement = connection.prepareStatement("""
                     INSERT INTO goal_alerts(goal_id, triggered_at, threshold_percent, metric)
                     VALUES (?, 1, 101, 'MINUTES')
                     """)) {
            statement.setLong(1, siteGoal.id());
            assertThrows(SQLException.class, statement::executeUpdate);
        }
    }
}
