package com.deriva.ui.viewmodel;

import com.deriva.app.usecase.InterruptionsService;
import com.deriva.coach.model.CloseReason;
import com.deriva.coach.model.DriftEvent;
import com.deriva.coach.model.DriftStatus;
import com.deriva.coach.model.DriftUpdate;
import com.deriva.storage.port.DriftRepository;
import com.deriva.tracking.model.ContextKey;
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

class InterruptionsViewModelTest {
    private static final Instant BASE = Instant.parse("2026-11-22T11:08:00Z");
    private static final ZoneId ZONE = ZoneOffset.UTC;
    private static final ContextKey A = new ContextKey("chrome.exe", "instagram.com");
    private static final ContextKey B = new ContextKey("code.exe", null);

    private static DriftEvent stable(long id, int minute, int switches, String reason, boolean justified) {
        Instant start = BASE.plusSeconds(minute * 60L);
        return new DriftEvent(id, start, start.plusSeconds(60), start.plusSeconds(134),
                start.plusSeconds(134), start.plusSeconds(134), DriftStatus.STABLE, null,
                switches, A, 134_000L, reason, justified, null, Set.of(A, B));
    }

    private static DriftEvent open(long id, int minute, int switches) {
        Instant start = BASE.plusSeconds(minute * 60L);
        return new DriftEvent(id, start, start.plusSeconds(60), start.plusSeconds(180),
                null, null, DriftStatus.OPEN, null, switches, A, null, null, false, null, Set.of(A));
    }

    private static DriftEvent unconfirmed(long id, int minute, int switches) {
        Instant start = BASE.plusSeconds(minute * 60L);
        return new DriftEvent(id, start, start.plusSeconds(60), start.plusSeconds(120),
                null, start.plusSeconds(120), DriftStatus.UNCONFIRMED, CloseReason.IDLE,
                switches, A, null, null, false, null, Set.of(A));
    }

    private static InterruptionsViewModel viewModel(FakeRepository repository) {
        return new InterruptionsViewModel(new InterruptionsService(repository,
                Clock.fixed(BASE, ZoneOffset.UTC)), ZONE);
    }

    @Test
    void loadsRowsSortedDescendingAndCountsOpen() {
        FakeRepository repository = new FakeRepository(List.of(
                stable(1, 0, 10, null, false),
                open(2, 120, 12),
                unconfirmed(3, 30, 14)));
        InterruptionsPage page = viewModel(repository).load(LocalDate.of(2026, 11, 22),
                StatusFilter.ALL, true);

        assertEquals(3, page.totalEpisodes());
        assertEquals(1, page.openEpisodes());
        assertEquals(List.of(2L, 3L, 1L), page.rows().stream().map(InterruptionRow::id).toList());
    }

    @Test
    void filtersByStatusAndJustified() {
        FakeRepository repository = new FakeRepository(List.of(
                stable(1, 0, 10, "MESSAGES", true),
                stable(2, 10, 10, "BREAK", false),
                open(3, 20, 10)));
        InterruptionsViewModel viewModel = viewModel(repository);

        assertEquals(List.of(3L, 2L), viewModel.load(LocalDate.of(2026, 11, 22),
                StatusFilter.ALL, false).rows().stream().map(InterruptionRow::id).toList());
        assertEquals(List.of(3L), viewModel.load(LocalDate.of(2026, 11, 22),
                StatusFilter.OPEN, true).rows().stream().map(InterruptionRow::id).toList());
    }

    @Test
    void formatsFirstChangeStatusReasonAndStability() {
        FakeRepository repository = new FakeRepository(List.of(
                stable(1, 0, 10, "MESSAGES", true)));
        InterruptionRow row = viewModel(repository).load(LocalDate.of(2026, 11, 22),
                StatusFilter.ALL, true).rows().getFirst();

        assertEquals("22/11 11:08", row.startText());
        assertEquals("Instagram", row.firstChange());
        assertEquals("Estable", row.statusText());
        assertEquals("2 min 14 s", row.stabilityText());
        assertEquals("Pendiente", row.annotationText());
        assertEquals("Mensajes", row.reasonText());
        assertTrue(row.justified());
    }

    @Test
    void annotationStateUsesPersistedAnsweredAtNotReasonOrJustified() {
        Instant start = BASE.plusSeconds(15 * 60L);
        DriftEvent savedWithoutReason = new DriftEvent(2, start, start.plusSeconds(60),
                start.plusSeconds(134), start.plusSeconds(134), start.plusSeconds(134),
                DriftStatus.STABLE, null, 10, A, 134_000L, null, false,
                start.plusSeconds(200), Set.of(A));
        FakeRepository repository = new FakeRepository(List.of(
                stable(1, 0, 10, "MESSAGES", true), savedWithoutReason));
        InterruptionsViewModel viewModel = viewModel(repository);
        List<InterruptionRow> rows = viewModel.load(LocalDate.of(2026, 11, 22),
                StatusFilter.ALL, true).rows();

        InterruptionRow pending = rows.stream().filter(row -> row.id() == 1).findFirst().orElseThrow();
        InterruptionRow saved = rows.stream().filter(row -> row.id() == 2).findFirst().orElseThrow();

        assertEquals("Pendiente", pending.annotationText());
        assertNull(pending.answeredAt());
        assertEquals("Guardada", saved.annotationText());
        assertEquals("Guardada · 22/11 11:26", viewModel.annotationDetail(saved));
    }

