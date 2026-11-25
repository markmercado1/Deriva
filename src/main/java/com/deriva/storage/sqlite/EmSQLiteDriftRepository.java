package com.deriva.storage.sqlite;

import com.deriva.coach.model.EmCloseReason;
import com.deriva.coach.model.EmDriftEpisode;
import com.deriva.coach.model.EmDriftEvent;
import com.deriva.coach.model.EmDriftStatus;
import com.deriva.coach.model.EmDriftUpdate;
import com.deriva.storage.EmStorageException;
import com.deriva.storage.port.EmDriftRepository;
import com.deriva.tracking.model.EmContextKey;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/** SQLite adapter for drift episodes, contexts and annotations. */
public final class EmSQLiteDriftRepository implements EmDriftRepository {
    private static final String INSERT_EVENT = """
            INSERT INTO drift_event(start_ts, detected_at, last_observed_ts, stability_start_ts, closed_at,
                                    status, close_reason, switch_count, first_change_process, first_change_host,
                                    time_to_stability_ms)
            VALUES (?, ?, ?, NULL, NULL, 'OPEN', NULL, ?, ?, ?, NULL)
            """;
    private static final String UPDATE_EVENT = """
            UPDATE drift_event
            SET last_observed_ts = ?, stability_start_ts = ?, closed_at = ?, status = ?, close_reason = ?,
                switch_count = ?, time_to_stability_ms = ?
            WHERE id = ?
            """;
    private static final String INSERT_CONTEXT = """
            INSERT OR IGNORE INTO drift_context(event_id, process_name, site_host) VALUES (?, ?, ?)
            """;
    private static final String SELECT_EVENT = """
            SELECT id, start_ts, detected_at, last_observed_ts, stability_start_ts, closed_at,
                   status, close_reason, switch_count, first_change_process, first_change_host,
                   time_to_stability_ms, reason, justified, answered_at
            FROM drift_event
            WHERE start_ts < ? AND COALESCE(closed_at, last_observed_ts) > ?
            ORDER BY start_ts, id
            """;

    private final Path databasePath;
    private final Map<Long, Long> sessionIds = new HashMap<>();

    public EmSQLiteDriftRepository(Path databasePath) {
        this.databasePath = Objects.requireNonNull(databasePath, "databasePath").toAbsolutePath();
        EmSQLiteSchema.initialize(this.databasePath);
    }

    @Override
    public void upsertDetection(EmDriftUpdate update) {
        Objects.requireNonNull(update, "update");
        try (Connection connection = EmSQLiteSchema.open(databasePath)) {
            connection.setAutoCommit(false);
            try {
                long eventId = update.type() == EmDriftUpdate.Type.OPENED
                        ? insert(connection, update.episode())
                        : updateRow(connection, update.episode());
                insertContexts(connection, eventId, update.episode().contexts());
                connection.commit();
            } catch (SQLException | RuntimeException e) {
                connection.rollback();
                if (e instanceof RuntimeException runtime) throw runtime;
                throw new EmStorageException("Could not persist drift update", e);
            }
        } catch (SQLException e) {
            throw new EmStorageException("Could not open SQLite database", e);
        }
    }

    @Override
    public void annotate(long id, String reason, boolean justified, Instant answeredAt) {
        if (id <= 0) throw new IllegalArgumentException("id");
        if (!EmDriftEvent.isValidReason(reason)) throw new IllegalArgumentException("reason");
        try (Connection connection = EmSQLiteSchema.open(databasePath);
             PreparedStatement statement = connection.prepareStatement(
                     "UPDATE drift_event SET reason = ?, justified = ?, answered_at = ? WHERE id = ?")) {
            statement.setString(1, reason);
            statement.setInt(2, justified ? 1 : 0);
            setNullableInstant(statement, 3, answeredAt);
            statement.setLong(4, id);
            if (statement.executeUpdate() != 1) throw new EmStorageException("Unknown drift event: " + id);
        } catch (SQLException e) {
            throw new EmStorageException("Could not annotate drift event", e);
        }
    }

    @Override
    public List<EmDriftEvent> between(Instant from, Instant to) {
        Objects.requireNonNull(from, "from");
        Objects.requireNonNull(to, "to");
        if (!from.isBefore(to)) return List.of();

        try (Connection connection = EmSQLiteSchema.open(databasePath);
             PreparedStatement statement = connection.prepareStatement(SELECT_EVENT)) {
            statement.setLong(1, to.toEpochMilli());
            statement.setLong(2, from.toEpochMilli());
            try (ResultSet result = statement.executeQuery()) {
                List<EmDriftEvent> events = new ArrayList<>();
                while (result.next()) events.add(read(connection, result));
                return List.copyOf(events);
            }
        } catch (SQLException e) {
            throw new EmStorageException("Could not read drift events", e);
        }
    }

    @Override
    public int recoverOpenEpisodes() {
        try (Connection connection = EmSQLiteSchema.open(databasePath)) {
            connection.setAutoCommit(false);
            try (PreparedStatement select = connection.prepareStatement(
                    "SELECT id, last_observed_ts FROM drift_event WHERE status = 'OPEN' ORDER BY id");
                 PreparedStatement update = connection.prepareStatement("""
                         UPDATE drift_event
                         SET status = 'UNCONFIRMED', close_reason = 'RESTART', closed_at = last_observed_ts,
                             stability_start_ts = NULL, time_to_stability_ms = NULL
                         WHERE id = ? AND status = 'OPEN'
                         """)) {
                int count = 0;
                try (ResultSet result = select.executeQuery()) {
                    while (result.next()) {
                        update.setLong(1, result.getLong("id"));
                        count += update.executeUpdate();
                    }
                }
                connection.commit();
                return count;
            } catch (SQLException e) {
                connection.rollback();
                throw new EmStorageException("Could not recover open drift episodes", e);
            }
        } catch (SQLException e) {
            throw new EmStorageException("Could not open SQLite database", e);
        }
    }

