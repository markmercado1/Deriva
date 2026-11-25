package com.deriva.ui.viewmodel;

import com.deriva.app.usecase.EmGoalsService;
import com.deriva.goals.model.EmGoal;
import com.deriva.goals.model.EmGoalAlert;
import com.deriva.goals.model.EmGoalMetric;
import com.deriva.goals.model.EmGoalRevision;
import com.deriva.goals.model.EmGoalState;
import com.deriva.privacy.EmObservationKind;
import com.deriva.storage.port.EmGoalAlertRepository;
import com.deriva.storage.port.EmGoalRepository;
import com.deriva.storage.port.EmSegmentRepository;
import com.deriva.tracking.model.EmContextKey;
import com.deriva.tracking.model.EmSegment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class EmGoalsViewModelTest {
    private static final Instant NOW = Instant.parse("2026-11-24T12:00:00Z");
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);
    private static final ZoneOffset ZONE = ZoneOffset.UTC;

    private InMemoryGoalRepository repository;
    private FakeSegments segments;
    private InMemoryAlertRepository alerts;
    private EmGoalsService service;
    private EmGoalsViewModel viewModel;

    @BeforeEach
    void setUp() {
        repository = new InMemoryGoalRepository();
        segments = new FakeSegments();
        alerts = new InMemoryAlertRepository();
        service = new EmGoalsService(repository, segments, alerts, CLOCK);
        viewModel = new EmGoalsViewModel(service, ZONE);
        viewModel.load();
    }

    private EmGoalsViewModel.Outcome createChessGoal() {
        viewModel.updateForm("chess.exe", "", "60", "4", true, true);
        return viewModel.save();
    }

    @Test
    void createsAGoalForSixtyMinutesAndFourEntries() {
        EmGoalsViewModel.Outcome outcome = createChessGoal();

        assertFalse(outcome.error(), outcome.message());
        assertEquals("Objetivo creado.", outcome.message());
        assertEquals(1, outcome.page().goals().size());
        EmGoalRow row = outcome.page().goals().getFirst();
        assertEquals("Ajedrez", row.contextLabel());
        assertEquals("60 min · 4 entradas · Pasivo", row.limitsText());
        assertEquals("0 de 60 min · 0 de 4 entradas", row.progressText());
        assertEquals(EmGoalState.IN_PROGRESS, row.state());
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

        EmGoalsViewModel.Outcome outcome = createChessGoal();

        EmGoalRow row = outcome.page().goals().getFirst();
        assertEquals(EmGoalState.NEAR, row.state());
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

        EmGoalRow exceeded = viewModel.load().goals().getFirst();
        assertEquals(EmGoalState.EXCEEDED, exceeded.state());
        assertEquals("Excedido", exceeded.stateLabel());
        assertEquals("●", exceeded.stateIcon());
        assertEquals("exceeded", exceeded.stateKey());
        assertNull(exceeded.justification());

        alerts.add(EmGoalAlert.draft(id, NOW, 100, EmGoalMetric.MINUTES));
        EmGoalRow unanswered = viewModel.load().goals().getFirst();
        assertEquals(EmGoalState.EXCEEDED, unanswered.state());
        assertEquals("Sin responder", unanswered.stateLabel());
        assertEquals("○", unanswered.stateIcon());
        assertEquals("unanswered", unanswered.stateKey());
        assertNotNull(unanswered.justification());
        assertEquals(EmGoalsViewModel.UNANSWERED_NOTE, unanswered.justification().note());
        assertTrue(unanswered.justification().canJustify());
        assertEquals("Justificar exceso", unanswered.justification().actionLabel());
        assertFalse(unanswered.justification().canRemove());

        String reason = "Preparé una partida larga";
        EmGoalsViewModel.Outcome justified = viewModel.justify(id, reason);
        assertFalse(justified.error(), justified.message());
        EmGoalRow row = justified.page().goals().getFirst();
        assertEquals(EmGoalState.JUSTIFIED, row.state());
        assertEquals("Justificado por ti", row.stateLabel());
        assertEquals("✓", row.stateIcon());
        assertEquals("justified", row.stateKey());
        assertEquals(reason, row.justification().reason());
        assertEquals("Cubierto: 10 min", row.justification().coverage());
        assertNull(row.justification().note());
        assertTrue(row.justification().canRemove());
        assertFalse(row.justification().canJustify());
        EmGoalAlert stored = alerts.listByGoal(id).getFirst();
        assertTrue(stored.isJustified());
        assertEquals(reason, stored.justification());
        assertEquals(Duration.ofMinutes(10).toMillis(), stored.justifiedExcess());

        EmGoalsViewModel.Outcome removed = viewModel.removeJustification(id);
        assertFalse(removed.error(), removed.message());
        EmGoalRow reverted = removed.page().goals().getFirst();
        assertEquals("Sin responder", reverted.stateLabel());
        assertEquals(EmGoalsViewModel.UNANSWERED_NOTE, reverted.justification().note());
        assertFalse(alerts.listByGoal(id).getFirst().isJustified());
        assertNull(alerts.listByGoal(id).getFirst().justification());
    }

    @Test
    void laterUseCountsAgainAndRejustifyReplacesCoverage() {
        segments.add(observed("chess.exe", null,
                "2026-11-24T09:00:00Z", "2026-11-24T10:10:00Z"));
        long id = createChessGoal().page().goals().getFirst().id();
        alerts.add(EmGoalAlert.draft(id, NOW, 100, EmGoalMetric.MINUTES));
        viewModel.load();
        viewModel.justify(id, "Primera");
        assertEquals(Duration.ofMinutes(10).toMillis(),
                alerts.listByGoal(id).getFirst().justifiedExcess());

        segments.add(observed("chess.exe", null,
                "2026-11-24T10:10:00Z", "2026-11-24T10:15:00Z"));
        EmGoalRow exhausted = viewModel.load().goals().getFirst();
        assertEquals(EmGoalState.EXCEEDED, exhausted.state());
        assertEquals("Excedido", exhausted.stateLabel());
        assertEquals(EmGoalsViewModel.EXHAUSTED_NOTE, exhausted.justification().note());
        assertEquals("Justificar de nuevo", exhausted.justification().actionLabel());
        assertTrue(exhausted.justification().canRemove());

        viewModel.justify(id, "Segunda");
        EmGoalRow again = viewModel.load().goals().getFirst();
        assertEquals(EmGoalState.JUSTIFIED, again.state());
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
        alerts.add(EmGoalAlert.draft(id, NOW, 100, EmGoalMetric.MINUTES));

        EmGoalRow closedOnly = viewModel.load().goals().getFirst();
        assertEquals(EmGoalState.NEAR, closedOnly.state());
        assertFalse(closedOnly.justification().canJustify());

        EmSegment open = observed("code.exe", null,
                "2026-11-24T09:08:00Z", "2026-11-24T09:12:00Z");
        EmGoalsViewModel withOpen = new EmGoalsViewModel(service, ZONE, now -> Optional.of(open));
        EmGoalRow live = withOpen.load().goals().getFirst();
        assertEquals(EmGoalState.EXCEEDED, live.state());
        assertEquals("Sin responder", live.stateLabel());
        assertTrue(live.justification().canJustify());

        EmGoalsViewModel.Outcome outcome = withOpen.justify(id, "Incluye el segmento abierto");
        assertFalse(outcome.error(), outcome.message());
        assertEquals(EmGoalState.JUSTIFIED, outcome.page().goals().getFirst().state());
        assertEquals(Duration.ofMinutes(2).toMillis(),
                alerts.listByGoal(id).getFirst().justifiedExcess());
    }

    @Test
    void rejectsAReasonLongerThanFiveHundredCharacters() {
        segments.add(observed("chess.exe", null,
                "2026-11-24T09:00:00Z", "2026-11-24T10:10:00Z"));
        long id = createChessGoal().page().goals().getFirst().id();
        alerts.add(EmGoalAlert.draft(id, NOW, 100, EmGoalMetric.MINUTES));
        viewModel.load();

        EmGoalsViewModel.Outcome outcome = viewModel.justify(id, "x".repeat(501));

        assertTrue(outcome.error());
        assertTrue(outcome.message().contains("500"));
        assertFalse(alerts.listByGoal(id).getFirst().isJustified());
    }

    @Test
    void justificationTextsDoNotClaimCausalityOrVerification() {
        String texts = String.join(" ",
                EmGoalsViewModel.stateLabel(EmGoalState.IN_PROGRESS),
                EmGoalsViewModel.stateLabel(EmGoalState.NEAR),
                EmGoalsViewModel.stateLabel(EmGoalState.JUSTIFIED),
                EmGoalsViewModel.stateLabel(EmGoalState.EXCEEDED),
                EmGoalsViewModel.UNANSWERED_NOTE,
                EmGoalsViewModel.EXHAUSTED_NOTE).toLowerCase(Locale.ROOT);
        for (String forbidden : List.of("comprobado", "aprobado", "válido", "porque",
                "causa", "debido", "provoca"))
            assertFalse(texts.contains(forbidden), forbidden);
        assertEquals("Justificado por ti", EmGoalsViewModel.stateLabel(EmGoalState.JUSTIFIED));
        assertEquals("Sin responder es solo una anotación tuya; no cambia los registros de actividad.",
                EmGoalsViewModel.UNANSWERED_NOTE);
    }

    @Test
    void editingOnlyChangesLimitsAndFlagsNotTheContext() {
        long id = createChessGoal().page().goals().getFirst().id();
        viewModel.beginEdit(id);
        assertTrue(viewModel.page().editing());
        assertTrue(viewModel.page().contextLocked());

        viewModel.updateForm("other.exe", "example.com", "90", "2", false, true);
        EmGoalsViewModel.Outcome outcome = viewModel.save();

        assertFalse(outcome.error(), outcome.message());
        EmGoal stored = repository.find(id).orElseThrow();
        assertEquals(new EmContextKey("chess.exe", null), stored.context());
        assertEquals(90, stored.dailyMinutes());
        assertEquals(2, stored.dailyEntries());
        assertFalse(stored.passive());
        assertEquals("Objetivo actualizado.", outcome.message());
    }

    @Test
    void deactivatesAndReactivatesWithoutDeleting() {
        long id = createChessGoal().page().goals().getFirst().id();

        EmGoalsViewModel.Outcome deactivated = viewModel.toggleActive(id);
        EmGoalRow inactive = deactivated.page().goals().getFirst();
        assertFalse(inactive.active());
        assertEquals("Desactivado", inactive.stateLabel());
        assertEquals("Sin progreso mientras está desactivado.", inactive.progressText());
        assertEquals(1, repository.listAll().size());

        EmGoalsViewModel.Outcome reactivated = viewModel.toggleActive(id);
        assertTrue(reactivated.page().goals().getFirst().active());
        assertEquals("Objetivo activado.", reactivated.message());
    }

    @Test
    void pastDaysAreReadOnlyAndShowTheConfigurationUsedAndTheChanges() {
        EmContextKey chess = new EmContextKey("chess.exe", null);
        Instant created = Instant.parse("2026-11-22T08:00:00Z");
        EmGoal original = new EmGoal(1, chess, 30, 4, true, true, created, created);
        repository.put(original);
        repository.addRevision(original, created, false, false);
        Instant changed = Instant.parse("2026-11-22T18:00:00Z");
        EmGoal updated = new EmGoal(1, chess, 60, 4, true, true, created, changed);
        repository.put(updated);
        repository.addRevision(updated, changed, false, false);
        segments.add(observed("chess.exe", null,
                "2026-11-22T09:00:00Z", "2026-11-22T09:30:00Z"));

        EmGoalsPage page = viewModel.load(LocalDate.of(2026, 11, 22));

        assertTrue(page.historical());
        assertEquals(LocalDate.of(2026, 11, 22), page.selectedDate());
        assertEquals("Consulta histórica", page.formTitle());
        assertFalse(page.canSave());
        assertEquals(1, page.goals().size());
        EmGoalRow row = page.goals().getFirst();
        assertEquals("60 min · 4 entradas · Pasivo", row.limitsText());
        assertEquals("Configuración usada para el resumen: 60 min · 4 entradas · Pasivo",
                row.configurationNote());
        assertTrue(row.progressText().startsWith("30 de 60 min"));
        assertEquals(2, page.revisionLines().size());
        assertTrue(page.revisionLines().get(1).startsWith("18:00"));
        assertTrue(page.revisionLines().get(1).contains("60 min"));
        assertTrue(page.historyNote().contains("no se pueden recuperar"));

        EmGoalsViewModel.Outcome blocked = viewModel.delete(1);
        assertTrue(blocked.error());
        assertTrue(blocked.message().contains("solo lectura"));
        assertEquals(1, repository.listAll().size());
    }

    @Test
    void historyKeepsAnArchivedGoalOnItsLastDayAndHidesItAfterwards() {
        EmContextKey chess = new EmContextKey("chess.exe", null);
        Instant created = Instant.parse("2026-11-22T08:00:00Z");
        Instant archived = Instant.parse("2026-11-23T10:00:00Z");
        EmGoal goal = new EmGoal(1, chess, 30, 4, true, true, created, archived);
        repository.put(goal);
        repository.addRevision(goal, created, false, false);
        repository.addRevision(goal, archived, true, false);
        segments.add(observed("chess.exe", null,
                "2026-11-23T09:00:00Z", "2026-11-23T09:15:00Z"));

        EmGoalsPage lastDay = viewModel.load(LocalDate.of(2026, 11, 23));
        assertEquals(1, lastDay.goals().size());
        assertTrue(lastDay.goals().getFirst().configurationNote().contains("Borrado ese día"));

        EmGoalsPage afterwards = viewModel.load(LocalDate.of(2026, 11, 25));
        assertTrue(afterwards.historical());
        assertTrue(afterwards.goals().isEmpty());
        assertTrue(EmGoalsViewModel.HISTORY_EMPTY_TEXT.contains("no se pueden recuperar"));
    }

    @Test
    void deletedGoalStaysVisibleReadOnlyAndCanBeHidden() {
        long id = createChessGoal().page().goals().getFirst().id();

        EmGoalsViewModel.Outcome outcome = viewModel.delete(id);

        assertFalse(outcome.error());
        assertEquals("Objetivo borrado.", outcome.message());
        assertTrue(repository.listAll().isEmpty());
        assertEquals(1, outcome.page().goals().size());
        EmGoalRow row = outcome.page().goals().getFirst();
        assertTrue(row.archived());
        assertTrue(row.configurationNote().contains("solo lectura"));
        assertTrue(outcome.page().summary().contains("1 borrado hoy"));
        assertFalse(outcome.page().historical());

        assertTrue(viewModel.beginEdit(id).processText().isBlank());
        assertTrue(viewModel.toggleActive(id).error());

        EmGoalsPage hidden = viewModel.setShowArchived(false);
        assertTrue(hidden.goals().isEmpty());
        assertFalse(hidden.showArchived());
        assertTrue(hidden.summary().contains("1 borrado hoy oculto"));
        assertTrue(viewModel.load().goals().isEmpty());
    }

    @Test
    void periodSummarizesEachDayWithItsOwnConfigurationAndKeepsDeletedGoals() {
        EmContextKey chess = new EmContextKey("chess.exe", null);
        Instant chessCreated = Instant.parse("2026-11-20T08:00:00Z");
        EmGoal chessGoal = new EmGoal(1, chess, 30, 4, false, true, chessCreated, chessCreated);
        repository.addRevision(chessGoal, chessCreated, false, false);
        Instant changed = Instant.parse("2026-11-22T18:00:00Z");
        EmGoal chessWider = new EmGoal(1, chess, 60, 4, false, true, chessCreated, changed);
        repository.addRevision(chessWider, changed, false, false);
        Instant deleted = Instant.parse("2026-11-23T10:00:00Z");
        repository.addRevision(chessWider, deleted, true, false);

        EmContextKey dota = new EmContextKey("dota2.exe", null);
        Instant dotaCreated = Instant.parse("2026-11-22T07:00:00Z");
        EmGoal dotaGoal = new EmGoal(2, dota, 30, null, false, true, dotaCreated, dotaCreated);
        repository.put(dotaGoal);
        repository.addRevision(dotaGoal, dotaCreated, false, false);

        segments.add(observed("chess.exe", null, "2026-11-21T09:00:00Z", "2026-11-21T09:40:00Z"));
        segments.add(observed("chess.exe", null, "2026-11-22T09:00:00Z", "2026-11-22T09:10:00Z"));
        segments.add(observed("chess.exe", null, "2026-11-23T09:00:00Z", "2026-11-23T09:15:00Z"));
        segments.add(observed("chess.exe", null, "2026-11-23T11:00:00Z", "2026-11-23T11:30:00Z"));

        EmGoalsPage page = viewModel.load(LocalDate.of(2026, 11, 20), LocalDate.of(2026, 11, 24));

        assertTrue(page.range());
        assertTrue(page.historical());
        assertFalse(page.canSave());
        assertEquals(LocalDate.of(2026, 11, 24), page.rangeTo());
        assertEquals("Objetivos del periodo", page.listTitle());
        assertEquals("Detalle por día", page.summaryTitle());
        assertTrue(page.summary().contains("5 días · 2 objetivos · 1 borrado"), page.summary());
        assertEquals(2, page.goals().size());

        EmGoalRow chessRow = page.goals().getFirst();
        assertTrue(chessRow.archived());
        assertEquals("Tope superado 1 de 4 días", chessRow.stateLabel());
        assertEquals("exceeded", chessRow.stateKey());
        assertEquals(4, chessRow.alertLines().size());
        assertTrue(chessRow.alertLines().get(1).startsWith("21/11/2026 · 40 de 30 min"),
                chessRow.alertLines().get(1));
        assertTrue(chessRow.alertLines().get(1).contains("Excedido"));
        assertTrue(chessRow.alertLines().get(2).startsWith("22/11/2026 · 10 de 60 min"),
                chessRow.alertLines().get(2));
        // Use after the deletion at 10:00 does not count.
        assertTrue(chessRow.alertLines().get(3).startsWith("23/11/2026 · 15 de 60 min"),
                chessRow.alertLines().get(3));
        assertTrue(chessRow.alertLines().get(3).contains("Borrado a las 10:00"));
        assertTrue(chessRow.configurationNote().contains("Borrado el 23/11/2026"));
        assertEquals("Justificado: 0 min · Excedido sin justificar: 10 min", chessRow.balanceText());

        EmGoalRow dotaRow = page.goals().get(1);
        assertFalse(dotaRow.archived());
        assertEquals("Sin excesos", dotaRow.stateLabel());
        assertEquals(3, dotaRow.alertLines().size());
        assertTrue(page.revisionLines().stream().anyMatch(line -> line.startsWith("23/11/2026 10:00")));

        assertTrue(viewModel.delete(2).error());
        assertEquals(1, repository.listAll().size());

        EmGoalsPage hidden = viewModel.setShowArchived(false);
        assertEquals(1, hidden.goals().size());
        assertEquals(2, hidden.goals().getFirst().id());
        assertTrue(hidden.summary().contains("1 borrado oculto"), hidden.summary());
    }

    @Test
    void rejectsInvertedOrTooLongPeriodsAndKeepsTheSelection() {
        assertEquals(EmGoalsViewModel.RANGE_ORDER_ERROR,
                viewModel.selectRange(LocalDate.of(2026, 11, 24), LocalDate.of(2026, 11, 20)));
        assertEquals(EmGoalsViewModel.RANGE_LENGTH_ERROR,
                viewModel.selectRange(LocalDate.of(2025, 1, 1), LocalDate.of(2026, 11, 24)));
        EmGoalsPage page = viewModel.load();
        assertFalse(page.range());
        assertFalse(page.historical());
        assertEquals(LocalDate.of(2026, 11, 24), page.selectedDate());
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
        segments.add(new EmSegment(0, EmObservationKind.EXCLUDED, null, null, null,
                Instant.parse("2026-11-24T10:00:00Z"), Instant.parse("2026-11-24T10:05:00Z")));

        viewModel.updateForm("chrome.exe", "chess.com", "30", "", false, true);
        EmGoalRow row = viewModel.save().page().goals().getFirst();

        assertTrue(row.unmeasurable());
        assertEquals("Chess.com · Chrome", row.contextLabel());
    }

    @Test
    void showsPendingNoticeAndTheFourHonestOutcomeTexts() {
        long id = createChessGoal().page().goals().getFirst().id();
        alerts.add(EmGoalAlert.draft(id, NOW, 80, EmGoalMetric.MINUTES));
        viewModel.load();
        assertEquals(EmGoalsViewModel.PENDING_NOTICE_TEXT,
                viewModel.page().goals().getFirst().alertOutcomeText());

        EmGoalAlert observed = new EmGoalAlert(1, id, NOW, 100, EmGoalMetric.MINUTES,
                null, null, null, null, NOW, NOW.plusSeconds(120), null, null);
        assertEquals("Después del aviso, volvió a estabilidad en 12 s.",
                EmGoalsViewModel.outcomeText(
                        observed.observed(EmGoalAlert.OUTCOME_STABLE, NOW, 12_000L)));
        assertEquals("Al enviar el aviso no había deriva y no apareció ninguna en los 2 min siguientes.",
                EmGoalsViewModel.outcomeText(
                        observed.observed(EmGoalAlert.OUTCOME_NO_DRIFT, NOW, null)));
        assertEquals("Después del aviso apareció una deriva durante la observación de 2 min.",
                EmGoalsViewModel.outcomeText(
                        observed.observed(EmGoalAlert.OUTCOME_NEW_DRIFT, NOW, null)));
        assertEquals("Después del aviso, seguía habiendo deriva al terminar la observación de 2 min.",
                EmGoalsViewModel.outcomeText(
                        observed.observed(EmGoalAlert.OUTCOME_STILL_DRIFTING, NOW, null)));
        assertEquals("Después del aviso no se pudo observar el resultado: la captura se pausó "
                        + "o la aplicación se cerró.",
                EmGoalsViewModel.outcomeText(
                        observed.observed(EmGoalAlert.OUTCOME_NOT_OBSERVED, NOW, null)));
        assertEquals("Aviso no enviado: la aplicación se reinició antes de entregarlo.",
                EmGoalsViewModel.outcomeText(
                        observed.observed(EmGoalAlert.OUTCOME_DISCARDED, NOW, null)));
    }

    @Test
    void dailySummarySeparatesJustifiedAndExceededMinutesAndListsAlertOutcomes() {
        segments.add(observed("chess.exe", null,
                "2026-11-24T09:00:00Z", "2026-11-24T10:12:00Z"));
        long id = createChessGoal().page().goals().getFirst().id();
        alerts.add(EmGoalAlert.draft(id, NOW, 80, EmGoalMetric.MINUTES));
        EmGoalAlert full = alerts.add(EmGoalAlert.draft(id, NOW, 100, EmGoalMetric.MINUTES))
                .notified(NOW.plusSeconds(30), NOW.plusSeconds(150))
                .observed(EmGoalAlert.OUTCOME_NO_DRIFT, NOW.plusSeconds(150), null);
        alerts.update(full);

        EmGoalRow exceeded = viewModel.load().goals().getFirst();
        assertEquals("Justificado: 0 min · Excedido sin justificar: 12 min",
                exceeded.balanceText());
        assertEquals(2, exceeded.alertLines().size());
        assertEquals("Aviso 80 % de minutos · 12:00 — " + EmGoalsViewModel.PENDING_NOTICE_TEXT,
                exceeded.alertLines().get(0));
        assertEquals("Aviso 100 % de minutos · 12:00 — " + EmGoalsViewModel.NO_DRIFT_TEXT,
                exceeded.alertLines().get(1));

        viewModel.justify(id, "Torneo");
        EmGoalRow justified = viewModel.load().goals().getFirst();
        assertEquals("Justificado: 12 min · Excedido sin justificar: 0 min",
                justified.balanceText());
        assertEquals(2, justified.alertLines().size());
    }

    private static EmSegment observed(String process, String host, String start, String end) {
        return new EmSegment(0, EmObservationKind.OBSERVED, new EmContextKey(process, host),
                null, null, Instant.parse(start), Instant.parse(end));
    }

    private static final class InMemoryGoalRepository implements EmGoalRepository {
        private final Map<Long, EmGoal> goals = new LinkedHashMap<>();
        private final List<EmGoalRevision> revisions = new ArrayList<>();
        private long nextId = 1;

        private void put(EmGoal goal) {
            goals.put(goal.id(), goal);
        }

        private void addRevision(EmGoal goal, Instant recordedAt, boolean archived, boolean baseline) {
            revisions.add(new EmGoalRevision(goal, recordedAt, archived, baseline));
        }

        @Override
        public EmGoal create(EmGoal goal) {
            EmGoal stored = new EmGoal(nextId++, goal.context(), goal.dailyMinutes(),
                    goal.dailyEntries(), goal.passive(), goal.active(), NOW, NOW);
            goals.put(stored.id(), stored);
            addRevision(stored, NOW, false, false);
            return stored;
        }

        @Override
        public Optional<EmGoal> find(long id) {
            return Optional.ofNullable(goals.get(id));
        }

        @Override
        public void update(EmGoal goal) {
            goals.put(goal.id(), goal);
            addRevision(goal, NOW, false, false);
        }

        @Override
        public boolean delete(long id) {
            EmGoal removed = goals.remove(id);
            if (removed != null) addRevision(removed, NOW, true, false);
            return removed != null;
        }

        @Override
        public List<EmGoal> listActive() {
            return listAll().stream().filter(EmGoal::active).toList();
        }

        @Override
        public List<EmGoalRevision> revisionsBefore(Instant end) {
            return revisions.stream()
                    .filter(revision -> revision.recordedAt().isBefore(end))
                    .sorted(Comparator.comparing(EmGoalRevision::recordedAt))
                    .toList();
        }

        @Override
        public List<EmGoal> listAll() {
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

    private static final class InMemoryAlertRepository implements EmGoalAlertRepository {
        private final Map<Long, EmGoalAlert> alerts = new LinkedHashMap<>();
        private long nextId = 1;

        private EmGoalAlert add(EmGoalAlert alert) {
            EmGoalAlert stored = new EmGoalAlert(nextId++, alert.goalId(), alert.triggeredAt(),
                    alert.thresholdPercent(), alert.metric(), alert.outcome(),
                    alert.justification(), alert.justifiedAt(), alert.justifiedExcess(),
                    alert.notifiedAt(), alert.observedUntil(), alert.outcomeAt(),
                    alert.outcomeAfterMs());
            alerts.put(stored.id(), stored);
            return stored;
        }

        @Override
        public EmGoalAlert create(EmGoalAlert alert) {
            return add(alert);
        }

        @Override
        public Optional<EmGoalAlert> find(long id) {
            return Optional.ofNullable(alerts.get(id));
        }

        @Override
        public void update(EmGoalAlert alert) {
            alerts.put(alert.id(), alert);
        }

        @Override
        public void updateAll(List<EmGoalAlert> values) {
            for (EmGoalAlert alert : values) alerts.put(alert.id(), alert);
        }

        @Override
        public boolean delete(long id) {
            return alerts.remove(id) != null;
        }

        @Override
        public List<EmGoalAlert> listByGoal(long goalId) {
            return alerts.values().stream()
                    .filter(alert -> alert.goalId() == goalId)
                    .sorted(Comparator.comparing(EmGoalAlert::triggeredAt).thenComparingLong(EmGoalAlert::id))
                    .toList();
        }

        @Override
        public List<EmGoalAlert> listPending() {
            return alerts.values().stream()
                    .filter(alert -> alert.outcome() == null)
                    .sorted(Comparator.comparing(EmGoalAlert::triggeredAt).thenComparingLong(EmGoalAlert::id))
                    .toList();
        }

        @Override
        public List<EmGoalAlert> listBetween(Instant from, Instant to) {
            return alerts.values().stream()
                    .filter(alert -> !alert.triggeredAt().isBefore(from)
                            && alert.triggeredAt().isBefore(to))
                    .sorted(Comparator.comparing(EmGoalAlert::triggeredAt).thenComparingLong(EmGoalAlert::id))
                    .toList();
        }

        @Override
        public List<EmGoalAlert> listJustifiedBetween(Instant from, Instant to) {
            return alerts.values().stream()
                    .filter(EmGoalAlert::isJustified)
                    .filter(alert -> !alert.justifiedAt().isBefore(from)
                            && alert.justifiedAt().isBefore(to))
                    .sorted(Comparator.comparing(EmGoalAlert::justifiedAt).thenComparingLong(EmGoalAlert::id))
                    .toList();
        }
    }

    private static final class FakeSegments implements EmSegmentRepository {
        private final List<EmSegment> segments = new ArrayList<>();

        private void add(EmSegment segment) {
            segments.add(segment);
        }

        @Override
        public void saveAll(List<EmSegment> values) {
            segments.addAll(values);
        }

        @Override
        public List<EmSegment> between(Instant from, Instant to) {
            return segments.stream()
                    .filter(segment -> segment.start().isBefore(to) && from.isBefore(segment.end()))
                    .toList();
        }

        @Override
        public List<EmContextKey> observedContexts() {
            return segments.stream()
                    .filter(segment -> segment.kind() == EmObservationKind.OBSERVED)
                    .map(EmSegment::context)
                    .distinct()
                    .toList();
        }
    }
}
