package com.deriva.storage.sqlite;

import com.deriva.storage.StorageException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Objects;

/** SQLite connection settings and schema migrations (V1 to V2). */
final class SQLiteSchema {
    static final int VERSION = 2;

    static final String[] V1 = {
            """
            CREATE TABLE segment (
              id INTEGER PRIMARY KEY AUTOINCREMENT,
              kind TEXT NOT NULL CHECK (kind IN ('OBSERVED','IDLE','EXCLUDED','UNAVAILABLE')),
              process_name TEXT NOT NULL,
              site_host TEXT,
              site_label TEXT,
              site_source TEXT CHECK (site_source = 'EXTENSION'),
              start_ts INTEGER NOT NULL,
              end_ts INTEGER NOT NULL CHECK (end_ts >= start_ts)
            )
            """,
            "CREATE INDEX idx_segment_start ON segment(start_ts)",
            """
            CREATE TABLE drift_event (
              id INTEGER PRIMARY KEY,
              start_ts INTEGER NOT NULL,
              detected_at INTEGER NOT NULL,
              last_observed_ts INTEGER NOT NULL,
              stability_start_ts INTEGER,
              closed_at INTEGER,
              status TEXT NOT NULL CHECK (status IN ('OPEN','STABLE','UNCONFIRMED')),
              close_reason TEXT,
              switch_count INTEGER NOT NULL,
              first_change_process TEXT NOT NULL,
              first_change_host TEXT,
              time_to_stability_ms INTEGER,
              reason TEXT CHECK (reason IN ('SEARCHING_INFO','MESSAGES','BREAK','BOREDOM','OTHER')),
              justified INTEGER NOT NULL DEFAULT 0 CHECK (justified IN (0,1)),
              answered_at INTEGER,
              CHECK (time_to_stability_ms IS NULL OR time_to_stability_ms >= 0),
              CHECK (
                (status = 'STABLE' AND stability_start_ts IS NOT NULL
                  AND time_to_stability_ms IS NOT NULL AND closed_at IS NOT NULL)
                OR (status = 'OPEN' AND stability_start_ts IS NULL
                  AND time_to_stability_ms IS NULL AND closed_at IS NULL)
                OR (status = 'UNCONFIRMED' AND stability_start_ts IS NULL
                  AND time_to_stability_ms IS NULL AND closed_at IS NOT NULL)
              )
            )
            """,
            "CREATE INDEX idx_drift_start ON drift_event(start_ts)",
            """
            CREATE TABLE drift_context (
              event_id INTEGER NOT NULL REFERENCES drift_event(id) ON DELETE CASCADE,
              process_name TEXT NOT NULL,
              site_host TEXT NOT NULL DEFAULT '',
              PRIMARY KEY (event_id, process_name, site_host)
            )
            """,
            "CREATE TABLE setting (key TEXT PRIMARY KEY, value TEXT NOT NULL)",
            "CREATE TABLE excluded_app (process_name TEXT PRIMARY KEY)",
            "CREATE TABLE excluded_site (host TEXT PRIMARY KEY)"
    };

