package com.deriva.ui.viewmodel;

import com.deriva.app.usecase.EmInterruptionsService;
import com.deriva.coach.model.EmCloseReason;
import com.deriva.coach.model.EmDriftEvent;
import com.deriva.coach.model.EmDriftStatus;
import com.deriva.coach.model.EmDriftUpdate;
import com.deriva.storage.port.EmDriftRepository;
import com.deriva.tracking.model.EmContextKey;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class EmInterruptionsViewModelTest {
    private static final Instant BASE = Instant.parse("2026-11-22T11:08:00Z");
    private static final ZoneId ZONE = ZoneOffset.UTC;
    private static final EmContextKey A = new EmContextKey("chrome.exe", "instagram.com");
    private static final EmContextKey B = new EmContextKey("code.exe", null);

    private static EmDriftEvent stable(long id, int minute, int switches, String reason, boolean justified) {
        Instant start = BASE.plusSeconds(minute * 60L);
        return new EmDriftEvent(id, start, start.plusSeconds(60), start.plusSeconds(134),
                start.plusSeconds(134), start.plusSeconds(134), EmDriftStatus.STABLE, null,
                switches, A, 134_000L, reason, justified, null, Set.of(A, B));
    }

    private static EmDriftEvent open(long id, int minute, int switches) {
        Instant start = BASE.plusSeconds(minute * 60L);
        return new EmDriftEvent(id, start, start.plusSeconds(60), start.plusSeconds(180),
                null, null, EmDriftStatus.OPEN, null, switches, A, null, null, false, null, Set.of(A));
    }

    private static EmDriftEvent unconfirmed(long id, int minute, int switches) {
        Instant start = BASE.plusSeconds(minute * 60L);
        return new EmDriftEvent(id, start, start.plusSeconds(60), start.plusSeconds(120),
                null, start.plusSeconds(120), EmDriftStatus.UNCONFIRMED, EmCloseReason.IDLE,
                switches, A, null, null, false, null, Set.of(A));
    }

    private static EmInterruptionsViewModel viewModel(FakeRepository repository) {
        return new EmInterruptionsViewModel(new EmInterruptionsService(repository,
                Clock.fixed(BASE, ZoneOffset.UTC)), ZONE);
    }

    @Test
    void loadsRowsSortedDescendingAndCountsOpen() {
        FakeRepository repository = new FakeRepository(List.of(
                stable(1, 0, 10, null, false),
                open(2, 120, 12),
                unconfirmed(3, 30, 14)));
        EmInterruptionsPage page = viewModel(repository).load(LocalDate.of(2026, 11, 22),
                EmStatusFilter.ALL, true);

        assertEquals(3, page.totalEpisodes());
        assertEquals(1, page.openEpisodes());
        assertEquals(List.of(2L, 3L, 1L), page.rows().stream().map(EmInterruptionRow::id).toList());
    }

    @Test
    void filtersByStatusAndJustified() {
        FakeRepository repository = new FakeRepository(List.of(
                stable(1, 0, 10, "MESSAGES", true),
                stable(2, 10, 10, "BREAK", false),
                open(3, 20, 10)));
        EmInterruptionsViewModel viewModel = viewModel(repository);

        assertEquals(List.of(3L, 2L), viewModel.load(LocalDate.of(2026, 11, 22),
                EmStatusFilter.ALL, false).rows().stream().map(EmInterruptionRow::id).toList());
        assertEquals(List.of(3L), viewModel.load(LocalDate.of(2026, 11, 22),
                EmStatusFilter.OPEN, true).rows().stream().map(EmInterruptionRow::id).toList());
    }

    @Test
    void formatsFirstChangeStatusReasonAndStability() {
        FakeRepository repository = new FakeRepository(List.of(
                stable(1, 0, 10, "MESSAGES", true)));
        EmInterruptionRow row = viewModel(repository).load(LocalDate.of(2026, 11, 22),
                EmStatusFilter.ALL, true).rows().getFirst();

        assertEquals("22/11 11:08", row.startText());
        assertEquals("Instagram", row.firstChange());
        assertEquals("Estable", row.statusText());
        assertEquals("2 min 14 s", row.stabilityText());
        assertEquals("Pendiente", row.annotationText());
        assertEquals("Mensajes", row.reasonText());
        assertTrue(row.justified());
    }

    @Test
    void stableExplanationDoesNotHardcodeTheOldThreshold() {
        FakeRepository repository = new FakeRepository(List.of(
                stable(1, 0, 10, "MESSAGES", true)));
        EmInterruptionsViewModel viewModel = viewModel(repository);
        EmInterruptionRow row = viewModel.load(LocalDate.of(2026, 11, 22),
                EmStatusFilter.ALL, true).rows().getFirst();

        String text = viewModel.stabilityExplanation(row);
        assertFalse(text.contains("180"));
        assertTrue(text.contains("comprobación automática"));
        assertFalse(text.toLowerCase().contains("concentración"));
    }

    @Test
    void annotationStateUsesPersistedAnsweredAtNotReasonOrJustified() {
        Instant start = BASE.plusSeconds(15 * 60L);
        EmDriftEvent savedWithoutReason = new EmDriftEvent(2, start, start.plusSeconds(60),
                start.plusSeconds(134), start.plusSeconds(134), start.plusSeconds(134),
                EmDriftStatus.STABLE, null, 10, A, 134_000L, null, false,
                start.plusSeconds(200), Set.of(A));
        FakeRepository repository = new FakeRepository(List.of(
                stable(1, 0, 10, "MESSAGES", true), savedWithoutReason));
        EmInterruptionsViewModel viewModel = viewModel(repository);
        List<EmInterruptionRow> rows = viewModel.load(LocalDate.of(2026, 11, 22),
                EmStatusFilter.ALL, true).rows();

        EmInterruptionRow pending = rows.stream().filter(row -> row.id() == 1).findFirst().orElseThrow();
        EmInterruptionRow saved = rows.stream().filter(row -> row.id() == 2).findFirst().orElseThrow();

        assertEquals("Pendiente", pending.annotationText());
        assertNull(pending.answeredAt());
        assertEquals("Guardada", saved.annotationText());
        assertEquals("Guardada · 22/11 11:26", viewModel.annotationDetail(saved));
    }

    @Test
    void unconfirmedStabilityIsNotCheckedAndExplainsPersistedCloseReason() {
        FakeRepository repository = new FakeRepository(List.of(unconfirmed(3, 30, 14)));
        EmInterruptionsViewModel viewModel = viewModel(repository);
        EmInterruptionRow row = viewModel.load(LocalDate.of(2026, 11, 22),
                EmStatusFilter.ALL, true).rows().getFirst();

        assertEquals("No comprobada", row.statusText());
        assertEquals("—", row.stabilityText());
        assertTrue(viewModel.stabilityExplanation(row).contains("No se comprobó la estabilidad"));
        assertTrue(viewModel.stabilityExplanation(row).contains("inactividad"));
        assertEquals("No comprobadas", EmStatusFilter.UNCONFIRMED.toString());
    }

    @Test
    void requestsInclusiveDateRangeAsOneHalfOpenQuery() {
        FakeRepository repository = new FakeRepository(List.of(stable(1, 0, 10, null, false)));
        viewModel(repository).load(LocalDate.of(2026, 11, 21), LocalDate.of(2026, 11, 22),
                EmStatusFilter.ALL, true);

        assertEquals(1, repository.rangeCalls);
        assertEquals(Instant.parse("2026-11-21T00:00:00Z"), repository.from);
        assertEquals(Instant.parse("2026-11-23T00:00:00Z"), repository.to);
    }

    @Test
    void rejectsInvertedRange() {
        EmInterruptionsService service = new EmInterruptionsService(
                new FakeRepository(List.of()), Clock.fixed(BASE, ZoneOffset.UTC));

        assertThrows(IllegalArgumentException.class,
                () -> service.forRange(LocalDate.of(2026, 11, 22), LocalDate.of(2026, 11, 21), ZONE));
    }

    @Test
    void annotateDelegatesToRepository() {
        FakeRepository repository = new FakeRepository(List.of(stable(1, 0, 10, null, false)));
        viewModel(repository).annotate(42, "BREAK", true);

        assertEquals(42, repository.annotatedId);
        assertEquals("BREAK", repository.reason);
        assertTrue(repository.justified);
        assertEquals(BASE, repository.answeredAt);
    }

    @Test
    void demoPageIsSyntheticFiltersAndDoesNotQueryRepository() {
        FakeRepository repository = new FakeRepository(List.of());
        EmInterruptionsViewModel viewModel = viewModel(repository);
        LocalDate day = LocalDate.of(2026, 11, 22);

        EmInterruptionsPage all = viewModel.demoPage(day, EmStatusFilter.ALL, true);
        assertEquals(6, all.totalEpisodes());
        assertEquals(1, all.openEpisodes());
        assertEquals(6, all.rows().size());
        assertEquals(0, repository.rangeCalls);
        assertEquals(List.of(900005L, 900001L, 900003L, 900002L, 900004L, 900006L),
                all.rows().stream().map(EmInterruptionRow::id).toList());
        assertTrue(all.rows().stream().anyMatch(row -> row.status() == EmDriftStatus.OPEN));
        assertTrue(all.rows().stream().anyMatch(row -> row.status() == EmDriftStatus.STABLE));
        assertTrue(all.rows().stream().anyMatch(row -> row.status() == EmDriftStatus.UNCONFIRMED));

        EmInterruptionsPage open = viewModel.demoPage(day, EmStatusFilter.OPEN, true);
        assertEquals(1, open.rows().size());
        assertEquals(1, open.openEpisodes());

        EmInterruptionsPage withoutJustified = viewModel.demoPage(day, EmStatusFilter.ALL, false);
        assertTrue(withoutJustified.rows().stream().noneMatch(EmInterruptionRow::justified));
        assertTrue(withoutJustified.rows().size() < all.rows().size());
    }

    @Test
    void emptyDurationAndUnknownDisplayFallbacksAreVisible() {
        assertEquals("—", EmInterruptionsViewModel.duration(null));
        assertEquals("0 s", EmInterruptionsViewModel.duration(0L));
        EmContextKey unknown = new EmContextKey("unknown.exe", null);
        assertEquals("unknown", viewModel(new FakeRepository(List.of())).display(unknown));
    }

    @Test
    void savedAnnotationOnlyEnablesSavingWhenValuesDiffer() {
        var row = viewModel(new FakeRepository(List.of()))
                .demoPage(LocalDate.of(2026, 11, 23), EmStatusFilter.ALL, true)
                .rows().stream().filter(r -> r.answeredAt() != null).findFirst().orElseThrow();
        assertFalse(row.hasAnnotationChanges(row.reasonCode(), row.justified()));
        assertTrue(row.hasAnnotationChanges("OTHER", row.justified()));
        assertTrue(row.hasAnnotationChanges(row.reasonCode(), !row.justified()));
        assertFalse(row.hasAnnotationChanges(row.reasonCode(), row.justified()));
        assertFalse(row.hasAnnotationChanges(null, !row.justified()));
    }

    @Test
    void firstAnnotationRequiresReasonEvenWhenJustifiedChanges() {
        var row = viewModel(new FakeRepository(List.of()))
                .demoPage(LocalDate.of(2026, 11, 23), EmStatusFilter.ALL, true)
                .rows().stream().filter(r -> r.answeredAt() == null).findFirst().orElseThrow();
        assertFalse(row.hasAnnotationChanges(null, false));
        assertFalse(row.hasAnnotationChanges(null, true));
        assertTrue(row.hasAnnotationChanges("OTHER", false));
    }

    private static final class FakeRepository implements EmDriftRepository {
        private final List<EmDriftEvent> events;
        private long annotatedId;
        private String reason;
        private boolean justified;
        private Instant answeredAt;
        private Instant from;
        private Instant to;
        private int rangeCalls;

        private FakeRepository(List<EmDriftEvent> events) {
            this.events = new ArrayList<>(events);
        }

        @Override
        public void upsertDetection(EmDriftUpdate update) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void annotate(long id, String reason, boolean justified, Instant answeredAt) {
            this.annotatedId = id;
            this.reason = reason;
            this.justified = justified;
            this.answeredAt = answeredAt;
        }

        @Override
        public List<EmDriftEvent> between(Instant from, Instant to) {
            this.from = from;
            this.to = to;
            this.rangeCalls++;
            return events;
        }

        @Override
        public int recoverOpenEpisodes() {
            return 0;
        }
    }
}
