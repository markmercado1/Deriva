package com.deriva.integration;

import com.deriva.app.usecase.EmDailyUsage;
import com.deriva.app.usecase.EmDailyUsageService;
import com.deriva.privacy.EmObservation;
import com.deriva.privacy.EmObservationKind;
import com.deriva.privacy.EmPrivacyFilter;
import com.deriva.sensor.EmActivitySample;
import com.deriva.sensor.EmFakeIdleDetector;
import com.deriva.sensor.EmFakeSensor;
import com.deriva.storage.sqlite.EmSQLiteSegmentRepository;
import com.deriva.tracking.EmSegmenter;
import com.deriva.tracking.model.EmSegment;
import com.deriva.tracking.model.EmTickResult;
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

class EmPipelinePersistenceIntegrationTest {
    private static final Instant BASE = Instant.parse("2026-11-22T10:00:00Z");

    @TempDir Path tempDirectory;

    private static Instant at(int second) {
        return BASE.plusSeconds(second);
    }

    private static EmActivitySample sample(String process, long pid, int second) {
        return new EmActivitySample(at(second), process, pid, 200 + second, false, true);
    }

    @Test
    void fakeSensorPrivacySegmenterAndRepositoryWorkTogether() {
        EmFakeSensor sensor = new EmFakeSensor(
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
                new EmActivitySample(at(10), null, 0, 0, false, false));
        EmFakeIdleDetector idle = new EmFakeIdleDetector(
                Duration.ZERO, Duration.ZERO, Duration.ZERO, Duration.ZERO,
                Duration.ofSeconds(121), Duration.ofSeconds(121),
                Duration.ZERO, Duration.ZERO, Duration.ZERO, Duration.ZERO, Duration.ZERO);
        EmPrivacyFilter filter = new EmPrivacyFilter(999_999);
        EmSegmenter segmenter = new EmSegmenter();

        List<EmSegment> completed = new ArrayList<>();
        while (sensor.remaining() > 0) {
            EmObservation observation = filter.filter(sensor.sample(), idle.idleTime());
            EmTickResult result = segmenter.onObservation(observation);
            completed.addAll(result.completedSegments());
        }

        assertEquals(3, completed.size());
        assertEquals(List.of(EmObservationKind.OBSERVED, EmObservationKind.IDLE, EmObservationKind.OBSERVED),
                completed.stream().map(EmSegment::kind).toList());

        EmSQLiteSegmentRepository repository = new EmSQLiteSegmentRepository(tempDirectory.resolve("integration.db"));
        repository.saveAll(completed);

        List<EmSegment> stored = repository.between(at(0), at(10));
        assertEquals(3, stored.size());
        assertEquals(List.of(EmObservationKind.OBSERVED, EmObservationKind.IDLE, EmObservationKind.OBSERVED),
                stored.stream().map(EmSegment::kind).toList());
        assertEquals("notepad.exe", stored.get(0).context().processName());
        assertNull(stored.get(1).context());
        assertEquals(at(0), stored.get(0).start());
        assertEquals(at(4), stored.get(0).end());
        assertEquals(at(4), stored.get(1).start());
        assertEquals(at(6), stored.get(1).end());
        assertEquals(at(6), stored.get(2).start());
        assertEquals(at(10), stored.get(2).end());
        assertTrue(stored.stream().allMatch(segment -> segment.id() > 0));

        EmSQLiteSegmentRepository reopened = new EmSQLiteSegmentRepository(tempDirectory.resolve("integration.db"));
        assertEquals(stored, reopened.between(at(0), at(10)));
    }

    @Test
    void closeFlushesPendingCandidateAndReopenKeepsBothWithoutOverlap() {
        EmSegmenter segmenter = new EmSegmenter();
        EmActivitySample[] first = {
                sample("notepad.exe", 100, 0), sample("notepad.exe", 101, 1),
                sample("notepad.exe", 102, 2), sample("notepad.exe", 103, 3)
        };
        EmFakeIdleDetector idle = new EmFakeIdleDetector(
                Duration.ZERO, Duration.ZERO, Duration.ZERO, Duration.ZERO, Duration.ZERO);
        EmPrivacyFilter filter = new EmPrivacyFilter(999_999);
        for (EmActivitySample activity : first)
            segmenter.onObservation(filter.filter(activity, idle.idleTime()));

        segmenter.onObservation(filter.filter(sample("browser.exe", 200, 4), idle.idleTime()));
        segmenter.onObservation(filter.filter(sample("browser.exe", 201, 5), idle.idleTime()));
        List<EmSegment> completed = segmenter.close(at(6));
        assertEquals(2, completed.size());
        assertEquals(at(0), completed.get(0).start());
        assertEquals(at(4), completed.get(0).end());
        assertEquals("notepad.exe", completed.get(0).context().processName());
        assertEquals(at(4), completed.get(1).start());
        assertEquals(at(6), completed.get(1).end());
        assertEquals("browser.exe", completed.get(1).context().processName());

        Path database = tempDirectory.resolve("pending-close.db");
        EmSQLiteSegmentRepository repository = new EmSQLiteSegmentRepository(database);
        repository.saveAll(completed);
        List<EmSegment> reopened = new EmSQLiteSegmentRepository(database).between(at(0), at(6));
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
        EmSegmenter segmenter = new EmSegmenter();
        EmPrivacyFilter filter = new EmPrivacyFilter(999_999);
        EmFakeIdleDetector idle = new EmFakeIdleDetector(Duration.ZERO);
        List<EmSegment> completed = new ArrayList<>();
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
        EmSQLiteSegmentRepository repository = new EmSQLiteSegmentRepository(database);
        repository.saveAll(completed);

        EmDailyUsage usage = new EmDailyUsageService(repository,
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
