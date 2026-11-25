package com.deriva.integration;

import com.deriva.bridge.EmBrowserReport;
import com.deriva.bridge.EmBrowserStateStore;
import com.deriva.privacy.EmObservation;
import com.deriva.privacy.EmObservationKind;
import com.deriva.privacy.EmPrivacyFilter;
import com.deriva.sensor.EmActivitySample;
import com.deriva.storage.sqlite.EmSQLitePrivacyCheck;
import com.deriva.storage.sqlite.EmSQLiteSegmentRepository;
import com.deriva.tracking.EmSegmenter;
import com.deriva.tracking.model.EmSegment;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;

class EmBrowserPrivacyIntegrationTest {
    private static final Instant T = Instant.parse("2026-11-22T10:00:00Z");

    @TempDir Path tempDirectory;

    @Test
    void chromeHostRequiresFreshNormalBridgeState() {
        AtomicLong now = new AtomicLong();
        EmBrowserStateStore store = new EmBrowserStateStore(
                Clock.fixed(T, ZoneOffset.UTC), now::get);
        String session = store.beginSession();
        assertTrue(store.accept(session, new EmBrowserReport(EmBrowserReport.State.NORMAL, "chess.com", 1)));

        EmPrivacyFilter filter = new EmPrivacyFilter(999, Duration.ofSeconds(120), store::freshNormalHost);
        EmActivitySample chrome = new EmActivitySample(T, "chrome.exe", 123, 456, false, true);

        EmObservation allowed = filter.filter(chrome, Duration.ZERO);
        assertEquals(EmObservationKind.OBSERVED, allowed.kind());
        assertEquals("chess.com", allowed.siteHost());

        now.set(3_000_000_000L);
        EmObservation stale = filter.filter(chrome, Duration.ZERO);
        assertEquals(EmObservationKind.EXCLUDED, stale.kind());
        assertNull(stale.siteHost());

        now.set(0);
        assertTrue(store.accept(session, new EmBrowserReport(EmBrowserReport.State.UNKNOWN, null, 2)));
        EmObservation unknown = filter.filter(chrome, Duration.ZERO);
        assertEquals(EmObservationKind.EXCLUDED, unknown.kind());
        assertNull(unknown.siteHost());
    }

    @Test
    void persistedPrivacyMatrixContainsNoSensitiveMarkers() throws Exception {
        AtomicLong now = new AtomicLong();
        EmBrowserStateStore store = new EmBrowserStateStore(Clock.fixed(T, ZoneOffset.UTC), now::get);
        String session = store.beginSession();
        EmPrivacyFilter filter = new EmPrivacyFilter(999, Duration.ofSeconds(120), store::freshNormalHost);
        EmSegmenter segmenter = new EmSegmenter();
        List<EmSegment> completed = new ArrayList<>();

        assertTrue(store.accept(session, new EmBrowserReport(EmBrowserReport.State.NORMAL, "normal.example", 1)));
        for (int second = 0; second <= 3; second++)
            observe(segmenter, completed, filter.filter(chrome(second), Duration.ZERO));

        assertTrue(store.accept(session, new EmBrowserReport(EmBrowserReport.State.UNKNOWN, null, 2)));
        for (int second = 4; second <= 7; second++)
            observe(segmenter, completed, filter.filter(chrome(second), Duration.ZERO));

        assertTrue(store.accept(session, new EmBrowserReport(
                EmBrowserReport.State.NORMAL, "private-marker.example", 3)));
        now.set(3_000_000_000L);
        observe(segmenter, completed, filter.filter(chrome(8), Duration.ZERO));

        assertTrue(store.accept(session, new EmBrowserReport(EmBrowserReport.State.UNKNOWN, null, 4)));
        now.set(0);
        assertTrue(store.accept(session, new EmBrowserReport(EmBrowserReport.State.NORMAL, "normal.example", 5)));
        observe(segmenter, completed, filter.filter(
                new EmActivitySample(T.plusSeconds(10), "firefox.exe", 321, 900, false, true), Duration.ZERO));
        observe(segmenter, completed, filter.filter(
                new EmActivitySample(T.plusSeconds(13), "own.exe", 999, 901, false, true), Duration.ZERO));

        for (int second = 14; second <= 19; second++)
            observe(segmenter, completed, filter.filter(chrome(second), Duration.ofSeconds(121)));
        for (int second = 20; second <= 23; second++)
            observe(segmenter, completed, filter.filter(
                    new EmActivitySample(T.plusSeconds(second), null, 0, 0, false, false), Duration.ZERO));
        completed.addAll(segmenter.close(T.plusSeconds(24)));

        Path database = tempDirectory.resolve("matrix.db");
        EmSQLiteSegmentRepository repository = new EmSQLiteSegmentRepository(database);
        repository.saveAll(completed);
        assertDoesNotThrow(() -> EmSQLitePrivacyCheck.verify(database));

        List<EmSegment> stored = repository.between(T, T.plusSeconds(24));
        assertEquals(4, stored.size());
        assertEquals(List.of(EmObservationKind.OBSERVED, EmObservationKind.EXCLUDED,
                EmObservationKind.IDLE, EmObservationKind.UNAVAILABLE),
                stored.stream().map(EmSegment::kind).toList());
        assertEquals(1, stored.stream()
                .filter(segment -> segment.kind() == EmObservationKind.OBSERVED).count());
        assertEquals("normal.example",
                stored.getFirst().context().siteHost());
        for (EmSegment segment : stored)
            if (segment.kind() != EmObservationKind.OBSERVED) assertNull(segment.context());

        String persisted = databaseBytes(database);
        assertTrue(persisted.contains("normal.example"));
        assertFalse(persisted.contains("private-marker.example"));
        assertFalse(persisted.contains("https://"));
        assertFalse(persisted.contains("title="));
        assertFalse(persisted.contains("0123456789abcdef".repeat(4)));
    }

