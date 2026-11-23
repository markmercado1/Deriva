package com.deriva.storage.sqlite;

import com.deriva.coach.model.DriftEpisode;
import com.deriva.coach.model.DriftStatus;
import com.deriva.coach.model.DriftUpdate;
import com.deriva.privacy.ObservationKind;
import com.deriva.tracking.model.ContextKey;
import com.deriva.tracking.model.Segment;
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

class SQLiteDataMaintenanceTest {
    @TempDir Path tempDirectory;

    @Test
    void deletesActivityButKeepsConfigurationTokenAndSchema() throws Exception {
        Path database = tempDirectory.resolve("delete.db");
        Instant start = Instant.parse("2026-11-22T10:00:00Z");
        ContextKey context = new ContextKey("code.exe", null);
        new SQLiteSegmentRepository(database).saveAll(List.of(
                new Segment(0, ObservationKind.OBSERVED, context, null, null,
                        start, start.plusSeconds(30))));
        new SQLiteDriftRepository(database).upsertDetection(new DriftUpdate(
                DriftUpdate.Type.OPENED, new DriftEpisode(1, start, start, start.plusSeconds(10),
                null, null, DriftStatus.OPEN, null, 10, context, null, Set.of(context))));

        try (Connection connection = SQLiteSchema.open(database);
             Statement statement = connection.createStatement()) {
            statement.executeUpdate("INSERT INTO setting(key, value) VALUES ('bridge_token', 'keep-me')");
            statement.executeUpdate("INSERT INTO setting(key, value) VALUES ('bridge_port', '47821')");
            statement.executeUpdate("INSERT INTO excluded_app(process_name) VALUES ('secret.exe')");
            statement.executeUpdate("INSERT INTO excluded_site(host) VALUES ('example.com')");
        }

        SQLiteDataMaintenance.deleteActivity(database);

        try (Connection connection = SQLiteSchema.open(database);
             Statement statement = connection.createStatement()) {
            assertEquals(0, count(statement, "segment"));
            assertEquals(0, count(statement, "drift_event"));
            assertEquals(0, count(statement, "drift_context"));
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