    private long insert(Connection connection, EmDriftEpisode episode) throws SQLException {
        if (episode.status() != EmDriftStatus.OPEN)
            throw new IllegalArgumentException("OPENED update without OPEN episode");
        try (PreparedStatement statement = connection.prepareStatement(INSERT_EVENT)) {
            statement.setLong(1, episode.startTs().toEpochMilli());
            statement.setLong(2, episode.detectedAt().toEpochMilli());
            statement.setLong(3, episode.lastObservedTs().toEpochMilli());
            statement.setInt(4, episode.switchCount());
            statement.setString(5, episode.firstChangeContext().processName());
            statement.setString(6, episode.firstChangeContext().siteHost());
            statement.executeUpdate();
        }
        long id;
        try (var statement = connection.createStatement();
             ResultSet result = statement.executeQuery("SELECT last_insert_rowid()")) {
            if (!result.next()) throw new EmStorageException("Could not obtain drift event id");
            id = result.getLong(1);
        }
        Long previous = sessionIds.put(episode.id(), id);
        if (previous != null && previous != id)
            throw new EmStorageException("Duplicate in-memory drift id: " + episode.id());
        return id;
    }

    private long updateRow(Connection connection, EmDriftEpisode episode) throws SQLException {
        Long eventId = sessionIds.get(episode.id());
        if (eventId == null) throw new EmStorageException("Drift episode was not opened: " + episode.id());
        try (PreparedStatement statement = connection.prepareStatement(UPDATE_EVENT)) {
            statement.setLong(1, episode.lastObservedTs().toEpochMilli());
            setNullableInstant(statement, 2, episode.stabilityStartTs());
            setNullableInstant(statement, 3, episode.closedAt());
            statement.setString(4, episode.status().name());
            statement.setString(5, episode.closeReason() == null ? null : episode.closeReason().name());
            statement.setInt(6, episode.switchCount());
            if (episode.timeToStabilityMs() == null) statement.setNull(7, Types.BIGINT);
            else statement.setLong(7, episode.timeToStabilityMs());
            statement.setLong(8, eventId);
            if (statement.executeUpdate() != 1)
                throw new EmStorageException("Unknown drift event: " + eventId);
        }
        return eventId;
    }

    private static void insertContexts(Connection connection, long eventId, Set<EmContextKey> contexts)
            throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(INSERT_CONTEXT)) {
            for (EmContextKey context : contexts) {
                statement.setLong(1, eventId);
                statement.setString(2, context.processName());
                statement.setString(3, context.siteHost() == null ? "" : context.siteHost());
                statement.addBatch();
            }
            statement.executeBatch();
        }
    }

    private static EmDriftEvent read(Connection connection, ResultSet result) throws SQLException {
        long id = result.getLong("id");
        String firstHost = result.getString("first_change_host");
        EmContextKey firstContext = new EmContextKey(result.getString("first_change_process"),
                firstHost == null || firstHost.isBlank() ? null : firstHost);
        long time = result.getLong("time_to_stability_ms");
        Long timeToStability = result.wasNull() ? null : time;
        String reason = result.getString("reason");
        long answered = result.getLong("answered_at");
        Instant answeredAt = result.wasNull() ? null : Instant.ofEpochMilli(answered);

        return new EmDriftEvent(id,
                Instant.ofEpochMilli(result.getLong("start_ts")),
                Instant.ofEpochMilli(result.getLong("detected_at")),
                Instant.ofEpochMilli(result.getLong("last_observed_ts")),
                instantOrNull(result, "stability_start_ts"),
                instantOrNull(result, "closed_at"),
                EmDriftStatus.valueOf(result.getString("status")),
                closeReasonOrNull(result.getString("close_reason")),
                result.getInt("switch_count"),
                firstContext,
                timeToStability,
                reason,
                result.getInt("justified") != 0,
                answeredAt,
                readContexts(connection, id));
    }

    private static Set<EmContextKey> readContexts(Connection connection, long eventId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT process_name, site_host FROM drift_context WHERE event_id = ? ORDER BY process_name, site_host")) {
            statement.setLong(1, eventId);
            Set<EmContextKey> contexts = new LinkedHashSet<>();
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    String host = result.getString("site_host");
                    contexts.add(new EmContextKey(result.getString("process_name"),
                            host == null || host.isBlank() ? null : host));
                }
            }
            return contexts;
        }
    }

    private static Instant instantOrNull(ResultSet result, String column) throws SQLException {
        long value = result.getLong(column);
        return result.wasNull() ? null : Instant.ofEpochMilli(value);
    }

    private static EmCloseReason closeReasonOrNull(String value) {
        return value == null ? null : EmCloseReason.valueOf(value);
    }

    private static void setNullableInstant(PreparedStatement statement, int index, Instant value)
            throws SQLException {
        if (value == null) statement.setNull(index, Types.BIGINT);
        else statement.setLong(index, value.toEpochMilli());
    }
}
