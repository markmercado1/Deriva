package com.deriva.app.usecase;

import com.deriva.goals.model.Goal;
import com.deriva.goals.model.GoalAlert;
import com.deriva.goals.model.GoalMetric;
import com.deriva.goals.model.GoalProgress;
import com.deriva.goals.model.GoalState;
import com.deriva.privacy.ObservationKind;
import com.deriva.storage.sqlite.SQLiteGoalAlertRepository;
import com.deriva.storage.sqlite.SQLiteGoalRepository;
import com.deriva.storage.sqlite.SQLiteSegmentRepository;
import com.deriva.tracking.model.ContextKey;
import com.deriva.tracking.model.Segment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GoalsJustificationServiceTest {
    private static final ZoneOffset ZONE = ZoneOffset.UTC;
    private static final Instant NOW = Instant.parse("2026-11-24T12:00:00Z");
    private static final LocalDate DAY = LocalDate.of(2026, 11, 24);
    private static final ContextKey CODE = new ContextKey("code.exe", null);

    @TempDir Path tempDirectory;

    private Path database;
    private MutableClock clock;
    private SQLiteGoalRepository goals;
    private SQLiteSegmentRepository segments;
    private SQLiteGoalAlertRepository alerts;
    private GoalsService service;

    @BeforeEach
    void setUp() {
        database = tempDirectory.resolve("goals.db");
        clock = new MutableClock(NOW, ZONE);
        goals = new SQLiteGoalRepository(database, clock);
        segments = new SQLiteSegmentRepository(database);
        alerts = new SQLiteGoalAlertRepository(database);
        service = new GoalsService(goals, segments, alerts, clock);
    }

    @Test
    void coversTheOpenSegmentAndClosingItDoesNotBringTheExcessBack() {
        Goal goal = service.create(Goal.draft(CODE, 10, null, false));
        save("2026-11-24T09:00:00Z", "2026-11-24T09:08:00Z");
        Segment open = observed("code.exe", null,
                "2026-11-24T09:08:00Z", "2026-11-24T09:12:00Z");
        GoalAlertService alarm = new GoalAlertService(goals, segments, alerts,
                notice -> { }, clock, ZONE);
        GoalAlert alert = alarm.evaluate(NOW, now -> Optional.of(open)).stream()
                .filter(created -> created.thresholdPercent() == 100)
                .findFirst()
                .orElseThrow();

        service.justify(goal.id(), "Incluye el tramo abierto", ZONE, now -> Optional.of(open));

        assertEquals(Duration.ofMinutes(2).toMillis(),
                alerts.find(alert.id()).orElseThrow().justifiedExcess());
        assertEquals(GoalState.JUSTIFIED, progressOf(goal.id(), now -> Optional.of(open)).state());

        save("2026-11-24T09:08:00Z", "2026-11-24T09:12:00Z");
        GoalProgress closed = progressOf(goal.id(), now -> Optional.empty());

        assertEquals(Duration.ofMinutes(12).toMillis(), closed.usedMillis());
        assertEquals(GoalState.JUSTIFIED, closed.state());
    }

    @Test
    void aJustificationOnlyAppliesToTheDayItWasMarked() {
        Goal goal = service.create(Goal.draft(CODE, 10, null, false));
        save("2026-11-24T09:00:00Z", "2026-11-24T09:12:00Z");
        GoalAlert alert = alerts.create(GoalAlert.draft(goal.id(), NOW, 100, GoalMetric.MINUTES));
        Instant yesterday = Instant.parse("2026-11-23T12:00:00Z");
        alerts.update(alert.justified(Duration.ofMinutes(2).toMillis(), yesterday, "Ayer"));

        GoalProgress ignored = progressOf(goal.id(), now -> Optional.empty());

        assertEquals(GoalState.EXCEEDED, ignored.state());
        assertTrue(ignored.justifications().isEmpty());

        service.justify(goal.id(), "Hoy", ZONE, now -> Optional.empty());
        GoalProgress applied = progressOf(goal.id(), now -> Optional.empty());
        assertEquals(GoalState.JUSTIFIED, applied.state());
        assertEquals(NOW, alerts.find(alert.id()).orElseThrow().justifiedAt());
    }

    @Test
    void rejustifyReplacesCoverageAndSurvivesReopen() {
        Goal goal = service.create(Goal.draft(CODE, 10, null, false));
        save("2026-11-24T09:00:00Z", "2026-11-24T09:12:00Z");
        GoalAlert alert = alerts.create(GoalAlert.draft(goal.id(), NOW, 100, GoalMetric.MINUTES));
        service.justify(goal.id(), "Primera", ZONE, now -> Optional.empty());
        assertEquals(Duration.ofMinutes(2).toMillis(),
                alerts.find(alert.id()).orElseThrow().justifiedExcess());

        clock.set(Instant.parse("2026-11-24T12:05:00Z"));
        save("2026-11-24T12:00:00Z", "2026-11-24T12:03:00Z");
        service.justify(goal.id(), "Segunda", ZONE, now -> Optional.empty());

        GoalAlert stored = alerts.find(alert.id()).orElseThrow();
        assertEquals(Duration.ofMinutes(5).toMillis(), stored.justifiedExcess());
        assertEquals("Segunda", stored.justification());
        assertEquals(Instant.parse("2026-11-24T12:05:00Z"), stored.justifiedAt());

        SQLiteGoalAlertRepository reopenedAlerts = new SQLiteGoalAlertRepository(database);
        GoalsService reopened = new GoalsService(
                new SQLiteGoalRepository(database, clock),
                new SQLiteSegmentRepository(database), reopenedAlerts, clock);
        GoalProgress afterReopen = reopened.progressForDay(DAY, ZONE, now -> Optional.empty())
                .stream().filter(item -> item.goal().id() == goal.id()).findFirst().orElseThrow();
        assertEquals(GoalState.JUSTIFIED, afterReopen.state());
        assertEquals(Duration.ofMinutes(5).toMillis(),
                reopenedAlerts.find(alert.id()).orElseThrow().justifiedExcess());

        reopened.removeJustification(goal.id(), ZONE);
        GoalAlert cleared = reopenedAlerts.find(alert.id()).orElseThrow();
        assertFalse(cleared.isJustified());
        assertNull(cleared.justification());
        assertEquals(GoalState.EXCEEDED, reopened.progressForDay(DAY, ZONE, now -> Optional.empty())
                .stream().filter(item -> item.goal().id() == goal.id()).findFirst().orElseThrow().state());
    }

    @Test
    void justifyingAndRemovingLeaveActivityTablesByteIdentical() throws Exception {
        Goal goal = service.create(Goal.draft(CODE, 10, null, false));
        save("2026-11-24T09:00:00Z", "2026-11-24T09:12:00Z");
        alerts.create(GoalAlert.draft(goal.id(), NOW, 100, GoalMetric.MINUTES));
        insertDriftRows();

        String before = activityFingerprint();
        service.justify(goal.id(), "Solo una anotaciÃ³n", ZONE, now -> Optional.empty());
        assertEquals(before, activityFingerprint());
        service.removeJustification(goal.id(), ZONE);
        assertEquals(before, activityFingerprint());
    }

    private GoalProgress progressOf(long goalId, com.deriva.goals.OpenSegmentProvider openSegments) {
        return service.progressForDay(DAY, ZONE, openSegments).stream()
                .filter(item -> item.goal().id() == goalId)
                .findFirst()
                .orElseThrow();
    }

    private void save(String start, String end) {
        segments.saveAll(List.of(observed("code.exe", null, start, end)));
    }

    private static Segment observed(String process, String host, String start, String end) {
        return new Segment(0, ObservationKind.OBSERVED, new ContextKey(process, host),
                null, null, Instant.parse(start), Instant.parse(end));
    }

    private void insertDriftRows() throws Exception {
        try (Connection connection = connect(); Statement statement = connection.createStatement()) {
            statement.executeUpdate("""
                    INSERT INTO drift_event(id, start_ts, detected_at, last_observed_ts, status,
                                            switch_count, first_change_process)
                    VALUES (1, 1000, 1200, 5000, 'OPEN', 10, 'code.exe')
                    """);
            statement.executeUpdate(
                    "INSERT INTO drift_context(event_id, process_name, site_host) VALUES (1, 'code.exe', '')");
        }
    }

    private Connection connect() throws Exception {
        return DriverManager.getConnection("jdbc:sqlite:" + database.toAbsolutePath());
    }

    /**
     * Canonical text of every column in the three activity tables, in a stable order.
     * The same text before and after a write means the activity rows were not touched.
     */
    private String activityFingerprint() throws Exception {
        StringBuilder payload = new StringBuilder();
        try (Connection connection = connect()) {
            payload.append(table(connection, "SELECT * FROM segment ORDER BY id"));
            payload.append(table(connection, "SELECT * FROM drift_event ORDER BY id"));
            payload.append(table(connection,
                    "SELECT * FROM drift_context ORDER BY event_id, process_name, site_host"));
        }
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(payload.toString().getBytes(StandardCharsets.UTF_8)));
    }

    private static String table(Connection connection, String sql) throws Exception {
        try (Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery(sql)) {
            int columns = result.getMetaData().getColumnCount();
            StringBuilder text = new StringBuilder();
            while (result.next()) {
                for (int column = 1; column <= columns; column++) {
                    Object value = result.getObject(column);
                    if (value == null) {
                        text.append("<null>");
                    } else if (value instanceof byte[] bytes) {
                        text.append(HexFormat.of().formatHex(bytes));
                    } else if (value instanceof String string) {
                        text.append("T").append(HexFormat.of().formatHex(
                                string.getBytes(StandardCharsets.UTF_8)));
                    } else {
                        text.append(value);
                    }
                    text.append('\u001F');
                }
                text.append('\u001E');
            }
            return text.toString();
        }
    }

    private static final class MutableClock extends Clock {
        private Instant instant;
        private final ZoneId zone;

        private MutableClock(Instant instant, ZoneId zone) {
            this.instant = instant;
            this.zone = zone;
        }

        void set(Instant instant) {
            this.instant = instant;
        }

        @Override
        public ZoneId getZone() {
            return zone;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return new MutableClock(instant, zone);
        }

        @Override
        public Instant instant() {
            return instant;
        }
    }
}
