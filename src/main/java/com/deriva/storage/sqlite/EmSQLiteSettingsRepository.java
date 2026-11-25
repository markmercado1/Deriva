package com.deriva.storage.sqlite;

import com.deriva.storage.EmStorageException;
import com.deriva.storage.port.EmSettingsRepository;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/** JDBC adapter for the existing setting(key, value) table. Saves are one transaction. */
public final class EmSQLiteSettingsRepository implements EmSettingsRepository {
    private static final String SELECT = "SELECT key, value FROM setting ORDER BY key";
    private static final String UPSERT = """
            INSERT INTO setting(key, value) VALUES (?, ?)
            ON CONFLICT(key) DO UPDATE SET value = excluded.value
            """;

    private final Path databasePath;

    public EmSQLiteSettingsRepository(Path databasePath) {
        this.databasePath = Objects.requireNonNull(databasePath, "databasePath").toAbsolutePath();
        EmSQLiteSchema.initialize(this.databasePath);
    }

    @Override
    public Map<String, String> loadAll() {
        try (Connection connection = EmSQLiteSchema.open(databasePath);
             PreparedStatement statement = connection.prepareStatement(SELECT);
             ResultSet result = statement.executeQuery()) {
            Map<String, String> values = new LinkedHashMap<>();
            while (result.next()) values.put(result.getString(1), result.getString(2));
            return Map.copyOf(values);
        } catch (SQLException e) {
            throw new EmStorageException("Could not read settings", e);
        }
    }

    @Override
    public void saveAll(Map<String, String> values) {
        Objects.requireNonNull(values, "values");
        if (values.isEmpty()) return;

        try (Connection connection = EmSQLiteSchema.open(databasePath)) {
            connection.setAutoCommit(false);
            try (PreparedStatement statement = connection.prepareStatement(UPSERT)) {
                for (Map.Entry<String, String> entry : values.entrySet()) {
                    if (entry.getKey() == null || entry.getKey().isBlank() || entry.getValue() == null)
                        throw new IllegalArgumentException("setting");
                    statement.setString(1, entry.getKey());
                    statement.setString(2, entry.getValue());
                    statement.addBatch();
                }
                statement.executeBatch();
                connection.commit();
            } catch (SQLException | RuntimeException e) {
                connection.rollback();
                if (e instanceof RuntimeException runtime) throw runtime;
                throw new EmStorageException("Could not save settings", e);
            }
        } catch (SQLException e) {
            throw new EmStorageException("Could not open SQLite database", e);
        }
    }
}