    @Test
    void unknownBrowserStateStoresNoHost() throws Exception {
        AtomicLong now = new AtomicLong();
        EmBrowserStateStore store = new EmBrowserStateStore(Clock.fixed(T, ZoneOffset.UTC), now::get);
        String session = store.beginSession();
        assertTrue(store.accept(session, new EmBrowserReport(EmBrowserReport.State.UNKNOWN, null, 1)));

        EmPrivacyFilter filter = new EmPrivacyFilter(999, Duration.ofSeconds(120), store::freshNormalHost);
        EmSegmenter segmenter = new EmSegmenter();
        EmActivitySample first = new EmActivitySample(T, "chrome.exe", 123, 456, false, true);
        EmActivitySample second = new EmActivitySample(T.plusSeconds(1), "chrome.exe", 123, 456, false, true);

        segmenter.onObservation(filter.filter(first, Duration.ZERO));
        segmenter.onObservation(filter.filter(second, Duration.ZERO));
        List<EmSegment> completed = segmenter.close(T.plusSeconds(2));

        Path database = tempDirectory.resolve("private.db");
        EmSQLiteSegmentRepository repository = new EmSQLiteSegmentRepository(database);
        repository.saveAll(completed);
        assertDoesNotThrow(() -> EmSQLitePrivacyCheck.verify(database));

        List<EmSegment> stored = repository.between(T, T.plusSeconds(2));
        assertEquals(1, stored.size());
        assertEquals(EmObservationKind.EXCLUDED, stored.getFirst().kind());
        assertNull(stored.getFirst().context());

        try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + database);
             Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery("SELECT COUNT(*) FROM segment WHERE site_host IS NOT NULL")) {
            assertTrue(result.next());
            assertEquals(0, result.getInt(1));
        }
    }

    private static void observe(EmSegmenter segmenter, List<EmSegment> completed, EmObservation observation) {
        completed.addAll(segmenter.onObservation(observation).completedSegments());
    }

    private static EmActivitySample chrome(int second) {
        return new EmActivitySample(T.plusSeconds(second), "chrome.exe", 123, 456, false, true);
    }

    private static String databaseBytes(Path database) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        for (String suffix : List.of("", "-wal", "-shm")) {
            Path file = Path.of(database + suffix);
            if (Files.exists(file)) bytes.write(Files.readAllBytes(file));
        }
        return bytes.toString(StandardCharsets.ISO_8859_1);
    }
}
