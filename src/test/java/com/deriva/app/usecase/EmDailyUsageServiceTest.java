package com.deriva.app.usecase;

import com.deriva.privacy.EmObservationKind;
import com.deriva.storage.port.EmSegmentRepository;
import com.deriva.tracking.model.EmContextKey;
import com.deriva.tracking.model.EmSegment;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class EmDailyUsageServiceTest {
    private static final ZoneId ZONE = ZoneOffset.UTC;
    private static final LocalDate DAY = LocalDate.of(2026, 11, 22);
    private static final Instant DAY_START = Instant.parse("2026-11-22T00:00:00Z");
    private static final Instant NOON = Instant.parse("2026-11-22T12:00:00Z");
    private static final EmContextKey CHROME = new EmContextKey("chrome.exe", null);
    private static final EmContextKey VSCODE = new EmContextKey("code.exe", null);

    @Test
    void clipsCrossMidnightAndKeepsSitesInsideTheirBrowser() {
        FakeRepository repository = new FakeRepository(List.of(
                observed("chrome.exe", "youtube.com", "2026-11-21T23:50:00Z", "2026-11-22T00:20:00Z"),
                observed("chrome.exe", "instagram.com", "2026-11-22T08:00:00Z", "2026-11-22T08:30:00Z"),
                new EmSegment(0, EmObservationKind.OBSERVED, CHROME, null, null,
                        instant("2026-11-22T08:30:00Z"), instant("2026-11-22T08:40:00Z")),
                new EmSegment(0, EmObservationKind.OBSERVED, VSCODE, null, null,
                        instant("2026-11-22T08:40:00Z"), instant("2026-11-22T09:10:00Z")),
                new EmSegment(0, EmObservationKind.IDLE, null, null, null,
                        instant("2026-11-22T09:10:00Z"), instant("2026-11-22T09:20:00Z")),
                new EmSegment(0, EmObservationKind.EXCLUDED, null, null, null,
                        instant("2026-11-22T09:20:00Z"), instant("2026-11-22T09:30:00Z")),
                new EmSegment(0, EmObservationKind.UNAVAILABLE, null, null, null,
                        instant("2026-11-22T09:30:00Z"), instant("2026-11-22T09:40:00Z"))));

        EmDailyUsage usage = new EmDailyUsageService(repository, Clock.fixed(NOON, ZONE))
                .forDay(DAY, ZONE);

        assertEquals(Duration.ofMinutes(90).toMillis(), usage.observedMillis());
        assertEquals(Duration.ofMinutes(60).toMillis(), usage.appMillis().get("chrome.exe"));
        assertEquals(Duration.ofMinutes(30).toMillis(), usage.appMillis().get("code.exe"));
        assertEquals(Duration.ofMinutes(20).toMillis(), usage.siteMillis().get("youtube.com"));
        assertEquals(Duration.ofMinutes(30).toMillis(), usage.siteMillis().get("instagram.com"));
        assertEquals(usage.observedMillis(), usage.appMillis().values().stream()
                .mapToLong(Long::longValue).sum());
        long siteTotal = usage.siteMillis().values().stream().mapToLong(Long::longValue).sum();
        assertTrue(siteTotal <= usage.appMillis().get("chrome.exe"));
        assertEquals(Duration.ofMinutes(10).toMillis(), usage.idleMillis());
        assertEquals(Duration.ofMinutes(10).toMillis(), usage.excludedMillis());
        assertEquals(Duration.ofMinutes(10).toMillis(), usage.unavailableMillis());
        assertEquals(Duration.ofMinutes(120).toMillis(), usage.recordedMillis());
        assertEquals(Duration.ofHours(12).toMillis(), usage.elapsedMillis());
        assertEquals(Duration.ofMinutes(600).toMillis(), usage.unrecordedMillis());
        assertTrue(usage.hasSegments());
        assertEquals(DAY_START, repository.from);
        assertEquals(Instant.parse("2026-11-23T00:00:00Z"), repository.to);
    }

    @Test
    void addsOpenButNotPendingAndSkipsOverlappingPersistedSegment() {
        EmSegment persisted = observed("chrome.exe", "youtube.com",
                "2026-11-22T08:00:00Z", "2026-11-22T08:30:00Z");
        EmSegment open = observed("chrome.exe", "youtube.com",
                "2026-11-22T08:30:00Z", "2026-11-22T09:00:00Z");
        EmSegment pending = observed("code.exe", null,
                "2026-11-22T09:00:00Z", "2026-11-22T09:05:00Z");
        EmCaptureSnapshot snapshot = new EmCaptureSnapshot(
                instant("2026-11-22T09:05:00Z"), open, pending, true);

        EmDailyUsage usage = new EmDailyUsageService(new FakeRepository(List.of(persisted)),
                Clock.fixed(NOON, ZONE)).forDay(DAY, ZONE, snapshot);

        assertEquals(Duration.ofMinutes(60).toMillis(), usage.observedMillis());
        assertEquals(Duration.ofMinutes(60).toMillis(), usage.appMillis().get("chrome.exe"));
        assertEquals(Duration.ofMinutes(60).toMillis(), usage.siteMillis().get("youtube.com"));
        assertFalse(usage.appMillis().containsKey("code.exe"));
        assertTrue(usage.snapshot().captureRunning());

        EmDailyUsage duplicate = new EmDailyUsageService(new FakeRepository(List.of(persisted)),
                Clock.fixed(NOON, ZONE)).forDay(DAY, ZONE, new EmCaptureSnapshot(
                        instant("2026-11-22T08:30:00Z"), persisted, null, true));
        assertEquals(Duration.ofMinutes(30).toMillis(), duplicate.observedMillis());
    }

    @Test
    void pastDayUsesFullDayAsElapsed() {
        EmDailyUsage usage = new EmDailyUsageService(new FakeRepository(List.of()),
                Clock.fixed(Instant.parse("2026-11-23T12:00:00Z"), ZONE)).forDay(DAY, ZONE);

        assertEquals(Duration.ofHours(24).toMillis(), usage.elapsedMillis());
        assertFalse(usage.hasSegments());
        assertEquals(0, usage.observedMillis());
    }

    @Test
    void rangeClipsBothEdgesAndIncludesLiveWithoutDuplicateSites() {
        EmSegment first = observed("chrome.exe", "example.com", "2026-11-20T23:00:00Z", "2026-11-21T01:00:00Z");
        EmSegment open = observed("chrome.exe", "example.com", "2026-11-22T11:00:00Z", "2026-11-22T12:00:00Z");
        FakeRepository repo = new FakeRepository(List.of(first));
        var usage = new EmDailyUsageService(repo, Clock.fixed(NOON, ZONE)).forRange(
                DAY.minusDays(1), DAY.plusDays(3), ZONE, new EmCaptureSnapshot(NOON, open, null, true));
        assertEquals(Duration.ofHours(2).toMillis(), usage.observedMillis());
        assertEquals(usage.observedMillis(), usage.siteMillis().get("example.com"));
        assertEquals(Duration.ofHours(36).toMillis(), usage.elapsedMillis());
        assertEquals(instant("2026-11-21T00:00:00Z"), repo.from);
        assertEquals(instant("2026-11-26T00:00:00Z"), repo.to);
        assertTrue(usage.snapshot().captureRunning());
    }

    @Test
    void rangeHandlesDstFutureAndInvalidOrder() {
        var service = new EmDailyUsageService(new FakeRepository(List.of()), Clock.fixed(NOON, ZONE));
        var dst = service.forRange(LocalDate.of(2026, 3, 7), LocalDate.of(2026, 3, 9),
                ZoneId.of("America/New_York"), EmCaptureSnapshot.stopped());
        assertEquals(Duration.ofHours(71).toMillis(), dst.elapsedMillis());
        assertEquals(0, service.forRange(DAY.plusDays(1), DAY.plusYears(1), ZONE,
                EmCaptureSnapshot.stopped()).elapsedMillis());
        assertThrows(IllegalArgumentException.class, () -> service.forRange(DAY, DAY.minusDays(1), ZONE,
                EmCaptureSnapshot.stopped()));
    }

    @Test
    void presetsRespectLeapYearAndWeekAcrossYears() {
        var leap = LocalDate.of(2024, 2, 15);
        assertEquals(LocalDate.of(2024, 2, 29), com.deriva.ui.viewmodel.EmUsagePeriod.MONTH.end(leap));
        assertEquals(LocalDate.of(2024, 12, 31), com.deriva.ui.viewmodel.EmUsagePeriod.YEAR.end(leap));
        assertEquals(LocalDate.of(2025, 12, 29), com.deriva.ui.viewmodel.EmUsagePeriod.WEEK.start(LocalDate.of(2026, 1, 1)));
        assertEquals(LocalDate.of(2026, 1, 4), com.deriva.ui.viewmodel.EmUsagePeriod.WEEK.end(LocalDate.of(2026, 1, 1)));
    }

    private static EmSegment observed(String process, String host, String start, String end) {
        return new EmSegment(0, EmObservationKind.OBSERVED, new EmContextKey(process, host),
                null, null, instant(start), instant(end));
    }

    private static Instant instant(String value) {
        return Instant.parse(value);
    }

    private static final class FakeRepository implements EmSegmentRepository {
        private final List<EmSegment> segments;
        private Instant from;
        private Instant to;

        private FakeRepository(List<EmSegment> segments) {
            this.segments = segments;
        }

        @Override
        public void saveAll(List<EmSegment> segments) {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<EmSegment> between(Instant from, Instant to) {
            this.from = from;
            this.to = to;
            return segments;
        }

        @Override
        public java.util.List<com.deriva.tracking.model.EmContextKey> observedContexts() {
            return java.util.List.of();
        }
    }
}
