package com.deriva.storage.sqlite;

import com.deriva.goals.model.EmGoalAlert;
import com.deriva.goals.model.EmGoalMetric;
import com.deriva.storage.EmStorageException;
import com.deriva.storage.port.EmGoalAlertRepository;

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

/** JDBC adapter for goal alerts. Archived goals keep their alerts for history but no longer produce pending notices. */
public final class EmSQLiteGoalAlertRepository implements EmGoalAlertRepository {
    private static final String INSERT = """
            INSERT INTO goal_alerts(goal_id, triggered_at, threshold_percent, metric, outcome,
                                    justification, justified_at, justified_excess,
                                    notified_at, observed_until, outcome_at, outcome_after_ms)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;
    private static final String UPDATE = """
            UPDATE goal_alerts
            SET triggered_at = ?, threshold_percent = ?, metric = ?, outcome = ?,
                justification = ?, justified_at = ?, justified_excess = ?,
                notified_at = ?, observed_until = ?, outcome_at = ?, outcome_after_ms = ?
            WHERE id = ?
            """;
    private static final String SELECT = """
            SELECT id, goal_id, triggered_at, threshold_percent, metric, outcome,
                   justification, justified_at, justified_excess,
                   notified_at, observed_until, outcome_at, outcome_after_ms
            FROM goal_alerts
            """;

    private final Path databasePath;

    public EmSQLiteGoalAlertRepository(Path databasePath) {
        this.databasePath = Objects.requireNonNull(databasePath, "databasePath").toAbsolutePath();
        EmSQLiteSchema.initialize(this.databasePath);
    }

    @Override
    public EmGoalAlert create(EmGoalAlert alert) {
        Objects.requireNonNull(alert, "alert");
        if (alert.id() != 0) throw new IllegalArgumentException("new alert must not have an id");

        try (Connection connection = EmSQLiteSchema.open(databasePath)) {
            connection.setAutoCommit(false);
            try {
                try (PreparedStatement statement = connection.prepareStatement(INSERT)) {
                    bindInsert(statement, alert);
                    statement.executeUpdate();
                }
                long id = lastInsertId(connection);
                connection.commit();
                return new EmGoalAlert(id, alert.goalId(), alert.triggeredAt(), alert.thresholdPercent(),
                        alert.metric(), alert.outcome(), alert.justification(),
                        alert.justifiedAt(), alert.justifiedExcess(),
                        alert.notifiedAt(), alert.observedUntil(), alert.outcomeAt(),
                        alert.outcomeAfterMs());
            } catch (SQLException | RuntimeException e) {
                connection.rollback();
                if (e instanceof RuntimeException runtime) throw runtime;
                throw new EmStorageException("Could not create goal alert", e);
            }
        } catch (SQLException e) {
            throw new EmStorageException("Could not open SQLite database", e);
        }
    }

    @Override
    public Optional<EmGoalAlert> find(long id) {
        if (id <= 0) throw new IllegalArgumentException("id");
        return query(SELECT + " WHERE id = ?", statement -> statement.setLong(1, id))
                .stream().findFirst();
    }

    @Override
    public void update(EmGoalAlert alert) {
        Objects.requireNonNull(alert, "alert");
        if (alert.id() <= 0) throw new IllegalArgumentException("id");

        try (Connection connection = EmSQLiteSchema.open(databasePath);
             PreparedStatement statement = connection.prepareStatement(UPDATE)) {
            bindUpdate(statement, alert);
            if (statement.executeUpdate() != 1)
                throw new EmStorageException("Unknown goal alert: " + alert.id());
        } catch (SQLException e) {
            throw new EmStorageException("Could not update goal alert", e);
        }
    }

    @Override
    public void updateAll(List<EmGoalAlert> alerts) {
        Objects.requireNonNull(alerts, "alerts");
        if (alerts.isEmpty()) return;
        for (EmGoalAlert alert : alerts) {
            Objects.requireNonNull(alert, "alert");
            if (alert.id() <= 0) throw new IllegalArgumentException("id");
        }
        try (Connection connection = EmSQLiteSchema.open(databasePath)) {
            connection.setAutoCommit(false);
            try (PreparedStatement statement = connection.prepareStatement(UPDATE)) {
                for (EmGoalAlert alert : alerts) {
                    bindUpdate(statement, alert);
                    if (statement.executeUpdate() != 1)
                        throw new EmStorageException("Unknown goal alert: " + alert.id());
                }
                connection.commit();
            } catch (SQLException | RuntimeException e) {
                connection.rollback();
                if (e instanceof RuntimeException runtime) throw runtime;
                throw new EmStorageException("Could not update goal alerts", e);
            }
        } catch (SQLException e) {
            throw new EmStorageException("Could not open SQLite database", e);
        }
    }

    @Override
    public boolean delete(long id) {
        if (id <= 0) throw new IllegalArgumentException("id");
        try (Connection connection = EmSQLiteSchema.open(databasePath);
             PreparedStatement statement = connection.prepareStatement("DELETE FROM goal_alerts WHERE id = ?")) {
            statement.setLong(1, id);
            return statement.executeUpdate() == 1;
        } catch (SQLException e) {
            throw new EmStorageException("Could not delete goal alert", e);
        }
    }

    @Override
    public List<EmGoalAlert> listByGoal(long goalId) {
        if (goalId <= 0) throw new IllegalArgumentException("goalId");
        return query(SELECT + " WHERE goal_id = ? ORDER BY triggered_at, id",
                statement -> statement.setLong(1, goalId));
    }

    @Override
    public List<EmGoalAlert> listPending() {
        return query(SELECT + " WHERE outcome IS NULL"
                + " AND goal_id IN (SELECT id FROM goals WHERE archived_at IS NULL)"
                + " ORDER BY triggered_at, id", statement -> { });
    }

    @Override
    public List<EmGoalAlert> listBetween(Instant from, Instant to) {
        Objects.requireNonNull(from, "from");
        Objects.requireNonNull(to, "to");
        return query(SELECT + " WHERE triggered_at >= ? AND triggered_at < ? ORDER BY triggered_at, id",
                statement -> {
                    statement.setLong(1, from.toEpochMilli());
                    statement.setLong(2, to.toEpochMilli());
                });
    }

    @Override
    public List<EmGoalAlert> listJustifiedBetween(Instant from, Instant to) {
        Objects.requireNonNull(from, "from");
        Objects.requireNonNull(to, "to");
        return query(SELECT + " WHERE justified_at >= ? AND justified_at < ? ORDER BY justified_at, id",
                statement -> {
                    statement.setLong(1, from.toEpochMilli());
                    statement.setLong(2, to.toEpochMilli());
                });
    }

    private List<EmGoalAlert> query(String sql, Binder binder) {
        try (Connection connection = EmSQLiteSchema.open(databasePath);
             PreparedStatement statement = connection.prepareStatement(sql)) {
            binder.bind(statement);
            try (ResultSet result = statement.executeQuery()) {
                List<EmGoalAlert> alerts = new ArrayList<>();
                while (result.next()) alerts.add(read(result));
                return List.copyOf(alerts);
            }
        } catch (SQLException e) {
            throw new EmStorageException("Could not read goal alerts", e);
        }
    }

    private static void bindInsert(PreparedStatement statement, EmGoalAlert alert) throws SQLException {
        statement.setLong(1, alert.goalId());
        bindValues(statement, 2, alert);
    }

    private static void bindUpdate(PreparedStatement statement, EmGoalAlert alert) throws SQLException {
        bindValues(statement, 1, alert);
        statement.setLong(12, alert.id());
    }

    private static void bindValues(PreparedStatement statement, int first, EmGoalAlert alert)
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
        bindInstant(statement, first + 7, alert.notifiedAt());
        bindInstant(statement, first + 8, alert.observedUntil());
        bindInstant(statement, first + 9, alert.outcomeAt());
        if (alert.outcomeAfterMs() == null) statement.setNull(first + 10, Types.BIGINT);
        else statement.setLong(first + 10, alert.outcomeAfterMs());
    }

    private static void bindInstant(PreparedStatement statement, int index, Instant value)
            throws SQLException {
        if (value == null) statement.setNull(index, Types.BIGINT);
        else statement.setLong(index, value.toEpochMilli());
    }

    private static long lastInsertId(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery("SELECT last_insert_rowid()")) {
            if (!result.next()) throw new EmStorageException("Could not obtain goal alert id");
            return result.getLong(1);
        }
    }

    private static EmGoalAlert read(ResultSet result) throws SQLException {
        long excessValue = result.getLong("justified_excess");
        Long excess = result.wasNull() ? null : excessValue;
        long momentValue = result.getLong("justified_at");
        Instant moment = result.wasNull() ? null : Instant.ofEpochMilli(momentValue);
        Instant notifiedAt = instant(result, "notified_at");
        Instant observedUntil = instant(result, "observed_until");
        Instant outcomeAt = instant(result, "outcome_at");
        long afterValue = result.getLong("outcome_after_ms");
        Long afterMs = result.wasNull() ? null : afterValue;
        return new EmGoalAlert(result.getLong("id"), result.getLong("goal_id"),
                Instant.ofEpochMilli(result.getLong("triggered_at")),
                result.getInt("threshold_percent"),
                EmGoalMetric.valueOf(result.getString("metric")),
                result.getString("outcome"), result.getString("justification"),
                moment, excess, notifiedAt, observedUntil, outcomeAt, afterMs);
    }

    private static Instant instant(ResultSet result, String column) throws SQLException {
        long value = result.getLong(column);
        return result.wasNull() ? null : Instant.ofEpochMilli(value);
    }

    @FunctionalInterface
    private interface Binder {
        void bind(PreparedStatement statement) throws SQLException;
    }
}
