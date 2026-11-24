package com.deriva.ui.viewmodel;

import com.deriva.app.usecase.GoalsService;
import com.deriva.goals.model.Goal;
import com.deriva.goals.model.GoalState;
import com.deriva.privacy.ObservationKind;
import com.deriva.storage.port.GoalRepository;
import com.deriva.storage.port.SegmentRepository;
import com.deriva.tracking.model.ContextKey;
import com.deriva.tracking.model.Segment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class GoalsViewModelTest {
    private static final Instant NOW = Instant.parse("2026-11-24T12:00:00Z");
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);
    private static final ZoneOffset ZONE = ZoneOffset.UTC;

    private InMemoryGoalRepository repository;
    private FakeSegments segments;
    private GoalsService service;
    private GoalsViewModel viewModel;

    @BeforeEach
    void setUp() {
        repository = new InMemoryGoalRepository();
        segments = new FakeSegments();
        service = new GoalsService(repository, segments, CLOCK);
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
