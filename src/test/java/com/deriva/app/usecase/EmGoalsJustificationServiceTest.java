package com.deriva.app.usecase;

import com.deriva.goals.model.EmGoal;
import com.deriva.goals.model.EmGoalAlert;
import com.deriva.goals.model.EmGoalMetric;
import com.deriva.goals.model.EmGoalProgress;
import com.deriva.goals.model.EmGoalState;
import com.deriva.privacy.EmObservationKind;
import com.deriva.storage.sqlite.EmSQLiteGoalAlertRepository;
import com.deriva.storage.sqlite.EmSQLiteGoalRepository;
import com.deriva.storage.sqlite.EmSQLiteSegmentRepository;
import com.deriva.tracking.model.EmContextKey;
import com.deriva.tracking.model.EmSegment;
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

class EmGoalsJustificationServiceTest {
    private static final ZoneOffset ZONE = ZoneOffset.UTC;
    private static final Instant NOW = Instant.parse("2026-11-24T12:00:00Z");
    private static final LocalDate DAY = LocalDate.of(2026, 11, 24);
    private static final EmContextKey CODE = new EmContextKey("code.exe", null);

    @TempDir Path tempDirectory;

    private Path database;
    private MutableClock clock;
    private EmSQLiteGoalRepository goals;
    private EmSQLiteSegmentRepository segments;
    private EmSQLiteGoalAlertRepository alerts;
    private EmGoalsService service;

    @BeforeEach
    void setUp() {
        database = tempDirectory.resolve("goals.db");
        clock = new MutableClock(NOW, ZONE);
        goals = new EmSQLiteGoalRepository(database, clock);
        segments = new EmSQLiteSegmentRepository(database);
        alerts = new EmSQLiteGoalAlertRepository(database);
        service = new EmGoalsService(goals, segments, alerts, clock);
    }

    @Test
    void coversTheOpenSegmentAndClosingItDoesNotBringTheExcessBack() {
        EmGoal goal = service.create(EmGoal.draft(CODE, 10, null, false));
        save("2026-11-24T09:00:00Z", "2026-11-24T09:08:00Z");
        EmSegment open = observed("code.exe", null,
                "2026-11-24T09:08:00Z", "2026-11-24T09:12:00Z");
        EmGoalAlertService alarm = new EmGoalAlertService(goals, segments, alerts,
                notice -> { }, clock, ZONE);
        EmGoalAlert alert = alarm.evaluate(NOW, now -> Optional.of(open)).stream()
                .filter(created -> created.thresholdPercent() == 100)
                .findFirst()
                .orElseThrow();

        service.justify(goal.id(), "Incluye el tramo abierto", ZONE, now -> Optional.of(open));

        assertEquals(Duration.ofMinutes(2).toMillis(),
                alerts.find(alert.id()).orElseThrow().justifiedExcess());
        assertEquals(EmGoalState.JUSTIFIED, progressOf(goal.id(), now -> Optional.of(open)).state());

        save("2026-11-24T09:08:00Z", "2026-11-24T09:12:00Z");
        EmGoalProgress closed = progressOf(goal.id(), now -> Optional.empty());

        assertEquals(Duration.ofMinutes(12).toMillis(), closed.usedMillis());
        assertEquals(EmGoalState.JUSTIFIED, closed.state());
    }

    @Test
    void aJustificationOnlyAppliesToTheDayItWasMarked() {
        EmGoal goal = service.create(EmGoal.draft(CODE, 10, null, false));
        save("2026-11-24T09:00:00Z", "2026-11-24T09:12:00Z");
        EmGoalAlert alert = alerts.create(EmGoalAlert.draft(goal.id(), NOW, 100, EmGoalMetric.MINUTES));
        Instant yesterday = Instant.parse("2026-11-23T12:00:00Z");
        alerts.update(alert.justified(Duration.ofMinutes(2).toMillis(), yesterday, "Ayer"));

        EmGoalProgress ignored = progressOf(goal.id(), now -> Optional.empty());

        assertEquals(EmGoalState.EXCEEDED, ignored.state());
        assertTrue(ignored.justifications().isEmpty());

        service.justify(goal.id(), "Hoy", ZONE, now -> Optional.empty());
        EmGoalProgress applied = progressOf(goal.id(), now -> Optional.empty());
        assertEquals(EmGoalState.JUSTIFIED, applied.state());
        assertEquals(NOW, alerts.find(alert.id()).orElseThrow().justifiedAt());
    }

