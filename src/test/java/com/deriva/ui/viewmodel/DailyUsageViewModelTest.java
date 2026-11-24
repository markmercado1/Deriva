package com.deriva.ui.viewmodel;

import com.deriva.app.usecase.CaptureControl;
import com.deriva.app.usecase.CaptureSnapshot;
import com.deriva.app.usecase.CaptureState;
import com.deriva.app.usecase.CaptureStatus;
import com.deriva.app.usecase.DailyUsageService;
import com.deriva.privacy.ObservationKind;
import com.deriva.storage.port.SegmentRepository;
import com.deriva.tracking.model.ContextKey;
import com.deriva.tracking.model.Segment;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DailyUsageViewModelTest {
    private static final LocalDate DAY = LocalDate.of(2026, 11, 22);
    private static final Instant NOON = Instant.parse("2026-11-22T12:00:00Z");
    private static final ZoneId ZONE = ZoneOffset.UTC;

    @Test
    void distinguishesNoDataFromZeroForeground() {
        DailyUsageViewModel empty = viewModel(List.of());
        UsagePage noData = empty.load(DAY);
        assertEquals("Sin datos", noData.totalText());
        assertFalse(noData.hasSegments());
        assertTrue(noData.statusText().contains("Sin datos"));
        assertEquals("Sin registro para esta fecha", noData.coverageText());
        assertTrue(noData.apps().isEmpty());
        assertTrue(noData.sites().isEmpty());

        UsagePage onlyIdle = viewModel(List.of(idle("2026-11-22T08:00:00Z", "2026-11-22T08:30:00Z")))
                .load(DAY);
        assertEquals("0 s", onlyIdle.totalText());
        assertTrue(onlyIdle.hasSegments());
        assertTrue(onlyIdle.statusText().contains("Sin uso en primer plano"));
        assertTrue(onlyIdle.apps().isEmpty());
        assertTrue(onlyIdle.sites().isEmpty());
    }

    @Test
    void formatsAndSortsAppsAndSites() {
        List<Segment> segments = List.of(
                observed("chrome.exe", "youtube.com", "2026-11-22T08:00:00Z", "2026-11-22T09:00:00Z"),
                observed("chrome.exe", "instagram.com", "2026-11-22T09:00:00Z", "2026-11-22T09:30:00Z"),
                observed("code.exe", null, "2026-11-22T09:30:00Z", "2026-11-22T10:00:00Z"),
                observed("chrome.exe", null, "2026-11-22T10:00:00Z", "2026-11-22T10:15:00Z"));

        UsagePage page = new DailyUsageViewModel(
                new DailyUsageService(new FakeRepository(segments), Clock.fixed(NOON, ZONE)), ZONE)
                .load(DAY);

        assertEquals("2 h 15 min", page.totalText());
        assertEquals("Zona horaria: Z", page.zoneText());
        assertEquals(List.of("Chrome", "VS Code"),
                page.apps().stream().map(UsageRow::name).toList());
        assertEquals(List.of("1 h 45 min", "30 min"),
                page.apps().stream().map(UsageRow::durationText).toList());
        assertEquals(1.0, page.apps().getFirst().ratio(), 0.0001);
        assertEquals(List.of("YouTube", "Instagram"),
                page.sites().stream().map(UsageRow::name).toList());
        assertEquals(List.of("1 h", "30 min"),
                page.sites().stream().map(UsageRow::durationText).toList());
        assertEquals(1.0, page.sites().getFirst().ratio(), 0.0001);
        assertTrue(page.statusText().contains("segmento abierto"));
    }

    @Test
    void liveSnapshotMarksOpenAndPendingWithoutCountingPending() {
        Segment persisted = observed("chrome.exe", "youtube.com",
                "2026-11-22T08:00:00Z", "2026-11-22T08:30:00Z");
        Segment open = observed("chrome.exe", "youtube.com",
                "2026-11-22T08:30:00Z", "2026-11-22T09:00:00Z");
        Segment pending = observed("code.exe", null,
                "2026-11-22T09:00:00Z", "2026-11-22T09:05:00Z");
        CaptureSnapshot snapshot = new CaptureSnapshot(
                Instant.parse("2026-11-22T09:05:00Z"), open, pending, true);
        DailyUsageViewModel viewModel = new DailyUsageViewModel(
                new DailyUsageService(new FakeRepository(List.of(persisted)),
                        Clock.fixed(NOON, ZONE)), ZONE, new FakeCapture(snapshot));

        UsagePage page = viewModel.load(DAY);

        assertEquals("1 h", page.totalText());
        assertEquals(List.of("Chrome"), page.apps().stream().map(UsageRow::name).toList());
        assertEquals(List.of("YouTube"), page.sites().stream().map(UsageRow::name).toList());
        assertTrue(page.statusText().contains("abierto: YouTube"));
        assertTrue(page.statusText().contains("pendiente: VS Code"));
        assertTrue(page.statusText().contains("no suma hasta confirmarse"));

        UsagePage yesterday = viewModel.load(DAY.minusDays(1));
        assertFalse(yesterday.statusText().contains("abierto:"));
    }

    private static DailyUsageViewModel viewModel(List<Segment> segments) {
        return new DailyUsageViewModel(new DailyUsageService(new FakeRepository(segments),
                Clock.fixed(NOON, ZONE)), ZONE);
    }

    private static Segment observed(String process, String host, String start, String end) {
        return new Segment(0, ObservationKind.OBSERVED, new ContextKey(process, host),
                null, null, Instant.parse(start), Instant.parse(end));
    }

    private static Segment idle(String start, String end) {
        return new Segment(0, ObservationKind.IDLE, null, null, null,
                Instant.parse(start), Instant.parse(end));
    }

    private static final class FakeCapture implements CaptureControl {
        private final CaptureSnapshot snapshot;

        private FakeCapture(CaptureSnapshot snapshot) {
            this.snapshot = snapshot;
        }

        @Override
        public CaptureStatus status() {
            return new CaptureStatus(CaptureState.RUNNING, "Captura activa.");
        }

        @Override
        public CaptureSnapshot snapshot() {
            return snapshot;
        }

        @Override
        public com.deriva.coach.model.DetectionSettings effectiveSettings() {
            return com.deriva.coach.model.DetectionSettings.defaults();
        }

        @Override
        public java.time.Duration effectivePassiveMax() {
            return java.time.Duration.ofMinutes(10);
        }

        @Override
        public void start() {}

        @Override
        public void stop() {}

        @Override
        public void pause() {}

        @Override
        public void deleteAllRecords() {}

        @Override
        public void shutdown() {}
    }

    private static final class FakeRepository implements SegmentRepository {
        private final List<Segment> segments;

        private FakeRepository(List<Segment> segments) {
            this.segments = segments;
        }

        @Override
        public void saveAll(List<Segment> segments) {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<Segment> between(Instant from, Instant to) {
            return segments;
        }

        @Override
        public java.util.List<com.deriva.tracking.model.ContextKey> observedContexts() {
            return java.util.List.of();
        }
    }
}
