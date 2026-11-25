package com.deriva.storage.sqlite;

import com.deriva.coach.model.EmDriftEpisode;
import com.deriva.coach.model.EmDriftStatus;
import com.deriva.coach.model.EmDriftUpdate;
import com.deriva.goals.model.EmGoal;
import com.deriva.goals.model.EmGoalAlert;
import com.deriva.goals.model.EmGoalMetric;
import com.deriva.privacy.EmObservationKind;
import com.deriva.tracking.model.EmContextKey;
import com.deriva.tracking.model.EmSegment;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.Instant;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EmSQLiteDataMaintenanceTest {
    @TempDir Path tempDirectory;

    @Test
    void deletesActivityButKeepsConfigurationTokenAndSchema() throws Exception {
        Path database = tempDirectory.resolve("delete.db");
        Instant start = Instant.parse("2026-11-22T10:00:00Z");
        EmContextKey context = new EmContextKey("code.exe", null);
        new EmSQLiteSegmentRepository(database).saveAll(List.of(
                new EmSegment(0, EmObservationKind.OBSERVED, context, null, null,
                        start, start.plusSeconds(30))));
        new EmSQLiteDriftRepository(database).upsertDetection(new EmDriftUpdate(
                EmDriftUpdate.Type.OPENED, new EmDriftEpisode(1, start, start, start.plusSeconds(10),
                null, null, EmDriftStatus.OPEN, null, 10, context, null, Set.of(context))));

        EmGoal goal = new EmSQLiteGoalRepository(database).create(EmGoal.draft(context, 60, null, false));
        new EmSQLiteGoalAlertRepository(database)
                .create(EmGoalAlert.draft(goal.id(), start, 100, EmGoalMetric.MINUTES));

        try (Connection connection = EmSQLiteSchema.open(database);
             Statement statement = connection.createStatement()) {
            statement.executeUpdate("INSERT INTO setting(key, value) VALUES ('bridge_token', 'keep-me')");
            statement.executeUpdate("INSERT INTO setting(key, value) VALUES ('bridge_port', '47821')");
            statement.executeUpdate("INSERT INTO excluded_app(process_name) VALUES ('secret.exe')");
            statement.executeUpdate("INSERT INTO excluded_site(host) VALUES ('example.com')");
        }

        EmSQLiteDataMaintenance.deleteActivity(database);

        try (Connection connection = EmSQLiteSchema.open(database);
             Statement statement = connection.createStatement()) {
            assertEquals(0, count(statement, "segment"));
            assertEquals(0, count(statement, "drift_event"));
            assertEquals(0, count(statement, "drift_context"));
            assertEquals(0, count(statement, "goal_alerts"));
            assertEquals(1, count(statement, "goals"));
            assertEquals(2, count(statement, "setting"));
            assertEquals(1, count(statement, "excluded_app"));
            assertEquals(1, count(statement, "excluded_site"));
            assertEquals(1, count(statement, "schema_version"));
            try (ResultSet result = statement.executeQuery(
                    "SELECT value FROM setting WHERE key = 'bridge_token'")) {
                result.next();
                assertEquals("keep-me", result.getString(1));
            }
        }
    }

    private static int count(Statement statement, String table) throws Exception {
        try (ResultSet result = statement.executeQuery("SELECT COUNT(*) FROM " + table)) {
            result.next();
            return result.getInt(1);
        }
    }
}
