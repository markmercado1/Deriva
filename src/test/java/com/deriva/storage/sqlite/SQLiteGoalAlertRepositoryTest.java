package com.deriva.storage.sqlite;

import com.deriva.goals.model.Goal;
import com.deriva.goals.model.GoalAlert;
import com.deriva.goals.model.GoalMetric;
import com.deriva.storage.StorageException;
import com.deriva.tracking.model.ContextKey;
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

class SQLiteGoalAlertRepositoryTest {
    private static final Instant T0 = Instant.parse("2026-11-24T09:00:00Z");
    private static final Clock CLOCK = Clock.fixed(T0, ZoneOffset.UTC);

    @TempDir Path tempDirectory;

    private Path database() {
        return tempDirectory.resolve("alerts.db");
    }

    private Goal goal(String process, String host) {
        return new SQLiteGoalRepository(database(), CLOCK)
                .create(Goal.draft(new ContextKey(process, host), 30, null, false));
    }

    @Test
    void createsUpdatesListsAndReopensAlerts() {
        Goal siteGoal = goal("chrome.exe", "youtube.com");
        Goal appGoal = goal("code.exe", null);
        SQLiteGoalAlertRepository alerts = new SQLiteGoalAlertRepository(database());

        GoalAlert first = alerts.create(GoalAlert.draft(siteGoal.id(), T0, 80, GoalMetric.MINUTES));
        GoalAlert second = alerts.create(GoalAlert.draft(siteGoal.id(), T0.plusSeconds(600), 100, GoalMetric.MINUTES));
        alerts.create(GoalAlert.draft(appGoal.id(), T0.plusSeconds(60), 100, GoalMetric.ENTRIES));

        assertTrue(first.id() > 0);
        assertEquals(List.of(first.id(), second.id()),
                alerts.listByGoal(siteGoal.id()).stream().map(GoalAlert::id).toList());
        assertEquals(3, alerts.listPending().size());

        GoalAlert reviewed = new GoalAlert(first.id(), first.goalId(), first.triggeredAt(),
                first.thresholdPercent(), first.metric(), "IGNORED", "Revisado con el usuario");
        alerts.update(reviewed);

        assertEquals(reviewed, alerts.find(first.id()).orElseThrow());
        assertEquals(2, alerts.listPending().size());
        assertEquals(reviewed, new SQLiteGoalAlertRepository(database()).find(first.id()).orElseThrow());
    }

    @Test
    void deletingGoalCascadesToItsAlertsOnly() {
        Goal removed = goal("chrome.exe", "youtube.com");
        Goal kept = goal("code.exe", null);
        SQLiteGoalAlertRepository alerts = new SQLiteGoalAlertRepository(database());
        alerts.create(GoalAlert.draft(removed.id(), T0, 100, GoalMetric.MINUTES));
        alerts.create(GoalAlert.draft(removed.id(), T0.plusSeconds(60), 100, GoalMetric.ENTRIES));
        alerts.create(GoalAlert.draft(kept.id(), T0, 80, GoalMetric.MINUTES));

        assertTrue(new SQLiteGoalRepository(database(), CLOCK).delete(removed.id()));

        assertTrue(alerts.listByGoal(removed.id()).isEmpty());
        assertEquals(1, alerts.listByGoal(kept.id()).size());
        assertEquals(1, alerts.listPending().size());
    }

    @Test
    void deletesSingleAlert() {
        Goal siteGoal = goal("chrome.exe", "youtube.com");
        SQLiteGoalAlertRepository alerts = new SQLiteGoalAlertRepository(database());
        GoalAlert alert = alerts.create(GoalAlert.draft(siteGoal.id(), T0, 100, GoalMetric.MINUTES));

        assertTrue(alerts.delete(alert.id()));
        assertTrue(alerts.find(alert.id()).isEmpty());
        assertFalse(alerts.delete(alert.id()));
    }

    @Test
    void rejectsAlertForUnknownGoal() {
        SQLiteGoalAlertRepository alerts = new SQLiteGoalAlertRepository(database());
        assertThrows(StorageException.class,
                () -> alerts.create(GoalAlert.draft(42, T0, 100, GoalMetric.ENTRIES)));
    }

    @Test
    void rejectsJustificationLongerThanFiveHundredCharacters() throws Exception {
        Goal siteGoal = goal("chrome.exe", "youtube.com");
        SQLiteGoalAlertRepository alerts = new SQLiteGoalAlertRepository(database());
        GoalAlert alert = alerts.create(GoalAlert.draft(siteGoal.id(), T0, 100, GoalMetric.MINUTES));

        String tooLong = "x".repeat(GoalAlert.MAX_JUSTIFICATION_LENGTH + 1);
        assertThrows(IllegalArgumentException.class,
                () -> new GoalAlert(alert.id(), alert.goalId(), alert.triggeredAt(),
                        alert.thresholdPercent(), alert.metric(), null, tooLong));

        try (Connection connection = SQLiteSchema.open(database());
             PreparedStatement statement = connection.prepareStatement(
                     "UPDATE goal_alerts SET justification = ? WHERE id = ?")) {
            statement.setString(1, tooLong);
            statement.setLong(2, alert.id());
            assertThrows(SQLException.class, statement::executeUpdate);
        }
    }

