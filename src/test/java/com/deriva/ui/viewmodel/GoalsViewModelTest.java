package com.deriva.ui.viewmodel;

import com.deriva.app.usecase.GoalsService;
import com.deriva.goals.model.Goal;
import com.deriva.goals.model.GoalAlert;
import com.deriva.goals.model.GoalMetric;
import com.deriva.goals.model.GoalState;
import com.deriva.privacy.ObservationKind;
import com.deriva.storage.port.GoalAlertRepository;
import com.deriva.storage.port.GoalRepository;
import com.deriva.storage.port.SegmentRepository;
import com.deriva.tracking.model.ContextKey;
import com.deriva.tracking.model.Segment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class GoalsViewModelTest {
    private static final Instant NOW = Instant.parse("2026-11-24T12:00:00Z");
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);
    private static final ZoneOffset ZONE = ZoneOffset.UTC;

    private InMemoryGoalRepository repository;
    private FakeSegments segments;
    private InMemoryAlertRepository alerts;
    private GoalsService service;
    private GoalsViewModel viewModel;

    @BeforeEach
    void setUp() {
        repository = new InMemoryGoalRepository();
        segments = new FakeSegments();
        alerts = new InMemoryAlertRepository();
        service = new GoalsService(repository, segments, alerts, CLOCK);
        viewModel = new GoalsViewModel(service, ZONE);
        viewModel.load();
    }

    private GoalsViewModel.Outcome createChessGoal() {
        viewModel.updateForm("chess.exe", "", "60", "4", true, true);
        return viewModel.save();
    }

    @Test
    void createsAGoalForSixtyMinutesAndFourEntries() {
        GoalsViewModel.Outcome outcome = createChessGoal();

        assertFalse(outcome.error(), outcome.message());
        assertEquals("Objetivo creado.", outcome.message());
        assertEquals(1, outcome.page().goals().size());
        GoalRow row = outcome.page().goals().getFirst();
        assertEquals("Ajedrez", row.contextLabel());
        assertEquals("60 min · 4 entradas · Pasivo", row.limitsText());
        assertEquals("0 de 60 min · 0 de 4 entradas", row.progressText());
        assertEquals(GoalState.IN_PROGRESS, row.state());
        assertEquals("En curso", row.stateLabel());
        assertEquals("▲", row.stateIcon());
        assertTrue(row.passive());
        assertTrue(row.active());
    }

    @Test
    void everyGoalMutationAdvancesThePassiveSnapshotRevision() {
        assertEquals(1, service.snapshot().revision());
        long id = createChessGoal().page().goals().getFirst().id();
        assertEquals(2, service.snapshot().revision());
        assertTrue(service.snapshot().activeGoals().getFirst().passive());

        viewModel.toggleActive(id);
        assertEquals(3, service.snapshot().revision());
        assertTrue(service.snapshot().activeGoals().isEmpty());

        viewModel.delete(id);
        assertEquals(4, service.snapshot().revision());
    }

    @Test
    void showsNearStateAndEntriesFromPersistedSegments() {
        segments.add(observed("chess.exe", null, "2026-11-24T09:00:00Z", "2026-11-24T09:30:00Z"));
        segments.add(observed("chess.exe", null, "2026-11-24T10:00:00Z", "2026-11-24T10:20:00Z"));

        GoalsViewModel.Outcome outcome = createChessGoal();

        GoalRow row = outcome.page().goals().getFirst();
        assertEquals(GoalState.NEAR, row.state());
        assertEquals("Cerca del tope", row.stateLabel());
        assertEquals("◆", row.stateIcon());
        assertEquals("50 de 60 min · 2 de 4 entradas", row.progressText());
        assertTrue(row.ratio() >= 0.8 && row.ratio() < 1);
    }

    @Test
    void showsExceededUnansweredAndJustifiedStatesAndReverts() {
        segments.add(observed("chess.exe", null,
                "2026-11-24T09:00:00Z", "2026-11-24T10:10:00Z"));
        long id = createChessGoal().page().goals().getFirst().id();

        GoalRow exceeded = viewModel.load().goals().getFirst();
        assertEquals(GoalState.EXCEEDED, exceeded.state());
        assertEquals("Excedido", exceeded.stateLabel());
        assertEquals("●", exceeded.stateIcon());
        assertEquals("exceeded", exceeded.stateKey());
        assertNull(exceeded.justification());

        alerts.add(GoalAlert.draft(id, NOW, 100, GoalMetric.MINUTES));
        GoalRow unanswered = viewModel.load().goals().getFirst();
        assertEquals(GoalState.EXCEEDED, unanswered.state());
        assertEquals("Sin responder", unanswered.stateLabel());
        assertEquals("○", unanswered.stateIcon());
        assertEquals("unanswered", unanswered.stateKey());
        assertNotNull(unanswered.justification());
        assertEquals(GoalsViewModel.UNANSWERED_NOTE, unanswered.justification().note());
        assertTrue(unanswered.justification().canJustify());
        assertEquals("Justificar exceso", unanswered.justification().actionLabel());
        assertFalse(unanswered.justification().canRemove());

        String reason = "Preparé una partida larga";
        GoalsViewModel.Outcome justified = viewModel.justify(id, reason);
        assertFalse(justified.error(), justified.message());
        GoalRow row = justified.page().goals().getFirst();
        assertEquals(GoalState.JUSTIFIED, row.state());
        assertEquals("Justificado por ti", row.stateLabel());
        assertEquals("✓", row.stateIcon());
        assertEquals("justified", row.stateKey());
        assertEquals(reason, row.justification().reason());
        assertEquals("Cubierto: 10 min", row.justification().coverage());
        assertNull(row.justification().note());
        assertTrue(row.justification().canRemove());
        assertFalse(row.justification().canJustify());
        GoalAlert stored = alerts.listByGoal(id).getFirst();
        assertTrue(stored.isJustified());
        assertEquals(reason, stored.justification());
        assertEquals(Duration.ofMinutes(10).toMillis(), stored.justifiedExcess());

        GoalsViewModel.Outcome removed = viewModel.removeJustification(id);
        assertFalse(removed.error(), removed.message());
        GoalRow reverted = removed.page().goals().getFirst();
        assertEquals("Sin responder", reverted.stateLabel());
        assertEquals(GoalsViewModel.UNANSWERED_NOTE, reverted.justification().note());
        assertFalse(alerts.listByGoal(id).getFirst().isJustified());
        assertNull(alerts.listByGoal(id).getFirst().justification());
    }

    @Test
    void laterUseCountsAgainAndRejustifyReplacesCoverage() {
        segments.add(observed("chess.exe", null,
                "2026-11-24T09:00:00Z", "2026-11-24T10:10:00Z"));
        long id = createChessGoal().page().goals().getFirst().id();
        alerts.add(GoalAlert.draft(id, NOW, 100, GoalMetric.MINUTES));
        viewModel.load();
        viewModel.justify(id, "Primera");
        assertEquals(Duration.ofMinutes(10).toMillis(),
                alerts.listByGoal(id).getFirst().justifiedExcess());

        segments.add(observed("chess.exe", null,
                "2026-11-24T10:10:00Z", "2026-11-24T10:15:00Z"));
        GoalRow exhausted = viewModel.load().goals().getFirst();
        assertEquals(GoalState.EXCEEDED, exhausted.state());
        assertEquals("Excedido", exhausted.stateLabel());
        assertEquals(GoalsViewModel.EXHAUSTED_NOTE, exhausted.justification().note());
        assertEquals("Justificar de nuevo", exhausted.justification().actionLabel());
        assertTrue(exhausted.justification().canRemove());

        viewModel.justify(id, "Segunda");
        GoalRow again = viewModel.load().goals().getFirst();
        assertEquals(GoalState.JUSTIFIED, again.state());
        assertEquals(Duration.ofMinutes(15).toMillis(),
                alerts.listByGoal(id).getFirst().justifiedExcess());
        assertEquals("Segunda", alerts.listByGoal(id).getFirst().justification());
    }

    @Test
    void openSegmentIsIncludedSoTheAlertCanBeJustified() {
        segments.add(observed("code.exe", null,
                "2026-11-24T09:00:00Z", "2026-11-24T09:08:00Z"));
        viewModel.updateForm("code.exe", "", "10", "", false, true);
        long id = viewModel.save().page().goals().getFirst().id();
        alerts.add(GoalAlert.draft(id, NOW, 100, GoalMetric.MINUTES));

        GoalRow closedOnly = viewModel.load().goals().getFirst();
        assertEquals(GoalState.NEAR, closedOnly.state());
        assertFalse(closedOnly.justification().canJustify());

        Segment open = observed("code.exe", null,
                "2026-11-24T09:08:00Z", "2026-11-24T09:12:00Z");
        GoalsViewModel withOpen = new GoalsViewModel(service, ZONE, now -> Optional.of(open));
        GoalRow live = withOpen.load().goals().getFirst();
        assertEquals(GoalState.EXCEEDED, live.state());
        assertEquals("Sin responder", live.stateLabel());
        assertTrue(live.justification().canJustify());

        GoalsViewModel.Outcome outcome = withOpen.justify(id, "Incluye el segmento abierto");
        assertFalse(outcome.error(), outcome.message());
        assertEquals(GoalState.JUSTIFIED, outcome.page().goals().getFirst().state());
        assertEquals(Duration.ofMinutes(2).toMillis(),
                alerts.listByGoal(id).getFirst().justifiedExcess());
    }

    @Test
    void rejectsAReasonLongerThanFiveHundredCharacters() {
        segments.add(observed("chess.exe", null,
                "2026-11-24T09:00:00Z", "2026-11-24T10:10:00Z"));
        long id = createChessGoal().page().goals().getFirst().id();
        alerts.add(GoalAlert.draft(id, NOW, 100, GoalMetric.MINUTES));
        viewModel.load();

        GoalsViewModel.Outcome outcome = viewModel.justify(id, "x".repeat(501));

        assertTrue(outcome.error());
        assertTrue(outcome.message().contains("500"));
        assertFalse(alerts.listByGoal(id).getFirst().isJustified());
    }

    @Test
    void justificationTextsDoNotClaimCausalityOrVerification() {
        String texts = String.join(" ",
                GoalsViewModel.stateLabel(GoalState.IN_PROGRESS),
                GoalsViewModel.stateLabel(GoalState.NEAR),
                GoalsViewModel.stateLabel(GoalState.JUSTIFIED),
                GoalsViewModel.stateLabel(GoalState.EXCEEDED),
                GoalsViewModel.UNANSWERED_NOTE,
                GoalsViewModel.EXHAUSTED_NOTE).toLowerCase(Locale.ROOT);
        for (String forbidden : List.of("comprobado", "aprobado", "válido", "porque",
                "causa", "debido", "provoca"))
            assertFalse(texts.contains(forbidden), forbidden);
        assertEquals("Justificado por ti", GoalsViewModel.stateLabel(GoalState.JUSTIFIED));
        assertEquals("Sin responder es solo una anotación tuya; no cambia los registros de actividad.",
                GoalsViewModel.UNANSWERED_NOTE);
    }

    @Test
    void editingOnlyChangesLimitsAndFlagsNotTheContext() {
        long id = createChessGoal().page().goals().getFirst().id();
        viewModel.beginEdit(id);
        assertTrue(viewModel.page().editing());
        assertTrue(viewModel.page().contextLocked());

        viewModel.updateForm("other.exe", "example.com", "90", "2", false, true);
        GoalsViewModel.Outcome outcome = viewModel.save();

        assertFalse(outcome.error(), outcome.message());
        Goal stored = repository.find(id).orElseThrow();
        assertEquals(new ContextKey("chess.exe", null), stored.context());
        assertEquals(90, stored.dailyMinutes());
        assertEquals(2, stored.dailyEntries());
        assertFalse(stored.passive());
        assertEquals("Objetivo actualizado.", outcome.message());
    }

    @Test
    void deactivatesAndReactivatesWithoutDeleting() {
        long id = createChessGoal().page().goals().getFirst().id();

        GoalsViewModel.Outcome deactivated = viewModel.toggleActive(id);
        GoalRow inactive = deactivated.page().goals().getFirst();
        assertFalse(inactive.active());
        assertEquals("Desactivado", inactive.stateLabel());
        assertEquals("Sin progreso mientras está desactivado.", inactive.progressText());
        assertEquals(1, repository.listAll().size());

        GoalsViewModel.Outcome reactivated = viewModel.toggleActive(id);
        assertTrue(reactivated.page().goals().getFirst().active());
        assertEquals("Objetivo activado.", reactivated.message());
    }

    @Test
    void deletesTheGoalAndClearsTheList() {
        long id = createChessGoal().page().goals().getFirst().id();

        GoalsViewModel.Outcome outcome = viewModel.delete(id);

        assertFalse(outcome.error());
        assertEquals("Objetivo borrado.", outcome.message());
        assertTrue(outcome.page().goals().isEmpty());
        assertTrue(repository.listAll().isEmpty());
    }

    @Test
    void rejectsMissingLimitsAndNonPositiveValues() {
        viewModel.updateForm("chess.exe", "", "", "", false, true);
        assertTrue(viewModel.save().error());
        assertTrue(viewModel.page().validationError().contains("al menos un tope"));

        viewModel.updateForm("chess.exe", "", "0", "4", false, true);
        assertTrue(viewModel.save().error());
        assertTrue(viewModel.page().validationError().contains("mayores que cero"));

        viewModel.updateForm("chess.exe", "", "60", "cuatro", false, true);
        assertTrue(viewModel.save().error());
        assertTrue(viewModel.page().validationError().contains("números enteros"));
        assertTrue(repository.listAll().isEmpty());
    }

    @Test
    void rejectsPathsUrlsAndDuplicateContexts() {
        viewModel.updateForm("C:\\chess.exe", "", "60", "", false, true);
        assertTrue(viewModel.save().error());
        assertTrue(viewModel.page().validationError().contains("sin / ni"));

        viewModel.updateForm("chrome.exe", "https://chess.com/play", "60", "", false, true);
        assertTrue(viewModel.save().error());
        assertTrue(viewModel.page().validationError().contains("dominio"));

        createChessGoal();
        viewModel.updateForm("CHESS.EXE", "", "30", "", false, true);
        assertTrue(viewModel.save().error());
        assertTrue(viewModel.page().validationError().contains("Ya existe"));
    }

    @Test
    void marksUnmeasurableWhenExcludedTimeCouldBelongToASiteGoal() {
        segments.add(observed("chrome.exe", "chess.com",
                "2026-11-24T09:00:00Z", "2026-11-24T09:10:00Z"));
        segments.add(new Segment(0, ObservationKind.EXCLUDED, null, null, null,
                Instant.parse("2026-11-24T10:00:00Z"), Instant.parse("2026-11-24T10:05:00Z")));

        viewModel.updateForm("chrome.exe", "chess.com", "30", "", false, true);
        GoalRow row = viewModel.save().page().goals().getFirst();

        assertTrue(row.unmeasurable());
        assertEquals("Chess.com · Chrome", row.contextLabel());
    }

    @Test
    void showsPendingNoticeAndTheFourHonestOutcomeTexts() {
        long id = createChessGoal().page().goals().getFirst().id();
        alerts.add(GoalAlert.draft(id, NOW, 80, GoalMetric.MINUTES));
        viewModel.load();
        assertEquals(GoalsViewModel.PENDING_NOTICE_TEXT,
                viewModel.page().goals().getFirst().alertOutcomeText());

        GoalAlert observed = new GoalAlert(1, id, NOW, 100, GoalMetric.MINUTES,
                null, null, null, null, NOW, NOW.plusSeconds(120), null, null);
        assertEquals("Después del aviso, volvió a estabilidad en 12 s.",
                GoalsViewModel.outcomeText(
                        observed.observed(GoalAlert.OUTCOME_STABLE, NOW, 12_000L)));
        assertEquals("Al enviar el aviso no había deriva y no apareció ninguna en los 2 min siguientes.",
                GoalsViewModel.outcomeText(
                        observed.observed(GoalAlert.OUTCOME_NO_DRIFT, NOW, null)));
        assertEquals("Después del aviso apareció una deriva durante la observación de 2 min.",
                GoalsViewModel.outcomeText(
                        observed.observed(GoalAlert.OUTCOME_NEW_DRIFT, NOW, null)));
        assertEquals("Después del aviso, seguía habiendo deriva al terminar la observación de 2 min.",
                GoalsViewModel.outcomeText(
                        observed.observed(GoalAlert.OUTCOME_STILL_DRIFTING, NOW, null)));
        assertEquals("Después del aviso no se pudo observar el resultado: la captura se pausó "
                        + "o la aplicación se cerró.",
                GoalsViewModel.outcomeText(
                        observed.observed(GoalAlert.OUTCOME_NOT_OBSERVED, NOW, null)));
        assertEquals("Aviso no enviado: la aplicación se reinició antes de entregarlo.",
                GoalsViewModel.outcomeText(
                        observed.observed(GoalAlert.OUTCOME_DISCARDED, NOW, null)));
    }

    @Test
    void dailySummarySeparatesJustifiedAndExceededMinutesAndListsAlertOutcomes() {
        segments.add(observed("chess.exe", null,
                "2026-11-24T09:00:00Z", "2026-11-24T10:12:00Z"));
        long id = createChessGoal().page().goals().getFirst().id();
        alerts.add(GoalAlert.draft(id, NOW, 80, GoalMetric.MINUTES));
        GoalAlert full = alerts.add(GoalAlert.draft(id, NOW, 100, GoalMetric.MINUTES))
                .notified(NOW.plusSeconds(30), NOW.plusSeconds(150))
                .observed(GoalAlert.OUTCOME_NO_DRIFT, NOW.plusSeconds(150), null);
        alerts.update(full);

        GoalRow exceeded = viewModel.load().goals().getFirst();
        assertEquals("Justificado: 0 min · Excedido sin justificar: 12 min",
                exceeded.balanceText());
        assertEquals(2, exceeded.alertLines().size());
        assertEquals("Aviso 80 % de minutos · 12:00 — " + GoalsViewModel.PENDING_NOTICE_TEXT,
                exceeded.alertLines().get(0));
        assertEquals("Aviso 100 % de minutos · 12:00 — " + GoalsViewModel.NO_DRIFT_TEXT,
                exceeded.alertLines().get(1));

        viewModel.justify(id, "Torneo");
        GoalRow justified = viewModel.load().goals().getFirst();
        assertEquals("Justificado: 12 min · Excedido sin justificar: 0 min",
                justified.balanceText());
        assertEquals(2, justified.alertLines().size());
    }

    private static Segment observed(String process, String host, String start, String end) {
        return new Segment(0, ObservationKind.OBSERVED, new ContextKey(process, host),
                null, null, Instant.parse(start), Instant.parse(end));
    }

    private static final class InMemoryGoalRepository implements GoalRepository {
        private final Map<Long, Goal> goals = new LinkedHashMap<>();
        private long nextId = 1;

        @Override
        public Goal create(Goal goal) {
            Goal stored = new Goal(nextId++, goal.context(), goal.dailyMinutes(),
                    goal.dailyEntries(), goal.passive(), goal.active(), NOW, NOW);
            goals.put(stored.id(), stored);
            return stored;
        }

        @Override
        public Optional<Goal> find(long id) {
            return Optional.ofNullable(goals.get(id));
        }

        @Override
        public void update(Goal goal) {
            goals.put(goal.id(), goal);
        }

        @Override
        public boolean delete(long id) {
            return goals.remove(id) != null;
        }

        @Override
        public List<Goal> listActive() {
            return listAll().stream().filter(Goal::active).toList();
        }

        @Override
        public List<Goal> listAll() {
            return goals.values().stream()
                    .sorted((a, b) -> {
                        if (a.active() != b.active()) return a.active() ? -1 : 1;
                        int byProcess = a.context().processName().compareTo(b.context().processName());
                        if (byProcess != 0) return byProcess;
                        String ah = a.context().siteHost() == null ? "" : a.context().siteHost();
                        String bh = b.context().siteHost() == null ? "" : b.context().siteHost();
                        return ah.compareTo(bh);
                    })
                    .toList();
        }
    }

    private static final class InMemoryAlertRepository implements GoalAlertRepository {
        private final Map<Long, GoalAlert> alerts = new LinkedHashMap<>();
        private long nextId = 1;

        private GoalAlert add(GoalAlert alert) {
            GoalAlert stored = new GoalAlert(nextId++, alert.goalId(), alert.triggeredAt(),
                    alert.thresholdPercent(), alert.metric(), alert.outcome(),
                    alert.justification(), alert.justifiedAt(), alert.justifiedExcess(),
                    alert.notifiedAt(), alert.observedUntil(), alert.outcomeAt(),
                    alert.outcomeAfterMs());
            alerts.put(stored.id(), stored);
            return stored;
        }

        @Override
        public GoalAlert create(GoalAlert alert) {
            return add(alert);
        }

        @Override
        public Optional<GoalAlert> find(long id) {
            return Optional.ofNullable(alerts.get(id));
        }

        @Override
        public void update(GoalAlert alert) {
            alerts.put(alert.id(), alert);
        }

        @Override
        public void updateAll(List<GoalAlert> values) {
            for (GoalAlert alert : values) alerts.put(alert.id(), alert);
        }

        @Override
        public boolean delete(long id) {
            return alerts.remove(id) != null;
        }

        @Override
        public List<GoalAlert> listByGoal(long goalId) {
            return alerts.values().stream()
                    .filter(alert -> alert.goalId() == goalId)
                    .sorted(Comparator.comparing(GoalAlert::triggeredAt).thenComparingLong(GoalAlert::id))
                    .toList();
        }

        @Override
        public List<GoalAlert> listPending() {
            return alerts.values().stream()
                    .filter(alert -> alert.outcome() == null)
                    .sorted(Comparator.comparing(GoalAlert::triggeredAt).thenComparingLong(GoalAlert::id))
                    .toList();
        }

        @Override
        public List<GoalAlert> listBetween(Instant from, Instant to) {
            return alerts.values().stream()
                    .filter(alert -> !alert.triggeredAt().isBefore(from)
                            && alert.triggeredAt().isBefore(to))
                    .sorted(Comparator.comparing(GoalAlert::triggeredAt).thenComparingLong(GoalAlert::id))
                    .toList();
        }

        @Override
        public List<GoalAlert> listJustifiedBetween(Instant from, Instant to) {
            return alerts.values().stream()
                    .filter(GoalAlert::isJustified)
                    .filter(alert -> !alert.justifiedAt().isBefore(from)
                            && alert.justifiedAt().isBefore(to))
                    .sorted(Comparator.comparing(GoalAlert::justifiedAt).thenComparingLong(GoalAlert::id))
                    .toList();
        }
    }

    private static final class FakeSegments implements SegmentRepository {
        private final List<Segment> segments = new ArrayList<>();

        private void add(Segment segment) {
            segments.add(segment);
        }

        @Override
        public void saveAll(List<Segment> values) {
            segments.addAll(values);
        }

        @Override
        public List<Segment> between(Instant from, Instant to) {
            return segments.stream()
                    .filter(segment -> segment.start().isBefore(to) && from.isBefore(segment.end()))
                    .toList();
        }

        @Override
        public List<ContextKey> observedContexts() {
            return segments.stream()
                    .filter(segment -> segment.kind() == ObservationKind.OBSERVED)
                    .map(Segment::context)
                    .distinct()
                    .toList();
        }
    }
}
