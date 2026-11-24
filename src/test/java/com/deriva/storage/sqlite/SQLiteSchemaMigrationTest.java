package com.deriva.storage.sqlite;

import com.deriva.storage.StorageException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class SQLiteSchemaMigrationTest {
    @TempDir Path tempDirectory;

    private Path database(String name) {
        return tempDirectory.resolve(name);
    }

    @Test
    void migratesV1DataWithoutLossAndIsIdempotent() throws Exception {
        Path database = database("v1.db");
        createV1WithData(database);

        SQLiteSchema.initialize(database);
        assertVersion(database, 2);
        assertGoalTables(database);
        assertV1DataIntact(database);

        SQLiteSchema.initialize(database);
        assertVersion(database, 2);
        assertEquals(1, count(database, "schema_version"));
        assertV1DataIntact(database);
    }

    @Test
    void newDatabaseStartsAtVersionTwo() throws Exception {
        Path database = database("new.db");
        SQLiteSchema.initialize(database);

        assertVersion(database, 2);
        assertGoalTables(database);
        assertEquals(0, count(database, "goals"));
        assertEquals(0, count(database, "goal_alerts"));
    }

    @Test
    void rejectsDatabaseFromANewerVersion() throws Exception {
        Path database = database("future.db");
        SQLiteSchema.initialize(database);
        try (Connection connection = SQLiteSchema.open(database);
             Statement statement = connection.createStatement()) {
            statement.executeUpdate("UPDATE schema_version SET version = 3");
        }
        assertThrows(StorageException.class, () -> SQLiteSchema.initialize(database));
    }

    private static void createV1WithData(Path database) throws Exception {
        try (Connection connection = SQLiteSchema.open(database);
             Statement statement = connection.createStatement()) {
            statement.execute("CREATE TABLE schema_version (version INTEGER NOT NULL)");
            statement.execute("INSERT INTO schema_version(version) VALUES (1)");
            for (String sql : SQLiteSchema.V1) statement.execute(sql);

            statement.executeUpdate("""
                    INSERT INTO segment(kind, process_name, site_host, site_label, site_source, start_ts, end_ts)
                    VALUES ('OBSERVED', 'code.exe', NULL, NULL, NULL, 1000, 2000)
                    """);
            statement.executeUpdate("""
                    INSERT INTO drift_event(start_ts, detected_at, last_observed_ts, status, switch_count,
                                            first_change_process, reason, justified, answered_at)
                    VALUES (1000, 1200, 5000, 'OPEN', 10, 'code.exe', 'BREAK', 1, 4000)
                    """);
            statement.executeUpdate(
                    "INSERT INTO drift_context(event_id, process_name, site_host) VALUES (1, 'code.exe', '')");
            statement.executeUpdate("INSERT INTO setting(key, value) VALUES ('drift_switches', '7')");
            statement.executeUpdate("INSERT INTO excluded_app(process_name) VALUES ('secret.exe')");
            statement.executeUpdate("INSERT INTO excluded_site(host) VALUES ('example.com')");
        }
    }

    private static void assertV1DataIntact(Path database) throws Exception {
        try (Connection connection = SQLiteSchema.open(database);
             Statement statement = connection.createStatement()) {
            try (ResultSet result = statement.executeQuery("""
                    SELECT kind, process_name, site_host, site_label, site_source, start_ts, end_ts
                    FROM segment
                    """)) {
                assertTrue(result.next());
                assertEquals("OBSERVED", result.getString(1));
                assertEquals("code.exe", result.getString(2));
                assertNull(result.getString(3));
                assertNull(result.getString(4));
                assertNull(result.getString(5));
                assertEquals(1000, result.getLong(6));
                assertEquals(2000, result.getLong(7));
                assertFalse(result.next());
            }
            try (ResultSet result = statement.executeQuery("""
                    SELECT status, switch_count, reason, justified, answered_at FROM drift_event
                    """)) {
                assertTrue(result.next());
                assertEquals("OPEN", result.getString(1));
                assertEquals(10, result.getInt(2));
                assertEquals("BREAK", result.getString(3));
                assertEquals(1, result.getInt(4));
                assertEquals(4000, result.getLong(5));
                assertFalse(result.next());
            }
            try (ResultSet result = statement.executeQuery(
                    "SELECT process_name, site_host FROM drift_context")) {
                assertTrue(result.next());
                assertEquals("code.exe", result.getString(1));
                assertEquals("", result.getString(2));
                assertFalse(result.next());
            }
            try (ResultSet result = statement.executeQuery(
                    "SELECT value FROM setting WHERE key = 'drift_switches'")) {
                assertTrue(result.next());
                assertEquals("7", result.getString(1));
            }
            assertEquals(1, count(connection, "excluded_app"));
            assertEquals(1, count(connection, "excluded_site"));
        }
    }

    private static void assertVersion(Path database, int expected) throws Exception {
        try (Connection connection = SQLiteSchema.open(database);
             Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery("SELECT version FROM schema_version")) {
            assertTrue(result.next());
            assertEquals(expected, result.getInt(1));
        }
    }

    private static void assertGoalTables(Path database) throws Exception {
        try (Connection connection = SQLiteSchema.open(database);
             Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery("""
                     SELECT name FROM sqlite_master
                     WHERE type = 'table' AND name IN ('goals', 'goal_alerts')
                     """)) {
            Set<String> names = new HashSet<>();
            while (result.next()) names.add(result.getString(1));
            assertEquals(Set.of("goals", "goal_alerts"), names);
        }
    }

    private static int count(Path database, String table) throws Exception {
        try (Connection connection = SQLiteSchema.open(database)) {
            return count(connection, table);
        }
    }

    private static int count(Connection connection, String table) throws Exception {
        try (Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery("SELECT COUNT(*) FROM " + table)) {
            result.next();
            return result.getInt(1);
        }
    }
}
