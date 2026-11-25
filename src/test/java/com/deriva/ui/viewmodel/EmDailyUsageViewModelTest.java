package com.deriva.ui.viewmodel;

import com.deriva.app.usecase.EmCaptureControl;
import com.deriva.app.usecase.EmCaptureSnapshot;
import com.deriva.app.usecase.EmCaptureState;
import com.deriva.app.usecase.EmCaptureStatus;
import com.deriva.app.usecase.EmDailyUsageService;
import com.deriva.privacy.EmObservationKind;
import com.deriva.storage.port.EmSegmentRepository;
import com.deriva.tracking.model.EmContextKey;
import com.deriva.tracking.model.EmSegment;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class EmDailyUsageViewModelTest {
    private static final LocalDate DAY = LocalDate.of(2026, 11, 22);
    private static final Instant NOON = Instant.parse("2026-11-22T12:00:00Z");
    private static final ZoneId ZONE = ZoneOffset.UTC;

    @Test
    void distinguishesNoDataFromZeroForeground() {
        EmDailyUsageViewModel empty = viewModel(List.of());
        EmUsagePage noData = empty.load(DAY);
        assertEquals("Sin datos", noData.totalText());
        assertFalse(noData.hasSegments());
        assertTrue(noData.statusText().contains("Sin datos"));
        assertEquals("Sin registro para este periodo", noData.coverageText());
        assertTrue(noData.apps().isEmpty());
        assertTrue(noData.sites().isEmpty());

        EmUsagePage onlyIdle = viewModel(List.of(idle("2026-11-22T08:00:00Z", "2026-11-22T08:30:00Z")))
                .load(DAY);
        assertEquals("0 s", onlyIdle.totalText());
        assertTrue(onlyIdle.hasSegments());
        assertTrue(onlyIdle.statusText().contains("Sin uso en primer plano"));
        assertTrue(onlyIdle.apps().isEmpty());
        assertTrue(onlyIdle.sites().isEmpty());
    }

    @Test
    void formatsAndSortsAppsAndSites() {
        List<EmSegment> segments = List.of(
                observed("chrome.exe", "youtube.com", "2026-11-22T08:00:00Z", "2026-11-22T09:00:00Z"),
                observed("chrome.exe", "instagram.com", "2026-11-22T09:00:00Z", "2026-11-22T09:30:00Z"),
                observed("code.exe", null, "2026-11-22T09:30:00Z", "2026-11-22T10:00:00Z"),
                observed("chrome.exe", null, "2026-11-22T10:00:00Z", "2026-11-22T10:15:00Z"));

        EmUsagePage page = new EmDailyUsageViewModel(
                new EmDailyUsageService(new FakeRepository(segments), Clock.fixed(NOON, ZONE)), ZONE)
                .load(DAY);

        assertEquals("2 h 15 min", page.totalText());
        assertEquals("Zona horaria: Z", page.zoneText());
        assertEquals(List.of("Chrome", "VS Code"),
                page.apps().stream().map(EmUsageRow::name).toList());
        assertEquals(List.of("1 h 45 min", "30 min"),
                page.apps().stream().map(EmUsageRow::durationText).toList());
        assertEquals(1.0, page.apps().getFirst().ratio(), 0.0001);
        assertEquals(List.of("YouTube", "Instagram"),
                page.sites().stream().map(EmUsageRow::name).toList());
        assertEquals(List.of("1 h", "30 min"),
                page.sites().stream().map(EmUsageRow::durationText).toList());
        assertEquals(1.0, page.sites().getFirst().ratio(), 0.0001);
        assertTrue(page.statusText().contains("segmento abierto"));
    }

    @Test
    void liveSnapshotMarksOpenAndPendingWithoutCountingPending() {
        EmSegment persisted = observed("chrome.exe", "youtube.com",
                "2026-11-22T08:00:00Z", "2026-11-22T08:30:00Z");
        EmSegment open = observed("chrome.exe", "youtube.com",
                "2026-11-22T08:30:00Z", "2026-11-22T09:00:00Z");
        EmSegment pending = observed("code.exe", null,
                "2026-11-22T09:00:00Z", "2026-11-22T09:05:00Z");
        EmCaptureSnapshot snapshot = new EmCaptureSnapshot(
                Instant.parse("2026-11-22T09:05:00Z"), open, pending, true);
        EmDailyUsageViewModel viewModel = new EmDailyUsageViewModel(
                new EmDailyUsageService(new FakeRepository(List.of(persisted)),
                        Clock.fixed(NOON, ZONE)), ZONE, new FakeCapture(snapshot));

        EmUsagePage page = viewModel.load(DAY);

        assertEquals("1 h", page.totalText());
        assertEquals(List.of("Chrome"), page.apps().stream().map(EmUsageRow::name).toList());
        assertEquals(List.of("YouTube"), page.sites().stream().map(EmUsageRow::name).toList());
        assertTrue(page.statusText().contains("abierto: YouTube"));
        assertTrue(page.statusText().contains("pendiente: VS Code"));
        assertTrue(page.statusText().contains("no suma hasta confirmarse"));

        EmUsagePage yesterday = viewModel.load(DAY.minusDays(1));
        assertFalse(yesterday.statusText().contains("abierto:"));
    }

    private static EmDailyUsageViewModel viewModel(List<EmSegment> segments) {
        return new EmDailyUsageViewModel(new EmDailyUsageService(new FakeRepository(segments),
                Clock.fixed(NOON, ZONE)), ZONE);
    }

    private static EmSegment observed(String process, String host, String start, String end) {
        return new EmSegment(0, EmObservationKind.OBSERVED, new EmContextKey(process, host),
                null, null, Instant.parse(start), Instant.parse(end));
    }

    private static EmSegment idle(String start, String end) {
        return new EmSegment(0, EmObservationKind.IDLE, null, null, null,
                Instant.parse(start), Instant.parse(end));
    }

    private static final class FakeCapture implements EmCaptureControl {
        private final EmCaptureSnapshot snapshot;

        private FakeCapture(EmCaptureSnapshot snapshot) {
            this.snapshot = snapshot;
        }

        @Override
        public EmCaptureStatus status() {
            return new EmCaptureStatus(EmCaptureState.RUNNING, "Captura activa.");
        }

        @Override
        public EmCaptureSnapshot snapshot() {
            return snapshot;
        }

        @Override
        public com.deriva.coach.model.EmDetectionSettings effectiveSettings() {
            return com.deriva.coach.model.EmDetectionSettings.defaults();
        }

        @Override
        public java.time.Duration effectivePassiveMax() {
            return java.time.Duration.ofMinutes(10);
        }

        @Override
        public java.time.Duration effectiveAlertDeferral() {
            return java.time.Duration.ofMinutes(5);
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

    private static final class FakeRepository implements EmSegmentRepository {
        private final List<EmSegment> segments;

        private FakeRepository(List<EmSegment> segments) {
            this.segments = segments;
        }

        @Override
        public void saveAll(List<EmSegment> segments) {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<EmSegment> between(Instant from, Instant to) {
            return segments;
        }

        @Override
        public java.util.List<com.deriva.tracking.model.EmContextKey> observedContexts() {
            return java.util.List.of();
        }
    }
}
