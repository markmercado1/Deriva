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
import java.sql.Types;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** JDBC adapter for goal alerts. Alerts disappear with their goal (ON DELETE CASCADE). */
public final class SQLiteGoalAlertRepository implements GoalAlertRepository {
    private static final String INSERT = """
            INSERT INTO goal_alerts(goal_id, triggered_at, threshold_percent, metric, outcome,
                                    justification, justified_at, justified_excess)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            """;
    private static final String UPDATE = """
            UPDATE goal_alerts
            SET triggered_at = ?, threshold_percent = ?, metric = ?, outcome = ?,
                justification = ?, justified_at = ?, justified_excess = ?
            WHERE id = ?
            """;
    private static final String SELECT = """
            SELECT id, goal_id, triggered_at, threshold_percent, metric, outcome,
                   justification, justified_at, justified_excess
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
                    bindInsert(statement, alert);
                    statement.executeUpdate();
                }
                long id = lastInsertId(connection);
                connection.commit();
                return new GoalAlert(id, alert.goalId(), alert.triggeredAt(), alert.thresholdPercent(),
                        alert.metric(), alert.outcome(), alert.justification(),
                        alert.justifiedAt(), alert.justifiedExcess());
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
        return query(SELECT + " WHERE id = ?", statement -> statement.setLong(1, id))
                .stream().findFirst();
    }

    @Override
    public void update(GoalAlert alert) {
        Objects.requireNonNull(alert, "alert");
        if (alert.id() <= 0) throw new IllegalArgumentException("id");

        try (Connection connection = SQLiteSchema.open(databasePath);
             PreparedStatement statement = connection.prepareStatement(UPDATE)) {
            bindUpdate(statement, alert);
            if (statement.executeUpdate() != 1)
                throw new StorageException("Unknown goal alert: " + alert.id());
        } catch (SQLException e) {
            throw new StorageException("Could not update goal alert", e);
        }
    }

    @Override
    public void updateAll(List<GoalAlert> alerts) {
        Objects.requireNonNull(alerts, "alerts");
        if (alerts.isEmpty()) return;
        for (GoalAlert alert : alerts) {
            Objects.requireNonNull(alert, "alert");
            if (alert.id() <= 0) throw new IllegalArgumentException("id");
        }
        try (Connection connection = SQLiteSchema.open(databasePath)) {
            connection.setAutoCommit(false);
            try (PreparedStatement statement = connection.prepareStatement(UPDATE)) {
                for (GoalAlert alert : alerts) {
                    bindUpdate(statement, alert);
                    if (statement.executeUpdate() != 1)
                        throw new StorageException("Unknown goal alert: " + alert.id());
                }
                connection.commit();
            } catch (SQLException | RuntimeException e) {
                connection.rollback();
                if (e instanceof RuntimeException runtime) throw runtime;
                throw new StorageException("Could not update goal alerts", e);
            }
        } catch (SQLException e) {
            throw new StorageException("Could not open SQLite database", e);
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
        return query(SELECT + " WHERE goal_id = ? ORDER BY triggered_at, id",
                statement -> statement.setLong(1, goalId));
    }

    @Override
    public List<GoalAlert> listPending() {
        return query(SELECT + " WHERE outcome IS NULL ORDER BY triggered_at, id",
                statement -> { });
    }

    @Override
    public List<GoalAlert> listBetween(Instant from, Instant to) {
        Objects.requireNonNull(from, "from");
        Objects.requireNonNull(to, "to");
        return query(SELECT + " WHERE triggered_at >= ? AND triggered_at < ? ORDER BY triggered_at, id",
                statement -> {
                    statement.setLong(1, from.toEpochMilli());
                    statement.setLong(2, to.toEpochMilli());
                });
    }

    @Override
    public List<GoalAlert> listJustifiedBetween(Instant from, Instant to) {
        Objects.requireNonNull(from, "from");
        Objects.requireNonNull(to, "to");
        return query(SELECT + " WHERE justified_at >= ? AND justified_at < ? ORDER BY justified_at, id",
                statement -> {
                    statement.setLong(1, from.toEpochMilli());
                    statement.setLong(2, to.toEpochMilli());
                });
    }

    private List<GoalAlert> query(String sql, Binder binder) {
        try (Connection connection = SQLiteSchema.open(databasePath);
             PreparedStatement statement = connection.prepareStatement(sql)) {
            binder.bind(statement);
            try (ResultSet result = statement.executeQuery()) {
                List<GoalAlert> alerts = new ArrayList<>();
                while (result.next()) alerts.add(read(result));
                return List.copyOf(alerts);
            }
        } catch (SQLException e) {
            throw new StorageException("Could not read goal alerts", e);
        }
    }

    private static void bindInsert(PreparedStatement statement, GoalAlert alert) throws SQLException {
        statement.setLong(1, alert.goalId());
        bindValues(statement, 2, alert);
    }

    private static void bindUpdate(PreparedStatement statement, GoalAlert alert) throws SQLException {
        bindValues(statement, 1, alert);
        statement.setLong(8, alert.id());
    }

    private static void bindValues(PreparedStatement statement, int first, GoalAlert alert)
            throws SQLException {
        statement.setLong(first, alert.triggeredAt().toEpochMilli());
        statement.setInt(first + 1, alert.thresholdPercent());
        statement.setString(first + 2, alert.metric().name());
        statement.setString(first + 3, alert.outcome());
        statement.setString(first + 4, alert.justification());
        if (alert.justifiedAt() == null) statement.setNull(first + 5, Types.BIGINT);
        else statement.setLong(first + 5, alert.justifiedAt().toEpochMilli());
        if (alert.justifiedExcess() == null) statement.setNull(first + 6, Types.BIGINT);
        else statement.setLong(first + 6, alert.justifiedExcess());
    }

    private static long lastInsertId(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery("SELECT last_insert_rowid()")) {
            if (!result.next()) throw new StorageException("Could not obtain goal alert id");
            return result.getLong(1);
        }
    }

    private static GoalAlert read(ResultSet result) throws SQLException {
        long excessValue = result.getLong("justified_excess");
        Long excess = result.wasNull() ? null : excessValue;
        long momentValue = result.getLong("justified_at");
        Instant moment = result.wasNull() ? null : Instant.ofEpochMilli(momentValue);
        return new GoalAlert(result.getLong("id"), result.getLong("goal_id"),
                Instant.ofEpochMilli(result.getLong("triggered_at")),
                result.getInt("threshold_percent"),
                GoalMetric.valueOf(result.getString("metric")),
                result.getString("outcome"), result.getString("justification"),
                moment, excess);
    }

    @FunctionalInterface
    private interface Binder {
        void bind(PreparedStatement statement) throws SQLException;
    }
}
