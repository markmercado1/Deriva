package com.deriva.storage.sqlite;

import com.deriva.privacy.ObservationKind;
import com.deriva.storage.StorageException;
import com.deriva.storage.port.SegmentRepository;
import com.deriva.tracking.model.ContextKey;
import com.deriva.tracking.model.Segment;

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
public final class SQLiteSegmentRepository implements SegmentRepository {
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

    public SQLiteSegmentRepository(Path databasePath) {
        this.databasePath = Objects.requireNonNull(databasePath, "databasePath").toAbsolutePath();
        SQLiteSchema.initialize(this.databasePath);
    }

    @Override
    public void saveAll(List<Segment> segments) {
        Objects.requireNonNull(segments, "segments");
        if (segments.isEmpty()) return;

        try (Connection connection = SQLiteSchema.open(databasePath)) {
            connection.setAutoCommit(false);
            try (PreparedStatement statement = connection.prepareStatement(INSERT)) {
                for (Segment segment : segments) {
                    if (segment == null) throw new IllegalArgumentException("segment");
                    bind(statement, segment);
                    statement.addBatch();
                }
                statement.executeBatch();
                connection.commit();
            } catch (SQLException | RuntimeException e) {
                connection.rollback();
                if (e instanceof RuntimeException runtime) throw runtime;
                throw new StorageException("Could not save segments", e);
            }
        } catch (SQLException e) {
            throw new StorageException("Could not open SQLite database", e);
        }
    }

    @Override
    public List<Segment> between(Instant from, Instant to) {
        Objects.requireNonNull(from, "from");
        Objects.requireNonNull(to, "to");
        if (!from.isBefore(to)) return List.of();

        try (Connection connection = SQLiteSchema.open(databasePath);
             PreparedStatement statement = connection.prepareStatement(OVERLAP)) {
            statement.setLong(1, to.toEpochMilli());
            statement.setLong(2, from.toEpochMilli());
            try (ResultSet result = statement.executeQuery()) {
                List<Segment> segments = new ArrayList<>();
                while (result.next()) segments.add(read(result));
                return List.copyOf(segments);
            }
        } catch (SQLException e) {
            throw new StorageException("Could not read segments", e);
        }
    }

    private static void bind(PreparedStatement statement, Segment segment) throws SQLException {
        statement.setString(1, segment.kind().name());
        if (segment.kind() == ObservationKind.OBSERVED) {
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

    private static Segment read(ResultSet result) throws SQLException {
        ObservationKind kind = ObservationKind.valueOf(result.getString("kind"));
        String process = result.getString("process_name");
        String host = result.getString("site_host");
        String label = result.getString("site_label");
        String source = result.getString("site_source");

        if (kind != ObservationKind.OBSERVED && (host != null || label != null || source != null))
            throw new StorageException("Invalid identity on non-observed segment");

        ContextKey context = kind == ObservationKind.OBSERVED ? new ContextKey(process, host) : null;
        return new Segment(result.getLong("id"), kind, context, label, source,
                Instant.ofEpochMilli(result.getLong("start_ts")),
                Instant.ofEpochMilli(result.getLong("end_ts")));
    }
}