    @Test
    void rejustifyReplacesCoverageAndSurvivesReopen() {
        EmGoal goal = service.create(EmGoal.draft(CODE, 10, null, false));
        save("2026-11-24T09:00:00Z", "2026-11-24T09:12:00Z");
        EmGoalAlert alert = alerts.create(EmGoalAlert.draft(goal.id(), NOW, 100, EmGoalMetric.MINUTES));
        service.justify(goal.id(), "Primera", ZONE, now -> Optional.empty());
        assertEquals(Duration.ofMinutes(2).toMillis(),
                alerts.find(alert.id()).orElseThrow().justifiedExcess());

        clock.set(Instant.parse("2026-11-24T12:05:00Z"));
        save("2026-11-24T12:00:00Z", "2026-11-24T12:03:00Z");
        service.justify(goal.id(), "Segunda", ZONE, now -> Optional.empty());

        EmGoalAlert stored = alerts.find(alert.id()).orElseThrow();
        assertEquals(Duration.ofMinutes(5).toMillis(), stored.justifiedExcess());
        assertEquals("Segunda", stored.justification());
        assertEquals(Instant.parse("2026-11-24T12:05:00Z"), stored.justifiedAt());

        EmSQLiteGoalAlertRepository reopenedAlerts = new EmSQLiteGoalAlertRepository(database);
        EmGoalsService reopened = new EmGoalsService(
                new EmSQLiteGoalRepository(database, clock),
                new EmSQLiteSegmentRepository(database), reopenedAlerts, clock);
        EmGoalProgress afterReopen = reopened.progressForDay(DAY, ZONE, now -> Optional.empty())
                .stream().filter(item -> item.goal().id() == goal.id()).findFirst().orElseThrow();
        assertEquals(EmGoalState.JUSTIFIED, afterReopen.state());
        assertEquals(Duration.ofMinutes(5).toMillis(),
                reopenedAlerts.find(alert.id()).orElseThrow().justifiedExcess());

        reopened.removeJustification(goal.id(), ZONE);
        EmGoalAlert cleared = reopenedAlerts.find(alert.id()).orElseThrow();
        assertFalse(cleared.isJustified());
        assertNull(cleared.justification());
        assertEquals(EmGoalState.EXCEEDED, reopened.progressForDay(DAY, ZONE, now -> Optional.empty())
                .stream().filter(item -> item.goal().id() == goal.id()).findFirst().orElseThrow().state());
    }

    @Test
    void justifyingAndRemovingLeaveActivityTablesByteIdentical() throws Exception {
        EmGoal goal = service.create(EmGoal.draft(CODE, 10, null, false));
        save("2026-11-24T09:00:00Z", "2026-11-24T09:12:00Z");
        alerts.create(EmGoalAlert.draft(goal.id(), NOW, 100, EmGoalMetric.MINUTES));
        insertDriftRows();

        String before = activityFingerprint();
        service.justify(goal.id(), "Solo una anotaciÃ³n", ZONE, now -> Optional.empty());
        assertEquals(before, activityFingerprint());
        service.removeJustification(goal.id(), ZONE);
        assertEquals(before, activityFingerprint());
    }

    /**
     * Gate F6B-7: in a controlled step, without capture, the activity tables keep the
     * same fingerprint through goal creation, an alarm evaluation, justify and remove.
     */
    @Test
    void goalCreationAlarmAndJustificationKeepActivityTablesByteIdentical() throws Exception {
        save("2026-11-24T09:00:00Z", "2026-11-24T09:12:00Z");
        insertDriftRows();
        String before = activityFingerprint();
        System.out.println("F6B-7 activity fingerprint: " + before);

        EmGoal goal = service.create(EmGoal.draft(CODE, 10, null, false));
        assertEquals(before, activityFingerprint(), "creating a goal wrote activity rows");

        EmGoalAlertService alarm = new EmGoalAlertService(goals, segments, alerts,
                notice -> { }, clock, ZONE);
        assertFalse(alarm.evaluate(NOW, now -> Optional.empty()).isEmpty());
        assertEquals(before, activityFingerprint(), "evaluating the alarm wrote activity rows");

        service.justify(goal.id(), "Torneo", ZONE, now -> Optional.empty());
        assertEquals(before, activityFingerprint(), "justifying wrote activity rows");

        service.removeJustification(goal.id(), ZONE);
        assertEquals(before, activityFingerprint(), "removing a justification wrote activity rows");
    }

    private EmGoalProgress progressOf(long goalId, com.deriva.goals.EmOpenSegmentProvider openSegments) {
        return service.progressForDay(DAY, ZONE, openSegments).stream()
                .filter(item -> item.goal().id() == goalId)
                .findFirst()
                .orElseThrow();
    }

    private void save(String start, String end) {
        segments.saveAll(List.of(observed("code.exe", null, start, end)));
    }

    private static EmSegment observed(String process, String host, String start, String end) {
        return new EmSegment(0, EmObservationKind.OBSERVED, new EmContextKey(process, host),
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
