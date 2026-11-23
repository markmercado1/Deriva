package com.deriva.integration;

import com.deriva.app.usecase.DailyUsage;
import com.deriva.app.usecase.DailyUsageService;
import com.deriva.privacy.Observation;
import com.deriva.privacy.ObservationKind;
import com.deriva.privacy.PrivacyFilter;
import com.deriva.sensor.ActivitySample;
import com.deriva.sensor.FakeIdleDetector;
import com.deriva.sensor.FakeSensor;
import com.deriva.storage.sqlite.SQLiteSegmentRepository;
import com.deriva.tracking.Segmenter;
import com.deriva.tracking.model.Segment;
import com.deriva.tracking.model.TickResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PipelinePersistenceIntegrationTest {
    private static final Instant BASE = Instant.parse("2026-11-22T10:00:00Z");

    @TempDir Path tempDirectory;

    private static Instant at(int second) {
        return BASE.plusSeconds(second);
    }

    private static ActivitySample sample(String process, long pid, int second) {
        return new ActivitySample(at(second), process, pid, 200 + second, false, true);
    }

    @Test
    void fakeSensorPrivacySegmenterAndRepositoryWorkTogether() {
        FakeSensor sensor = new FakeSensor(
                sample("notepad.exe", 100, 0),
                sample("notepad.exe", 101, 1),
                sample("notepad.exe", 102, 2),
                sample("notepad.exe", 103, 3),
                sample("notepad.exe", 104, 4),
                sample("notepad.exe", 105, 5),
                sample("notepad.exe", 106, 6),
                sample("notepad.exe", 107, 7),
                sample("notepad.exe", 108, 8),
                sample("notepad.exe", 109, 9),
                new ActivitySample(at(10), null, 0, 0, false, false));
        FakeIdleDetector idle = new FakeIdleDetector(
                Duration.ZERO, Duration.ZERO, Duration.ZERO, Duration.ZERO,
                Duration.ofSeconds(121), Duration.ofSeconds(121),
                Duration.ZERO, Duration.ZERO, Duration.ZERO, Duration.ZERO, Duration.ZERO);
        PrivacyFilter filter = new PrivacyFilter(999_999);
        Segmenter segmenter = new Segmenter();

        List<Segment> completed = new ArrayList<>();
        while (sensor.remaining() > 0) {
            Observation observation = filter.filter(sensor.sample(), idle.idleTime());
            TickResult result = segmenter.onObservation(observation);
            completed.addAll(result.completedSegments());
        }

        assertEquals(3, completed.size());
        assertEquals(List.of(ObservationKind.OBSERVED, ObservationKind.IDLE, ObservationKind.OBSERVED),
                completed.stream().map(Segment::kind).toList());

        SQLiteSegmentRepository repository = new SQLiteSegmentRepository(tempDirectory.resolve("integration.db"));
        repository.saveAll(completed);

        List<Segment> stored = repository.between(at(0), at(10));
        assertEquals(3, stored.size());
        assertEquals(List.of(ObservationKind.OBSERVED, ObservationKind.IDLE, ObservationKind.OBSERVED),
                stored.stream().map(Segment::kind).toList());
        assertEquals("notepad.exe", stored.get(0).context().processName());
        assertNull(stored.get(1).context());
        assertEquals(at(0), stored.get(0).start());
        assertEquals(at(4), stored.get(0).end());
        assertEquals(at(4), stored.get(1).start());
        assertEquals(at(6), stored.get(1).end());
        assertEquals(at(6), stored.get(2).start());
        assertEquals(at(10), stored.get(2).end());
        assertTrue(stored.stream().allMatch(segment -> segment.id() > 0));

        SQLiteSegmentRepository reopened = new SQLiteSegmentRepository(tempDirectory.resolve("integration.db"));
        assertEquals(stored, reopened.between(at(0), at(10)));
    }

    @Test
    void closeFlushesPendingCandidateAndReopenKeepsBothWithoutOverlap() {
        Segmenter segmenter = new Segmenter();
        ActivitySample[] first = {
                sample("notepad.exe", 100, 0), sample("notepad.exe", 101, 1),
                sample("notepad.exe", 102, 2), sample("notepad.exe", 103, 3)
        };
        FakeIdleDetector idle = new FakeIdleDetector(
                Duration.ZERO, Duration.ZERO, Duration.ZERO, Duration.ZERO, Duration.ZERO);
        PrivacyFilter filter = new PrivacyFilter(999_999);
        for (ActivitySample activity : first)
            segmenter.onObservation(filter.filter(activity, idle.idleTime()));

        segmenter.onObservation(filter.filter(sample("browser.exe", 200, 4), idle.idleTime()));
        segmenter.onObservation(filter.filter(sample("browser.exe", 201, 5), idle.idleTime()));
        List<Segment> completed = segmenter.close(at(6));
        assertEquals(2, completed.size());
        assertEquals(at(0), completed.get(0).start());
        assertEquals(at(4), completed.get(0).end());
        assertEquals("notepad.exe", completed.get(0).context().processName());
        assertEquals(at(4), completed.get(1).start());
        assertEquals(at(6), completed.get(1).end());
        assertEquals("browser.exe", completed.get(1).context().processName());

        Path database = tempDirectory.resolve("pending-close.db");
        SQLiteSegmentRepository repository = new SQLiteSegmentRepository(database);
        repository.saveAll(completed);
        List<Segment> reopened = new SQLiteSegmentRepository(database).between(at(0), at(6));
        assertEquals(2, reopened.size());
        for (int index = 0; index < completed.size(); index++) {
            assertEquals(completed.get(index).kind(), reopened.get(index).kind());
            assertEquals(completed.get(index).context(), reopened.get(index).context());
            assertEquals(completed.get(index).start(), reopened.get(index).start());
            assertEquals(completed.get(index).end(), reopened.get(index).end());
        }
        assertEquals(6_000, reopened.stream()
                .mapToLong(segment -> Duration.between(segment.start(), segment.end()).toMillis())
                .sum());
    }

    @Test
    void suspensionGapIsExcludedFromTotalsWithoutDuplicates() {
        Segmenter segmenter = new Segmenter();
        PrivacyFilter filter = new PrivacyFilter(999_999);
        FakeIdleDetector idle = new FakeIdleDetector(Duration.ZERO);
        List<Segment> completed = new ArrayList<>();
        for (int second = 0; second <= 3; second++)
            completed.addAll(segmenter.onObservation(
                    filter.filter(sample("notepad.exe", 100 + second, second), idle.idleTime()))
                    .completedSegments());

        for (int second = 10; second <= 13; second++)
            completed.addAll(segmenter.onObservation(
                    filter.filter(sample("notepad.exe", 200 + second, second), idle.idleTime()))
                    .completedSegments());
        completed.addAll(segmenter.close(at(13)));

        Path database = tempDirectory.resolve("suspension.db");
        SQLiteSegmentRepository repository = new SQLiteSegmentRepository(database);
        repository.saveAll(completed);

        DailyUsage usage = new DailyUsageService(repository,
                Clock.fixed(at(13), ZoneOffset.UTC))
                .forDay(LocalDate.of(2026, 11, 22), ZoneOffset.UTC);

        assertEquals(6_000, usage.observedMillis());
        assertEquals(6_000, usage.recordedMillis());
        // El día empieza a medianoche: 10 h previas al primer segmento + 7 s de hueco.
        assertEquals(Duration.ofHours(10).toMillis() + 7_000, usage.unrecordedMillis());
        assertEquals(6_000, usage.appMillis().get("notepad.exe"));
        assertEquals(2, repository.between(at(0), at(13)).size());
    }
}
