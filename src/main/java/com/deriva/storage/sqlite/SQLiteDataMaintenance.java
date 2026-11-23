package com.deriva.storage.sqlite;

import com.deriva.storage.StorageException;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Objects;

/** Explicit deletion of activity records; configuration and schema are preserved. */
public final class SQLiteDataMaintenance {
    private SQLiteDataMaintenance() {}

    public static void deleteActivity(Path databasePath) {
        Objects.requireNonNull(databasePath, "databasePath");
        try (Connection connection = SQLiteSchema.open(databasePath)) {
            connection.setAutoCommit(false);
            try (Statement statement = connection.createStatement()) {
                statement.executeUpdate("DELETE FROM drift_context");
                statement.executeUpdate("DELETE FROM drift_event");
                statement.executeUpdate("DELETE FROM segment");
                connection.commit();
            } catch (SQLException e) {
                connection.rollback();
                throw new StorageException("Could not delete activity records", e);
            }
        } catch (SQLException e) {
            throw new StorageException("Could not open SQLite database", e);
        }
    }
}
