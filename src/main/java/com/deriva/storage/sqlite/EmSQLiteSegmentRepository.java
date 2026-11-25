package com.deriva.storage.sqlite;

import com.deriva.privacy.EmObservationKind;
import com.deriva.storage.EmStorageException;
import com.deriva.storage.port.EmSegmentRepository;
import com.deriva.tracking.model.EmContextKey;
import com.deriva.tracking.model.EmSegment;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** JDBC adapter for segments. Writes are one transaction; reads use semi-open overlap. */
public final class EmSQLiteSegmentRepository implements EmSegmentRepository {
    private static final String INSERT = """
            INSERT INTO segment(kind, process_name, site_host, site_label, site_source, start_ts, end_ts)
            VALUES (?, ?, ?, ?, ?, ?, ?)
            """;
    private static final String OVERLAP = """
            SELECT id, kind, process_name, site_host, site_label, site_source, start_ts, end_ts
            FROM segment
            WHERE start_ts < ? AND end_ts > ?
            ORDER BY start_ts, id
            """;

    private final Path databasePath;

    public EmSQLiteSegmentRepository(Path databasePath) {
        this.databasePath = Objects.requireNonNull(databasePath, "databasePath").toAbsolutePath();
        EmSQLiteSchema.initialize(this.databasePath);
    }

    @Override
    public void saveAll(List<EmSegment> segments) {
        Objects.requireNonNull(segments, "segments");
        if (segments.isEmpty()) return;

        try (Connection connection = EmSQLiteSchema.open(databasePath)) {
            connection.setAutoCommit(false);
            try (PreparedStatement statement = connection.prepareStatement(INSERT)) {
                for (EmSegment segment : segments) {
                    if (segment == null) throw new IllegalArgumentException("segment");
                    bind(statement, segment);
                    statement.addBatch();
                }
                statement.executeBatch();
                connection.commit();
            } catch (SQLException | RuntimeException e) {
                connection.rollback();
                if (e instanceof RuntimeException runtime) throw runtime;
                throw new EmStorageException("Could not save segments", e);
            }
        } catch (SQLException e) {
            throw new EmStorageException("Could not open SQLite database", e);
        }
    }

    @Override
    public List<EmSegment> between(Instant from, Instant to) {
        Objects.requireNonNull(from, "from");
        Objects.requireNonNull(to, "to");
        if (!from.isBefore(to)) return List.of();

        try (Connection connection = EmSQLiteSchema.open(databasePath);
             PreparedStatement statement = connection.prepareStatement(OVERLAP)) {
            statement.setLong(1, to.toEpochMilli());
            statement.setLong(2, from.toEpochMilli());
            try (ResultSet result = statement.executeQuery()) {
                List<EmSegment> segments = new ArrayList<>();
                while (result.next()) segments.add(read(result));
                return List.copyOf(segments);
            }
        } catch (SQLException e) {
            throw new EmStorageException("Could not read segments", e);
        }
    }

    @Override
    public List<EmContextKey> observedContexts() {
        String sql = """
                SELECT DISTINCT process_name, site_host
                FROM segment
                WHERE kind = 'OBSERVED'
                ORDER BY process_name, COALESCE(site_host, '')
                """;
        try (Connection connection = EmSQLiteSchema.open(databasePath);
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet result = statement.executeQuery()) {
            List<EmContextKey> contexts = new ArrayList<>();
            while (result.next()) {
                String host = result.getString("site_host");
                contexts.add(new EmContextKey(result.getString("process_name"),
                        host == null || host.isBlank() ? null : host));
            }
            return List.copyOf(contexts);
        } catch (SQLException e) {
            throw new EmStorageException("Could not read observed contexts", e);
        }
    }

    private static void bind(PreparedStatement statement, EmSegment segment) throws SQLException {
        statement.setString(1, segment.kind().name());
        if (segment.kind() == EmObservationKind.OBSERVED) {
            statement.setString(2, segment.context().processName());
            statement.setString(3, segment.context().siteHost());
        } else {
            statement.setString(2, "__" + segment.kind().name() + "__");
            statement.setString(3, null);
        }
        statement.setString(4, segment.siteLabel());
        statement.setString(5, segment.siteSource());
        statement.setLong(6, segment.start().toEpochMilli());
        statement.setLong(7, segment.end().toEpochMilli());
    }

    private static EmSegment read(ResultSet result) throws SQLException {
        EmObservationKind kind = EmObservationKind.valueOf(result.getString("kind"));
        String process = result.getString("process_name");
        String host = result.getString("site_host");
        String label = result.getString("site_label");
        String source = result.getString("site_source");

        if (kind != EmObservationKind.OBSERVED && (host != null || label != null || source != null))
            throw new EmStorageException("Invalid identity on non-observed segment");

        EmContextKey context = kind == EmObservationKind.OBSERVED ? new EmContextKey(process, host) : null;
        return new EmSegment(result.getLong("id"), kind, context, label, source,
                Instant.ofEpochMilli(result.getLong("start_ts")),
                Instant.ofEpochMilli(result.getLong("end_ts")));
    }
}
