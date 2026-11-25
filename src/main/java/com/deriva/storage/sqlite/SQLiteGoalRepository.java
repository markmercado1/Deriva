package com.deriva.storage.sqlite;

import com.deriva.goals.model.Goal;
import com.deriva.storage.StorageException;
import com.deriva.storage.port.GoalRepository;
import com.deriva.tracking.model.ContextKey;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** JDBC adapter for goals. Creation and modification times come from the injected clock. */
public final class SQLiteGoalRepository implements GoalRepository {
    private static final String INSERT = """
            INSERT INTO goals(process_name, site_host, daily_minutes, daily_entries, passive, active,
                              created_at, updated_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            """;
    private static final String UPDATE = """
            UPDATE goals
            SET process_name = ?, site_host = ?, daily_minutes = ?, daily_entries = ?, passive = ?, active = ?,
                updated_at = ?
            WHERE id = ? AND archived_at IS NULL
            """;
    private static final String SELECT = """
            SELECT id, process_name, site_host, daily_minutes, daily_entries, passive, active,
                   created_at, updated_at
            FROM goals
            """;

    private final Path databasePath;
    private final Clock clock;

    public SQLiteGoalRepository(Path databasePath) {
        this(databasePath, Clock.systemUTC());
    }

    public SQLiteGoalRepository(Path databasePath, Clock clock) {
        this.databasePath = Objects.requireNonNull(databasePath, "databasePath").toAbsolutePath();
        this.clock = Objects.requireNonNull(clock, "clock");
        SQLiteSchema.initialize(this.databasePath);
    }

    @Override
    public Goal create(Goal goal) {
        Objects.requireNonNull(goal, "goal");
        if (goal.id() != 0) throw new IllegalArgumentException("new goal must not have an id");
        Instant now = clock.instant();

        try (Connection connection = SQLiteSchema.open(databasePath)) {
            connection.setAutoCommit(false);
            try {
                try (PreparedStatement statement = connection.prepareStatement(INSERT)) {
                    bindContext(statement, 1, goal.context());
                    bindLimits(statement, 3, goal);
                    statement.setInt(5, goal.passive() ? 1 : 0);
                    statement.setInt(6, goal.active() ? 1 : 0);
                    statement.setLong(7, now.toEpochMilli());
                    statement.setLong(8, now.toEpochMilli());
                    statement.executeUpdate();
                }
                long id = lastInsertId(connection);
                connection.commit();
                return new Goal(id, goal.context(), goal.dailyMinutes(), goal.dailyEntries(),
                        goal.passive(), goal.active(), now, now);
            } catch (SQLException | RuntimeException e) {
                connection.rollback();
                if (e instanceof RuntimeException runtime) throw runtime;
                throw new StorageException("Could not create goal", e);
            }
        } catch (SQLException e) {
            throw new StorageException("Could not open SQLite database", e);
        }
    }