    @Test
    void storesReopensAndClearsJustification() {
        Goal goal = goal("code.exe", null);
        SQLiteGoalAlertRepository alerts = new SQLiteGoalAlertRepository(database());
        GoalAlert alert = alerts.create(GoalAlert.draft(goal.id(), T0, 100, GoalMetric.MINUTES));
        Instant justAt = T0.plusSeconds(60);

        alerts.update(alert.justified(600_000, justAt, "Motivo tal cual"));

        GoalAlert stored = new SQLiteGoalAlertRepository(database()).find(alert.id()).orElseThrow();
        assertTrue(stored.isJustified());
        assertEquals(600_000L, stored.justifiedExcess().longValue());
        assertEquals(justAt, stored.justifiedAt());
        assertEquals("Motivo tal cual", stored.justification());
        assertEquals(List.of(stored), alerts.listBetween(T0, T0.plusSeconds(1)));
        assertEquals(List.of(stored),
                alerts.listJustifiedBetween(justAt, justAt.plusSeconds(1)));

        alerts.updateAll(List.of(stored.unjustified()));
        GoalAlert cleared = alerts.find(alert.id()).orElseThrow();
        assertFalse(cleared.isJustified());
        assertNull(cleared.justifiedAt());
        assertNull(cleared.justifiedExcess());
        assertNull(cleared.justification());
        assertEquals(1, alerts.listBetween(T0, T0.plusSeconds(1)).size());
        assertTrue(alerts.listJustifiedBetween(justAt, justAt.plusSeconds(1)).isEmpty());
    }

    @Test
    void rejectsIncompleteOrNegativeJustificationData() {
        Goal goal = goal("code.exe", null);
        assertThrows(IllegalArgumentException.class,
                () -> new GoalAlert(1, goal.id(), T0, 100, GoalMetric.MINUTES,
                        null, null, T0, null));
        assertThrows(IllegalArgumentException.class,
                () -> new GoalAlert(1, goal.id(), T0, 100, GoalMetric.MINUTES,
                        null, null, null, 1L));
        assertThrows(IllegalArgumentException.class,
                () -> new GoalAlert(1, goal.id(), T0, 100, GoalMetric.MINUTES,
                        null, null, T0, -1L));
        assertThrows(IllegalArgumentException.class,
                () -> new GoalAlert(1, goal.id(), T0, 80, GoalMetric.MINUTES,
                        null, null, T0, 1L));
    }

    @Test
    void storesReopensAndResolvesDeliveryAndObservationData() {
        Goal goal = goal("code.exe", null);
        SQLiteGoalAlertRepository alerts = new SQLiteGoalAlertRepository(database());
        GoalAlert alert = alerts.create(GoalAlert.draft(goal.id(), T0, 80, GoalMetric.MINUTES));

        GoalAlert delivered = alert.notified(T0.plusSeconds(30), T0.plusSeconds(150));
        alerts.update(delivered);
        GoalAlert stored = new SQLiteGoalAlertRepository(database()).find(alert.id()).orElseThrow();
        assertEquals(T0.plusSeconds(30), stored.notifiedAt());
        assertEquals(T0.plusSeconds(150), stored.observedUntil());
        assertTrue(stored.isObserving());

        GoalAlert resolved = stored.observed(GoalAlert.OUTCOME_STABLE, T0.plusSeconds(150), 120_000L);
        alerts.update(resolved);
        GoalAlert reopened = new SQLiteGoalAlertRepository(database()).find(alert.id()).orElseThrow();
        assertEquals(GoalAlert.OUTCOME_STABLE, reopened.outcome());
        assertEquals(T0.plusSeconds(150), reopened.outcomeAt());
        assertEquals(120_000L, reopened.outcomeAfterMs());
        assertFalse(reopened.isObserving());
        assertFalse(reopened.isPendingNotice());
    }

    @Test
    void rejectsInvalidThresholdInTheDatabase() throws Exception {
        Goal siteGoal = goal("chrome.exe", "youtube.com");
        try (Connection connection = SQLiteSchema.open(database());
             PreparedStatement statement = connection.prepareStatement("""
                     INSERT INTO goal_alerts(goal_id, triggered_at, threshold_percent, metric)
                     VALUES (?, 1, 101, 'MINUTES')
                     """)) {
            statement.setLong(1, siteGoal.id());
            assertThrows(SQLException.class, statement::executeUpdate);
        }
    }
}
