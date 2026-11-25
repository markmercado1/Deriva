package com.deriva.storage.sqlite;

import com.deriva.storage.EmStorageException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Objects;

/** SQLite connection settings and schema migrations (V1 to V4). */
final class EmSQLiteSchema {
    static final int VERSION = 5;

    private static final String HISTORY_COLUMNS = "goal_id, process_name, site_host, daily_minutes, "
            + "daily_entries, passive, active, created_at, updated_at, recorded_at, archived, baseline";
    private static final String HISTORY_NEW = "NEW.id, NEW.process_name, NEW.site_host, NEW.daily_minutes, "
            + "NEW.daily_entries, NEW.passive, NEW.active, NEW.created_at, NEW.updated_at, "
            + "NEW.updated_at, CASE WHEN NEW.archived_at IS NULL THEN 0 ELSE 1 END, 0";
    private static final String[] V5 = {
            "ALTER TABLE goals ADD COLUMN archived_at INTEGER",
            "DROP INDEX ux_goals_app",
            "DROP INDEX ux_goals_site",
            "CREATE UNIQUE INDEX ux_goals_app ON goals(process_name) WHERE site_host IS NULL AND archived_at IS NULL",
            "CREATE UNIQUE INDEX ux_goals_site ON goals(process_name,site_host) WHERE site_host IS NOT NULL AND archived_at IS NULL",
            """
            CREATE TABLE goal_history (
              revision INTEGER PRIMARY KEY AUTOINCREMENT,
              goal_id INTEGER NOT NULL REFERENCES goals(id) ON DELETE CASCADE,
              process_name TEXT NOT NULL, site_host TEXT,
              daily_minutes INTEGER, daily_entries INTEGER,
              passive INTEGER NOT NULL, active INTEGER NOT NULL,
              created_at INTEGER NOT NULL, updated_at INTEGER NOT NULL,
              recorded_at INTEGER NOT NULL, archived INTEGER NOT NULL,
              baseline INTEGER NOT NULL
            )
            """,
            "CREATE INDEX idx_goal_history_time ON goal_history(recorded_at,goal_id)",
            "INSERT INTO goal_history(" + HISTORY_COLUMNS + ") SELECT id,process_name,site_host,daily_minutes,"
                    + "daily_entries,passive,active,created_at,updated_at,CAST(unixepoch('subsec')*1000 AS INTEGER),0,1 FROM goals",
            "CREATE TRIGGER goal_history_insert AFTER INSERT ON goals BEGIN INSERT INTO goal_history("
                    + HISTORY_COLUMNS + ") VALUES(" + HISTORY_NEW + "); END",
            "CREATE TRIGGER goal_history_update AFTER UPDATE ON goals BEGIN INSERT INTO goal_history("
                    + HISTORY_COLUMNS + ") VALUES(" + HISTORY_NEW + "); END"
    };

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

    static final String[] V2 = {
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

    static final String[] V3 = {
            "ALTER TABLE goal_alerts ADD COLUMN justified_at INTEGER",
            """
            ALTER TABLE goal_alerts ADD COLUMN justified_excess INTEGER
              CHECK (justified_excess IS NULL OR justified_excess >= 0)
            """
    };

    private static final String[] V4 = {
            "ALTER TABLE goal_alerts ADD COLUMN notified_at INTEGER",
            "ALTER TABLE goal_alerts ADD COLUMN observed_until INTEGER",
            "ALTER TABLE goal_alerts ADD COLUMN outcome_at INTEGER",
            """
            ALTER TABLE goal_alerts ADD COLUMN outcome_after_ms INTEGER
              CHECK (outcome_after_ms IS NULL OR outcome_after_ms >= 0)
            """,
            // Existing alerts were already delivered (or recorded silently): never
            // treat them as pending notices or as observations to resume.
            "UPDATE goal_alerts SET notified_at = triggered_at WHERE notified_at IS NULL"
    };

    private EmSQLiteSchema() {}

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
            throw new EmStorageException("Could not open SQLite database", e);
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
                if (version > VERSION) throw new EmStorageException("Unsupported schema version: " + version);

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
                if (version < 3) {
                    run(connection, V3);
                    version = 3;
                    migrated = true;
                }
                if (version < 4) {
                    run(connection, V4);
                    version = 4;
                    migrated = true;
                }
                if (version < 5) {
                    run(connection, V5);
                    version = 5;
                    migrated = true;
                }
                if (migrated) writeVersion(connection, version);
                connection.commit();
            } catch (SQLException e) {
                connection.rollback();
                throw new EmStorageException("Could not migrate SQLite schema", e);
            } catch (RuntimeException e) {
                connection.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new EmStorageException("Could not initialize SQLite schema", e);
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