    @Override
    public Optional<Goal> find(long id) {
        if (id <= 0) throw new IllegalArgumentException("id");
        try (Connection connection = SQLiteSchema.open(databasePath);
             PreparedStatement statement = connection.prepareStatement(SELECT + " WHERE id = ? AND archived_at IS NULL")) {
            statement.setLong(1, id);
            try (ResultSet result = statement.executeQuery()) {
                return result.next() ? Optional.of(read(result)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new StorageException("Could not read goal", e);
        }
    }

    @Override
    public void update(Goal goal) {
        Objects.requireNonNull(goal, "goal");
        if (goal.id() <= 0) throw new IllegalArgumentException("id");
        Instant now = clock.instant();

        try (Connection connection = SQLiteSchema.open(databasePath);
             PreparedStatement statement = connection.prepareStatement(UPDATE)) {
            bindContext(statement, 1, goal.context());
            bindLimits(statement, 3, goal);
            statement.setInt(5, goal.passive() ? 1 : 0);
            statement.setInt(6, goal.active() ? 1 : 0);
            statement.setLong(7, now.toEpochMilli());
            statement.setLong(8, goal.id());
            if (statement.executeUpdate() != 1) throw new StorageException("Unknown goal: " + goal.id());
        } catch (SQLException e) {
            throw new StorageException("Could not update goal", e);
        }
    }

    @Override
    public boolean delete(long id) {
        if (id <= 0) throw new IllegalArgumentException("id");
        try (Connection connection = SQLiteSchema.open(databasePath);
             PreparedStatement statement = connection.prepareStatement(
                     "UPDATE goals SET archived_at = ?, updated_at = ? WHERE id = ? AND archived_at IS NULL")) {
            long now = clock.instant().toEpochMilli();
            statement.setLong(1, now);
            statement.setLong(2, now);
            statement.setLong(3, id);
            return statement.executeUpdate() == 1;
        } catch (SQLException e) {
            throw new StorageException("Could not delete goal", e);
        }
    }

    @Override
    public List<Goal> listActive() {
        return list(" WHERE active = 1 AND archived_at IS NULL ORDER BY process_name, COALESCE(site_host, '')");
    }

    @Override
    public List<Goal> listAll() {
        return list(" WHERE archived_at IS NULL ORDER BY active DESC, process_name, COALESCE(site_host, '')");
    }

    @Override
    public List<com.deriva.goals.model.GoalRevision> revisionsBefore(Instant end) {
        try (Connection connection = SQLiteSchema.open(databasePath);
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT goal_id AS id, * FROM goal_history WHERE recorded_at < ? ORDER BY recorded_at,revision")) {
            statement.setLong(1, end.toEpochMilli());
            try (ResultSet result = statement.executeQuery()) {
                var revisions = new ArrayList<com.deriva.goals.model.GoalRevision>();
                while (result.next()) revisions.add(new com.deriva.goals.model.GoalRevision(read(result),
                        Instant.ofEpochMilli(result.getLong("recorded_at")),
                        result.getInt("archived") != 0, result.getInt("baseline") != 0));
                return List.copyOf(revisions);
            }
        } catch (SQLException e) {
            throw new StorageException("Could not read goal history", e);
        }
    }

    private List<Goal> list(String clause) {
        try (Connection connection = SQLiteSchema.open(databasePath);
             PreparedStatement statement = connection.prepareStatement(SELECT + clause);
             ResultSet result = statement.executeQuery()) {
            List<Goal> goals = new ArrayList<>();
            while (result.next()) goals.add(read(result));
            return List.copyOf(goals);
        } catch (SQLException e) {
            throw new StorageException("Could not read goals", e);
        }
    }

    private static void bindContext(PreparedStatement statement, int index, ContextKey context)
            throws SQLException {
        statement.setString(index, context.processName());
        statement.setString(index + 1, context.siteHost());
    }

    private static void bindLimits(PreparedStatement statement, int index, Goal goal) throws SQLException {
        if (goal.dailyMinutes() == null) statement.setNull(index, Types.INTEGER);
        else statement.setInt(index, goal.dailyMinutes());
        if (goal.dailyEntries() == null) statement.setNull(index + 1, Types.INTEGER);
        else statement.setInt(index + 1, goal.dailyEntries());
    }

    private static long lastInsertId(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery("SELECT last_insert_rowid()")) {
            if (!result.next()) throw new StorageException("Could not obtain goal id");
            return result.getLong(1);
        }
    }

    private static Goal read(ResultSet result) throws SQLException {
        String host = result.getString("site_host");
        ContextKey context = new ContextKey(result.getString("process_name"),
                host == null || host.isBlank() ? null : host);
        return new Goal(result.getLong("id"), context,
                nullableInt(result, "daily_minutes"), nullableInt(result, "daily_entries"),
                result.getInt("passive") != 0, result.getInt("active") != 0,
                Instant.ofEpochMilli(result.getLong("created_at")),
                Instant.ofEpochMilli(result.getLong("updated_at")));
    }

    private static Integer nullableInt(ResultSet result, String column) throws SQLException {
        int value = result.getInt(column);
        return result.wasNull() ? null : value;
    }
}
