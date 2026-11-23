package com.deriva.integration;

import com.deriva.bridge.BrowserReport;
import com.deriva.bridge.BrowserStateStore;
import com.deriva.privacy.Observation;
import com.deriva.privacy.ObservationKind;
import com.deriva.privacy.PrivacyFilter;
import com.deriva.sensor.ActivitySample;
import com.deriva.storage.sqlite.SQLitePrivacyCheck;
import com.deriva.storage.sqlite.SQLiteSegmentRepository;
import com.deriva.tracking.Segmenter;
import com.deriva.tracking.model.Segment;
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

class BrowserPrivacyIntegrationTest {
    private static final Instant T = Instant.parse("2026-11-22T10:00:00Z");

    @TempDir Path tempDirectory;

    @Test
    void chromeHostRequiresFreshNormalBridgeState() {
        AtomicLong now = new AtomicLong();
        BrowserStateStore store = new BrowserStateStore(
                Clock.fixed(T, ZoneOffset.UTC), now::get);
        String session = store.beginSession();
        assertTrue(store.accept(session, new BrowserReport(BrowserReport.State.NORMAL, "chess.com", 1)));

        PrivacyFilter filter = new PrivacyFilter(999, Duration.ofSeconds(120), store::freshNormalHost);
        ActivitySample chrome = new ActivitySample(T, "chrome.exe", 123, 456, false, true);

        Observation allowed = filter.filter(chrome, Duration.ZERO);
        assertEquals(ObservationKind.OBSERVED, allowed.kind());
        assertEquals("chess.com", allowed.siteHost());

        now.set(3_000_000_000L);
        Observation stale = filter.filter(chrome, Duration.ZERO);
        assertEquals(ObservationKind.EXCLUDED, stale.kind());
        assertNull(stale.siteHost());

        now.set(0);
        assertTrue(store.accept(session, new BrowserReport(BrowserReport.State.UNKNOWN, null, 2)));
        Observation unknown = filter.filter(chrome, Duration.ZERO);
        assertEquals(ObservationKind.EXCLUDED, unknown.kind());
        assertNull(unknown.siteHost());
    }

    @Test
    void persistedPrivacyMatrixContainsNoSensitiveMarkers() throws Exception {
        AtomicLong now = new AtomicLong();
        BrowserStateStore store = new BrowserStateStore(Clock.fixed(T, ZoneOffset.UTC), now::get);
        String session = store.beginSession();
        PrivacyFilter filter = new PrivacyFilter(999, Duration.ofSeconds(120), store::freshNormalHost);
        Segmenter segmenter = new Segmenter();
        List<Segment> completed = new ArrayList<>();

        assertTrue(store.accept(session, new BrowserReport(BrowserReport.State.NORMAL, "normal.example", 1)));
        for (int second = 0; second <= 3; second++)
            observe(segmenter, completed, filter.filter(chrome(second), Duration.ZERO));

        assertTrue(store.accept(session, new BrowserReport(BrowserReport.State.UNKNOWN, null, 2)));
        for (int second = 4; second <= 7; second++)
            observe(segmenter, completed, filter.filter(chrome(second), Duration.ZERO));

        assertTrue(store.accept(session, new BrowserReport(
                BrowserReport.State.NORMAL, "private-marker.example", 3)));
        now.set(3_000_000_000L);
        observe(segmenter, completed, filter.filter(chrome(8), Duration.ZERO));

        assertTrue(store.accept(session, new BrowserReport(BrowserReport.State.UNKNOWN, null, 4)));
        now.set(0);
        assertTrue(store.accept(session, new BrowserReport(BrowserReport.State.NORMAL, "normal.example", 5)));
        observe(segmenter, completed, filter.filter(
                new ActivitySample(T.plusSeconds(10), "firefox.exe", 321, 900, false, true), Duration.ZERO));
        observe(segmenter, completed, filter.filter(
                new ActivitySample(T.plusSeconds(13), "own.exe", 999, 901, false, true), Duration.ZERO));

        for (int second = 14; second <= 19; second++)
            observe(segmenter, completed, filter.filter(chrome(second), Duration.ofSeconds(121)));
        for (int second = 20; second <= 23; second++)
            observe(segmenter, completed, filter.filter(
                    new ActivitySample(T.plusSeconds(second), null, 0, 0, false, false), Duration.ZERO));
        completed.addAll(segmenter.close(T.plusSeconds(24)));

        Path database = tempDirectory.resolve("matrix.db");
        SQLiteSegmentRepository repository = new SQLiteSegmentRepository(database);
        repository.saveAll(completed);
        assertDoesNotThrow(() -> SQLitePrivacyCheck.verify(database));

        List<Segment> stored = repository.between(T, T.plusSeconds(24));
        assertEquals(4, stored.size());
        assertEquals(List.of(ObservationKind.OBSERVED, ObservationKind.EXCLUDED,
                ObservationKind.IDLE, ObservationKind.UNAVAILABLE),
                stored.stream().map(Segment::kind).toList());
        assertEquals(1, stored.stream()
                .filter(segment -> segment.kind() == ObservationKind.OBSERVED).count());
        assertEquals("normal.example",
                stored.getFirst().context().siteHost());
        for (Segment segment : stored)
            if (segment.kind() != ObservationKind.OBSERVED) assertNull(segment.context());

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
        BrowserStateStore store = new BrowserStateStore(Clock.fixed(T, ZoneOffset.UTC), now::get);
        String session = store.beginSession();
        assertTrue(store.accept(session, new BrowserReport(BrowserReport.State.UNKNOWN, null, 1)));

        PrivacyFilter filter = new PrivacyFilter(999, Duration.ofSeconds(120), store::freshNormalHost);
        Segmenter segmenter = new Segmenter();
        ActivitySample first = new ActivitySample(T, "chrome.exe", 123, 456, false, true);
        ActivitySample second = new ActivitySample(T.plusSeconds(1), "chrome.exe", 123, 456, false, true);

        segmenter.onObservation(filter.filter(first, Duration.ZERO));
        segmenter.onObservation(filter.filter(second, Duration.ZERO));
        List<Segment> completed = segmenter.close(T.plusSeconds(2));

        Path database = tempDirectory.resolve("private.db");
        SQLiteSegmentRepository repository = new SQLiteSegmentRepository(database);
        repository.saveAll(completed);
        assertDoesNotThrow(() -> SQLitePrivacyCheck.verify(database));

        List<Segment> stored = repository.between(T, T.plusSeconds(2));
        assertEquals(1, stored.size());
        assertEquals(ObservationKind.EXCLUDED, stored.getFirst().kind());
        assertNull(stored.getFirst().context());

        try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + database);
             Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery("SELECT COUNT(*) FROM segment WHERE site_host IS NOT NULL")) {
            assertTrue(result.next());
            assertEquals(0, result.getInt(1));
        }
    }

    private static void observe(Segmenter segmenter, List<Segment> completed, Observation observation) {
        completed.addAll(segmenter.onObservation(observation).completedSegments());
    }

    private static ActivitySample chrome(int second) {
        return new ActivitySample(T.plusSeconds(second), "chrome.exe", 123, 456, false, true);
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