    private static final String[] V2 = {
            """
            CREATE TABLE IF NOT EXISTS goals (
              id INTEGER PRIMARY KEY AUTOINCREMENT,
              process_name TEXT NOT NULL COLLATE NOCASE,
              site_host TEXT,
              daily_minutes INTEGER,
              daily_entries INTEGER,
              passive INTEGER NOT NULL DEFAULT 0 CHECK (passive IN (0, 1)),
              active INTEGER NOT NULL DEFAULT 1 CHECK (active IN (0, 1)),
              created_at INTEGER NOT NULL,
              updated_at INTEGER NOT NULL,
              CHECK (daily_minutes IS NOT NULL OR daily_entries IS NOT NULL),
              CHECK (daily_minutes IS NULL OR daily_minutes > 0),
              CHECK (daily_entries IS NULL OR daily_entries > 0),
              CHECK (length(trim(process_name)) > 0),
              CHECK (instr(process_name, '/') = 0 AND instr(process_name, '\\') = 0),
              CHECK (site_host IS NULL OR (
                  length(trim(site_host)) > 0
                  AND instr(site_host, '/') = 0 AND instr(site_host, '?') = 0
                  AND instr(site_host, '#') = 0 AND instr(site_host, ':') = 0
                  AND instr(site_host, ' ') = 0))
            )
            """,
            """
            CREATE UNIQUE INDEX IF NOT EXISTS ux_goals_app
              ON goals(process_name) WHERE site_host IS NULL
            """,
            """
            CREATE UNIQUE INDEX IF NOT EXISTS ux_goals_site
              ON goals(process_name, site_host) WHERE site_host IS NOT NULL
            """,
            "CREATE INDEX IF NOT EXISTS idx_goals_active ON goals(active)",
            """
            CREATE TABLE IF NOT EXISTS goal_alerts (
              id INTEGER PRIMARY KEY AUTOINCREMENT,
              goal_id INTEGER NOT NULL REFERENCES goals(id) ON DELETE CASCADE,
              triggered_at INTEGER NOT NULL,
              threshold_percent INTEGER NOT NULL CHECK (threshold_percent BETWEEN 1 AND 100),
              metric TEXT NOT NULL CHECK (metric IN ('MINUTES', 'ENTRIES')),
              outcome TEXT,
              justification TEXT CHECK (justification IS NULL OR length(justification) <= 500)
            )
            """,
            """
            CREATE INDEX IF NOT EXISTS idx_goal_alerts_goal
              ON goal_alerts(goal_id, triggered_at)
            """
    };

    private SQLiteSchema() {}

    static Connection open(Path databasePath) {
        Objects.requireNonNull(databasePath, "databasePath");
        Path absolute = databasePath.toAbsolutePath();
        try {
            Path parent = absolute.getParent();
            if (parent != null) Files.createDirectories(parent);
            Connection connection = DriverManager.getConnection("jdbc:sqlite:" + absolute);
            try (Statement statement = connection.createStatement()) {
                statement.execute("PRAGMA journal_mode=WAL");
                statement.execute("PRAGMA foreign_keys=ON");
                statement.execute("PRAGMA busy_timeout=5000");
            }
            return connection;
        } catch (IOException | SQLException e) {
            throw new StorageException("Could not open SQLite database", e);
        }
    }

    static void initialize(Path databasePath) {
        try (Connection connection = open(databasePath)) {
            connection.setAutoCommit(false);
            try {
                try (Statement statement = connection.createStatement()) {
                    statement.execute("CREATE TABLE IF NOT EXISTS schema_version (version INTEGER NOT NULL)");
                }
                int version = readVersion(connection);
                if (version > VERSION) throw new StorageException("Unsupported schema version: " + version);

                boolean migrated = false;
                if (version < 1) {
                    run(connection, V1);
                    version = 1;
                    migrated = true;
                }
                if (version < 2) {
                    run(connection, V2);
                    version = 2;
                    migrated = true;
                }
                if (migrated) writeVersion(connection, version);
                connection.commit();
            } catch (SQLException e) {
                connection.rollback();
                throw new StorageException("Could not migrate SQLite schema", e);
            } catch (RuntimeException e) {
                connection.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new StorageException("Could not initialize SQLite schema", e);
        }
    }

    private static void run(Connection connection, String[] statements) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            for (String sql : statements) statement.execute(sql);
        }
    }

    private static void writeVersion(Connection connection, int version) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate("DELETE FROM schema_version");
            statement.executeUpdate("INSERT INTO schema_version(version) VALUES (" + version + ")");
        }
    }

    private static int readVersion(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery("SELECT version FROM schema_version LIMIT 1")) {
            return result.next() ? result.getInt(1) : 0;
        }
    }
}
