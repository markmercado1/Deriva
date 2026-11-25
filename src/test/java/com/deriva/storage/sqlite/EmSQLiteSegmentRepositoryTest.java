package com.deriva.storage.sqlite;

import com.deriva.privacy.EmObservationKind;
import com.deriva.storage.EmStorageException;
import com.deriva.tracking.model.EmContextKey;
import com.deriva.tracking.model.EmSegment;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class EmSQLiteSegmentRepositoryTest {
    private static final Instant BASE = Instant.parse("2026-11-22T10:00:00Z");

    @TempDir Path tempDirectory;

    private static Instant at(int second) {
        return BASE.plusSeconds(second);
    }

    private Path database() {
        return tempDirectory.resolve("deriva.db");
    }

    private static EmSegment observed(String process, String host, int start, int end) {
        return new EmSegment(0, EmObservationKind.OBSERVED, new EmContextKey(process, host),
                null, null, at(start), at(end));
    }

    private static EmSegment boundary(EmObservationKind kind, int start, int end) {
        return new EmSegment(0, kind, null, null, null, at(start), at(end));
    }

    @Test
    void savesAndReadsBySemiOpenOverlap() {
        EmSQLiteSegmentRepository repository = new EmSQLiteSegmentRepository(database());
        repository.saveAll(List.of(
                observed("notepad.exe", null, 0, 10),
                observed("chrome.exe", "example.com", 20, 30),
                observed("notepad.exe", null, 5, 25)));

        List<EmSegment> overlapping = repository.between(at(8), at(22));
        assertEquals(3, overlapping.size());
        assertEquals(at(0), overlapping.get(0).start());
        assertEquals(at(5), overlapping.get(1).start());
        assertEquals(at(20), overlapping.get(2).start());
        assertEquals("example.com", overlapping.get(2).context().siteHost());
        assertTrue(overlapping.stream().allMatch(segment -> segment.id() > 0));

        List<EmSegment> boundaryStart = repository.between(at(10), at(20));
        assertEquals(1, boundaryStart.size());
        assertEquals(at(5), boundaryStart.getFirst().start());

        assertTrue(repository.between(at(30), at(40)).isEmpty());
    }

    @Test
    void roundTripsNonObservedSegmentsWithoutIdentity() throws Exception {
        EmSQLiteSegmentRepository repository = new EmSQLiteSegmentRepository(database());
        repository.saveAll(List.of(
                boundary(EmObservationKind.IDLE, 0, 5),
                boundary(EmObservationKind.EXCLUDED, 5, 10),
                boundary(EmObservationKind.UNAVAILABLE, 10, 15)));

        List<EmSegment> stored = repository.between(at(0), at(15));
        assertEquals(List.of(EmObservationKind.IDLE, EmObservationKind.EXCLUDED, EmObservationKind.UNAVAILABLE),
                stored.stream().map(EmSegment::kind).toList());
        assertTrue(stored.stream().allMatch(segment -> segment.context() == null));
        assertTrue(stored.stream().allMatch(segment -> segment.siteLabel() == null));
        assertTrue(stored.stream().allMatch(segment -> segment.siteSource() == null));
        assertTrue(stored.stream().allMatch(segment -> segment.id() > 0));

        List<String> sentinels = new ArrayList<>();
        try (Connection connection = EmSQLiteSchema.open(database());
             Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery("SELECT process_name FROM segment ORDER BY start_ts")) {
            while (result.next()) sentinels.add(result.getString(1));
        }
        assertEquals(List.of("__IDLE__", "__EXCLUDED__", "__UNAVAILABLE__"), sentinels);
    }

    @Test
    void listsDistinctObservedContextsWithoutBoundaryIdentities() {
        EmSQLiteSegmentRepository repository = new EmSQLiteSegmentRepository(database());
        repository.saveAll(List.of(
                observed("notepad.exe", null, 0, 10),
                observed("notepad.exe", null, 10, 20),
                observed("chrome.exe", "example.com", 20, 30),
                boundary(EmObservationKind.IDLE, 30, 35)));

        assertEquals(List.of(
                        new EmContextKey("chrome.exe", "example.com"),
                        new EmContextKey("notepad.exe", null)),
                repository.observedContexts());
    }

    @Test
    void rollsBackWholeBatchOnFailure() throws Exception {
        EmSQLiteSegmentRepository repository = new EmSQLiteSegmentRepository(database());
        try (Connection connection = EmSQLiteSchema.open(database());
             Statement statement = connection.createStatement()) {
            statement.execute("""
                    CREATE TRIGGER fail_bad BEFORE INSERT ON segment
                    WHEN NEW.process_name = 'fail.exe'
                    BEGIN SELECT RAISE(ABORT, 'boom'); END
                    """);
        }

        assertThrows(EmStorageException.class, () -> repository.saveAll(List.of(
                observed("ok.exe", null, 0, 1),
                observed("fail.exe", null, 1, 2))));
        assertTrue(repository.between(at(0), at(10)).isEmpty());
    }

    @Test
    void reopensExistingDatabase() {
        Path database = database();
        EmSQLiteSegmentRepository first = new EmSQLiteSegmentRepository(database);
        first.saveAll(List.of(observed("notepad.exe", null, 0, 4)));

        EmSQLiteSegmentRepository reopened = new EmSQLiteSegmentRepository(database);
        List<EmSegment> stored = reopened.between(at(0), at(4));
        assertEquals(1, stored.size());
        assertEquals("notepad.exe", stored.getFirst().context().processName());
    }

    @Test
    void configuresWalForeignKeysAndSchemaVersion() throws Exception {
        new EmSQLiteSegmentRepository(database());
        try (Connection connection = EmSQLiteSchema.open(database());
             Statement statement = connection.createStatement()) {
            try (ResultSet result = statement.executeQuery("PRAGMA journal_mode")) {
                assertTrue(result.next());
                assertEquals("wal", result.getString(1));
            }
            try (ResultSet result = statement.executeQuery("PRAGMA foreign_keys")) {
                assertTrue(result.next());
                assertEquals(1, result.getInt(1));
            }
            try (ResultSet result = statement.executeQuery("SELECT version FROM schema_version")) {
                assertTrue(result.next());
                assertEquals(EmSQLiteSchema.VERSION, result.getInt(1));
            }
        }
    }
}
