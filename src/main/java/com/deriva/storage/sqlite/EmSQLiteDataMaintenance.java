package com.deriva.storage.sqlite;

import com.deriva.storage.EmStorageException;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Objects;

/**
 * Explicit deletion of activity records; configuration, goals and schema are preserved.
 *
 * <p>EmGoal alerts are usage history, so they are deleted with the activity; the goals
 * themselves are configuration and stay. Opening migrates the schema first, so a V1
 * database also has the alert table when this runs.</p>
 */
public final class EmSQLiteDataMaintenance {
    private EmSQLiteDataMaintenance() {}

    public static void deleteActivity(Path databasePath) {
        Objects.requireNonNull(databasePath, "databasePath");
        EmSQLiteSchema.initialize(databasePath);
        try (Connection connection = EmSQLiteSchema.open(databasePath)) {
            connection.setAutoCommit(false);
            try (Statement statement = connection.createStatement()) {
                statement.executeUpdate("DELETE FROM goal_alerts");
                statement.executeUpdate("DELETE FROM drift_context");
                statement.executeUpdate("DELETE FROM drift_event");
                statement.executeUpdate("DELETE FROM segment");
                connection.commit();
            } catch (SQLException e) {
                connection.rollback();
                throw new EmStorageException("Could not delete activity records", e);
            }
        } catch (SQLException e) {
            throw new EmStorageException("Could not open SQLite database", e);
        }
    }
}