    @Test
    void unconfirmedStabilityIsNotCheckedAndExplainsPersistedCloseReason() {
        FakeRepository repository = new FakeRepository(List.of(unconfirmed(3, 30, 14)));
        InterruptionsViewModel viewModel = viewModel(repository);
        InterruptionRow row = viewModel.load(LocalDate.of(2026, 11, 22),
                StatusFilter.ALL, true).rows().getFirst();

        assertEquals("No comprobada", row.statusText());
        assertEquals("—", row.stabilityText());
        assertTrue(viewModel.stabilityExplanation(row).contains("No se comprobó la estabilidad"));
        assertTrue(viewModel.stabilityExplanation(row).contains("inactividad"));
        assertEquals("No comprobadas", StatusFilter.UNCONFIRMED.toString());
    }

    @Test
    void requestsInclusiveDateRangeAsOneHalfOpenQuery() {
        FakeRepository repository = new FakeRepository(List.of(stable(1, 0, 10, null, false)));
        viewModel(repository).load(LocalDate.of(2026, 11, 21), LocalDate.of(2026, 11, 22),
                StatusFilter.ALL, true);

        assertEquals(1, repository.rangeCalls);
        assertEquals(Instant.parse("2026-11-21T00:00:00Z"), repository.from);
        assertEquals(Instant.parse("2026-11-23T00:00:00Z"), repository.to);
    }

    @Test
    void rejectsInvertedRange() {
        InterruptionsService service = new InterruptionsService(
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
        InterruptionsViewModel viewModel = viewModel(repository);
        LocalDate day = LocalDate.of(2026, 11, 22);

        InterruptionsPage all = viewModel.demoPage(day, StatusFilter.ALL, true);
        assertEquals(6, all.totalEpisodes());
        assertEquals(1, all.openEpisodes());
        assertEquals(6, all.rows().size());
        assertEquals(0, repository.rangeCalls);
        assertEquals(List.of(900005L, 900001L, 900003L, 900002L, 900004L, 900006L),
                all.rows().stream().map(InterruptionRow::id).toList());
        assertTrue(all.rows().stream().anyMatch(row -> row.status() == DriftStatus.OPEN));
        assertTrue(all.rows().stream().anyMatch(row -> row.status() == DriftStatus.STABLE));
        assertTrue(all.rows().stream().anyMatch(row -> row.status() == DriftStatus.UNCONFIRMED));

        InterruptionsPage open = viewModel.demoPage(day, StatusFilter.OPEN, true);
        assertEquals(1, open.rows().size());
        assertEquals(1, open.openEpisodes());

        InterruptionsPage withoutJustified = viewModel.demoPage(day, StatusFilter.ALL, false);
        assertTrue(withoutJustified.rows().stream().noneMatch(InterruptionRow::justified));
        assertTrue(withoutJustified.rows().size() < all.rows().size());
    }

    @Test
    void emptyDurationAndUnknownDisplayFallbacksAreVisible() {
        assertEquals("—", InterruptionsViewModel.duration(null));
        assertEquals("0 s", InterruptionsViewModel.duration(0L));
        ContextKey unknown = new ContextKey("unknown.exe", null);
        assertEquals("unknown", viewModel(new FakeRepository(List.of())).display(unknown));
    }

    @Test
    void savedAnnotationOnlyEnablesSavingWhenValuesDiffer() {
        var row = viewModel(new FakeRepository(List.of()))
                .demoPage(LocalDate.of(2026, 11, 23), StatusFilter.ALL, true)
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
                .demoPage(LocalDate.of(2026, 11, 23), StatusFilter.ALL, true)
                .rows().stream().filter(r -> r.answeredAt() == null).findFirst().orElseThrow();
        assertFalse(row.hasAnnotationChanges(null, false));
        assertFalse(row.hasAnnotationChanges(null, true));
        assertTrue(row.hasAnnotationChanges("OTHER", false));
    }

    private static final class FakeRepository implements DriftRepository {
        private final List<DriftEvent> events;
        private long annotatedId;
        private String reason;
        private boolean justified;
        private Instant answeredAt;
        private Instant from;
        private Instant to;
        private int rangeCalls;

        private FakeRepository(List<DriftEvent> events) {
            this.events = new ArrayList<>(events);
        }

        @Override
        public void upsertDetection(DriftUpdate update) {
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
        public List<DriftEvent> between(Instant from, Instant to) {
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
