package com.deriva.storage.sqlite;

import com.deriva.goals.model.GoalAlert;
import com.deriva.goals.model.GoalMetric;
import com.deriva.storage.StorageException;
import com.deriva.storage.port.GoalAlertRepository;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** JDBC adapter for goal alerts. Alerts disappear with their goal (ON DELETE CASCADE). */
public final class SQLiteGoalAlertRepository implements GoalAlertRepository {
    private static final String INSERT = """
            INSERT INTO goal_alerts(goal_id, triggered_at, threshold_percent, metric, outcome, justification)
            VALUES (?, ?, ?, ?, ?, ?)
            """;
    private static final String UPDATE = """
            UPDATE goal_alerts
            SET triggered_at = ?, threshold_percent = ?, metric = ?, outcome = ?, justification = ?
            WHERE id = ?
            """;
    private static final String SELECT = """
            SELECT id, goal_id, triggered_at, threshold_percent, metric, outcome, justification
            FROM goal_alerts
            """;

    private final Path databasePath;

    public SQLiteGoalAlertRepository(Path databasePath) {
        this.databasePath = Objects.requireNonNull(databasePath, "databasePath").toAbsolutePath();
        SQLiteSchema.initialize(this.databasePath);
    }

    @Override
    public GoalAlert create(GoalAlert alert) {
        Objects.requireNonNull(alert, "alert");
        if (alert.id() != 0) throw new IllegalArgumentException("new alert must not have an id");

        try (Connection connection = SQLiteSchema.open(databasePath)) {
            connection.setAutoCommit(false);
            try {
                try (PreparedStatement statement = connection.prepareStatement(INSERT)) {
                    statement.setLong(1, alert.goalId());
                    statement.setLong(2, alert.triggeredAt().toEpochMilli());
                    statement.setInt(3, alert.thresholdPercent());
                    statement.setString(4, alert.metric().name());
                    statement.setString(5, alert.outcome());
                    statement.setString(6, alert.justification());
                    statement.executeUpdate();
                }
                long id = lastInsertId(connection);
                connection.commit();
                return new GoalAlert(id, alert.goalId(), alert.triggeredAt(), alert.thresholdPercent(),
                        alert.metric(), alert.outcome(), alert.justification());
            } catch (SQLException | RuntimeException e) {
                connection.rollback();
                if (e instanceof RuntimeException runtime) throw runtime;
                throw new StorageException("Could not create goal alert", e);
            }
        } catch (SQLException e) {
            throw new StorageException("Could not open SQLite database", e);
        }
    }

    @Override
    public Optional<GoalAlert> find(long id) {
        if (id <= 0) throw new IllegalArgumentException("id");
        try (Connection connection = SQLiteSchema.open(databasePath);
             PreparedStatement statement = connection.prepareStatement(SELECT + " WHERE id = ?")) {
            statement.setLong(1, id);
            try (ResultSet result = statement.executeQuery()) {
                return result.next() ? Optional.of(read(result)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new StorageException("Could not read goal alert", e);
        }
    }

    @Override
    public void update(GoalAlert alert) {
        Objects.requireNonNull(alert, "alert");
        if (alert.id() <= 0) throw new IllegalArgumentException("id");

        try (Connection connection = SQLiteSchema.open(databasePath);
             PreparedStatement statement = connection.prepareStatement(UPDATE)) {
            statement.setLong(1, alert.triggeredAt().toEpochMilli());
            statement.setInt(2, alert.thresholdPercent());
            statement.setString(3, alert.metric().name());
            statement.setString(4, alert.outcome());
            statement.setString(5, alert.justification());
            statement.setLong(6, alert.id());
            if (statement.executeUpdate() != 1)
                throw new StorageException("Unknown goal alert: " + alert.id());
        } catch (SQLException e) {
            throw new StorageException("Could not update goal alert", e);
        }
    }

    @Override
    public boolean delete(long id) {
        if (id <= 0) throw new IllegalArgumentException("id");
        try (Connection connection = SQLiteSchema.open(databasePath);
             PreparedStatement statement = connection.prepareStatement("DELETE FROM goal_alerts WHERE id = ?")) {
            statement.setLong(1, id);
            return statement.executeUpdate() == 1;
        } catch (SQLException e) {
            throw new StorageException("Could not delete goal alert", e);
        }
    }

    @Override
    public List<GoalAlert> listByGoal(long goalId) {
        if (goalId <= 0) throw new IllegalArgumentException("goalId");
        return query(SELECT + " WHERE goal_id = ? ORDER BY triggered_at, id", goalId);
    }

    @Override
    public List<GoalAlert> listPending() {
        return query(SELECT + " WHERE outcome IS NULL ORDER BY triggered_at, id", 0);
    }

    private List<GoalAlert> query(String sql, long goalId) {
        try (Connection connection = SQLiteSchema.open(databasePath);
             PreparedStatement statement = connection.prepareStatement(sql)) {
            if (goalId > 0) statement.setLong(1, goalId);
            try (ResultSet result = statement.executeQuery()) {
                List<GoalAlert> alerts = new ArrayList<>();
                while (result.next()) alerts.add(read(result));
                return List.copyOf(alerts);
            }
        } catch (SQLException e) {
            throw new StorageException("Could not read goal alerts", e);
        }
    }

    private static long lastInsertId(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery("SELECT last_insert_rowid()")) {
            if (!result.next()) throw new StorageException("Could not obtain goal alert id");
            return result.getLong(1);
        }
    }

    private static GoalAlert read(ResultSet result) throws SQLException {
        return new GoalAlert(result.getLong("id"), result.getLong("goal_id"),
                Instant.ofEpochMilli(result.getLong("triggered_at")),
                result.getInt("threshold_percent"),
                GoalMetric.valueOf(result.getString("metric")),
                result.getString("outcome"), result.getString("justification"));
    }
}
