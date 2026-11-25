package com.deriva.ui.viewmodel;

import com.deriva.analysis.DayWindow;
import com.deriva.app.usecase.GoalsService;
import com.deriva.goals.OpenSegmentProvider;
import com.deriva.goals.model.Goal;
import com.deriva.goals.model.GoalAlert;
import com.deriva.goals.model.GoalMetric;
import com.deriva.goals.model.GoalProgress;
import com.deriva.goals.model.GoalRevision;
import com.deriva.goals.model.GoalState;
import com.deriva.tracking.model.ContextKey;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Pure presentation and validation model for daily goals. It never touches JavaFX;
 * the controller supplies plain strings and runs the blocking calls off the FX thread.
 */
public final class GoalsViewModel {
    /** Explains that the visible state is only the user's note, not a verification. */
    public static final String UNANSWERED_NOTE =
            "Sin responder es solo una anotación tuya; no cambia los registros de actividad.";
    /** Shown when later use has outgrown the stored justification. */
    public static final String EXHAUSTED_NOTE =
            "El uso posterior supera el exceso justificado; vuelve a contar.";
    public static final String PENDING_NOTICE_TEXT =
            "Aviso diferido: se enviará cuando termine la deriva o al llegar al máximo de espera.";
    public static final String NO_DRIFT_TEXT =
            "Al enviar el aviso no había deriva y no apareció ninguna en los 2 min siguientes.";
    public static final String NEW_DRIFT_TEXT =
            "Después del aviso apareció una deriva durante la observación de 2 min.";
    public static final String STILL_DRIFTING_TEXT =
            "Después del aviso, seguía habiendo deriva al terminar la observación de 2 min.";
    public static final String NOT_OBSERVED_TEXT =
            "Después del aviso no se pudo observar el resultado: la captura se pausó o la aplicación se cerró.";
    public static final String DISCARDED_TEXT =
            "Aviso no enviado: la aplicación se reinició antes de entregarlo.";
    public static final String OBSERVING_TEXT =
            "Aviso enviado; resultado posterior pendiente de la observación de 2 min.";
    public static final String NO_OUTCOME_TEXT = "Aviso registrado sin resultado posterior.";
    public static final String HISTORY_NOTE =
            "Los días pasados son de solo lectura. Solo se conserva la configuración registrada "
                    + "desde que se añadió el historial; los objetivos borrados antes no se pueden recuperar.";
    public static final String HISTORY_EMPTY_TEXT =
            "No hay objetivos con historial para este día. Los objetivos borrados antes de que "
                    + "existiera el historial no se pueden recuperar.";

    public record Outcome(GoalsPage page, String message, boolean error) {
        public boolean saved() {
            return !error;
        }
    }

    private final GoalsService service;
    private final ZoneId zone;
    private final OpenSegmentProvider openSegments;
    private final DateTimeFormatter timeFormat;
    private final DateTimeFormatter dayFormat = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private List<Goal> goals = List.of();
    private List<GoalContextOption> contexts = List.of();
    private Map<Long, GoalProgress> progressByGoal = Map.of();
    private Map<Long, List<GoalAlert>> alertsByGoal = Map.of();
    private Map<Long, List<GoalAlert>> fullAlertsByGoal = Map.of();
    private long editingId;
    private String processText = "";
    private String hostText = "";
    private String minutesText = "";
    private String entriesText = "";
    private boolean passive;
    private boolean active = true;
    private String validationError;
    private LocalDate selectedDate;
    private boolean followToday = true;
    private String historyNote;
    private List<String> revisionLines = List.of();
    private Set<Long> archivedGoalIds = Set.of();

    public GoalsViewModel(GoalsService service, ZoneId zone) {
        this(service, zone, now -> Optional.empty());
    }

    public GoalsViewModel(GoalsService service, ZoneId zone, OpenSegmentProvider openSegments) {
        this.service = Objects.requireNonNull(service, "service");
        this.zone = Objects.requireNonNull(zone, "zone");
        this.openSegments = Objects.requireNonNull(openSegments, "openSegments");
        this.timeFormat = DateTimeFormatter.ofPattern("HH:mm").withZone(zone);
    }

    public GoalsPage load() {
        LocalDate today = service.today(zone);
        if (followToday || selectedDate == null) selectedDate = today;
        contexts = buildContexts(service.observedContexts());
        if (isHistorical()) {
            GoalsService.DayHistory history = service.history(selectedDate, zone);
            goals = history.goals();
            archivedGoalIds = history.archived();
            progressByGoal = new LinkedHashMap<>();
            for (GoalProgress progress : history.progress())
                progressByGoal.put(progress.goal().id(), progress);
            loadAlerts(selectedDate);
            revisionLines = revisionLines(history);
            historyNote = HISTORY_NOTE;
            resetForm();
        } else {
            goals = service.listAll();
            archivedGoalIds = Set.of();
            progressByGoal = new LinkedHashMap<>();
            for (GoalProgress progress : service.progressForDay(today, zone, openSegments))
                progressByGoal.put(progress.goal().id(), progress);
            loadAlerts(today);
            revisionLines = List.of();
            historyNote = null;
            if (editingId > 0 && find(editingId) == null) resetForm();
        }
        return page();
    }

    /** Selects a local day; the controller reloads asynchronously afterwards. */
    public void selectDate(LocalDate day) {
        Objects.requireNonNull(day, "day");
        selectedDate = day;
        followToday = day.equals(service.today(zone));
        if (!followToday) resetForm();
    }

    public GoalsPage load(LocalDate day) {
        selectDate(day);
        return load();
    }

    private void loadAlerts(LocalDate day) {
        List<GoalAlert> dayAlerts = service.alertsForDay(day, zone);
        fullAlertsByGoal = dayAlerts.stream()
                .filter(alert -> alert.thresholdPercent() == 100)
                .collect(Collectors.groupingBy(GoalAlert::goalId, LinkedHashMap::new,
                        Collectors.toList()));
        alertsByGoal = dayAlerts.stream()
                .collect(Collectors.groupingBy(GoalAlert::goalId, LinkedHashMap::new,
                        Collectors.toList()));
    }

    public GoalsPage updateForm(String process, String host, String minutes,
                                String entries, boolean passiveValue, boolean activeValue) {
        processText = process == null ? "" : process;
        hostText = host == null ? "" : host;
        minutesText = minutes == null ? "" : minutes;
        entriesText = entries == null ? "" : entries;
        passive = passiveValue;
        active = activeValue;
        validationError = null;
        return page();
    }

    public GoalsPage selectContext(GoalContextOption option) {
        Objects.requireNonNull(option, "option");
        processText = option.context().processName();
        hostText = option.context().siteHost() == null ? "" : option.context().siteHost();
        validationError = null;
        return page();
    }

    public GoalsPage beginEdit(long id) {
        if (isHistorical()) return page();
        Goal goal = find(id);
        if (goal == null) return page();
        editingId = goal.id();
        processText = goal.context().processName();
        hostText = goal.context().siteHost() == null ? "" : goal.context().siteHost();
        minutesText = goal.dailyMinutes() == null ? "" : Integer.toString(goal.dailyMinutes());
        entriesText = goal.dailyEntries() == null ? "" : Integer.toString(goal.dailyEntries());
        passive = goal.passive();
        active = goal.active();
        validationError = null;
        return page();
    }

    public GoalsPage cancelEdit() {
        resetForm();
        return page();
    }

    public Outcome save() {
        if (isHistorical()) return readOnly();
        Goal existing = editingId == 0 ? null : find(editingId);
        Result result;
        if (existing != null) {
            result = validateLimits();
            if (result.error() != null) {
                validationError = result.error();
                return new Outcome(page(), result.error(), true);
            }
            try {
                service.update(new Goal(existing.id(), existing.context(), result.minutes(),
                        result.entries(), passive, active, existing.createdAt(), existing.updatedAt()));
                String message = "Objetivo actualizado.";
                resetForm();
                return new Outcome(load(), message, false);
            } catch (RuntimeException e) {
                validationError = "No se pudo guardar el objetivo. " + message(e);
                return new Outcome(page(), validationError, true);
            }
        }

        result = validateNewGoal();
        if (result.error() != null) {
            validationError = result.error();
            return new Outcome(page(), result.error(), true);
        }
        try {
            service.create(new Goal(0, result.context(), result.minutes(), result.entries(),
                    passive, active, null, null));
            String message = "Objetivo creado.";
            resetForm();
            return new Outcome(load(), message, false);
        } catch (RuntimeException e) {
            validationError = "No se pudo guardar el objetivo. " + message(e);
            return new Outcome(page(), validationError, true);
        }
    }

    /**
     * Marks the excess of the 100 % alert of today. The reason is optional and is kept
     * exactly as supplied; an empty text stores no reason.
     */
    public Outcome justify(long id, String reason) {
        if (isHistorical()) return readOnly();
        Goal goal = find(id);
        if (goal == null) return new Outcome(page(), "El objetivo ya no existe.", true);
        if (!goal.active())
            return new Outcome(page(), "Activa el objetivo para justificar su exceso.", true);
        if (fullAlertsByGoal.getOrDefault(id, List.of()).isEmpty())
            return new Outcome(page(), "Este objetivo no tiene una alerta al 100 % hoy.", true);
        if (reason != null && reason.length() > GoalAlert.MAX_JUSTIFICATION_LENGTH)
            return new Outcome(page(), "El motivo no puede superar los 500 caracteres.", true);
        String clean = reason != null && reason.isBlank() ? null : reason;
        try {
            service.justify(id, clean, zone, openSegments);
            return new Outcome(load(), "Justificación guardada.", false);
        } catch (RuntimeException e) {
            return new Outcome(page(), "No se pudo guardar la justificación. " + message(e), true);
        }
    }

    /** Removes the user justification; the recorded 100 % alert is kept. */
    public Outcome removeJustification(long id) {
        if (isHistorical()) return readOnly();
        if (find(id) == null) return new Outcome(page(), "El objetivo ya no existe.", true);
        try {
            service.removeJustification(id, zone);
            return new Outcome(load(), "Justificación quitada.", false);
        } catch (RuntimeException e) {
            return new Outcome(page(), "No se pudo quitar la justificación. " + message(e), true);
        }
    }

    public Outcome toggleActive(long id) {
        if (isHistorical()) return readOnly();
        Goal goal = find(id);
        if (goal == null) return new Outcome(page(), "El objetivo ya no existe.", true);
        try {
            boolean next = !goal.active();
            service.setActive(id, next);
            if (editingId == id) resetForm();
            return new Outcome(load(), next ? "Objetivo activado." : "Objetivo desactivado.", false);
        } catch (RuntimeException e) {
            return new Outcome(page(), "No se pudo cambiar el objetivo. " + message(e), true);
        }
    }

    public Outcome delete(long id) {
        if (isHistorical()) return readOnly();
        try {
            boolean deleted = service.delete(id);
            if (editingId == id) resetForm();
            return new Outcome(load(), deleted ? "Objetivo borrado." : "El objetivo ya no existe.",
                    !deleted);
        } catch (RuntimeException e) {
            return new Outcome(page(), "No se pudo borrar el objetivo. " + message(e), true);
        }
    }

    public GoalsPage page() {
        boolean historical = isHistorical();
        List<GoalRow> rows = new ArrayList<>(goals.size());
        for (Goal goal : goals) {
            GoalProgress progress = progressByGoal.get(goal.id());
            rows.add(row(goal, progress, fullAlertsByGoal.getOrDefault(goal.id(), List.of()),
                    historical, archivedGoalIds.contains(goal.id())));
        }
        String summary = historical ? historySummary() : todaySummary();
        LocalDate day = selectedDate == null ? service.today(zone) : selectedDate;
        return new GoalsPage(rows, contexts, summary, !goals.isEmpty(),
                !historical && editingId > 0, !historical && editingId > 0,
                historical ? "Consulta histórica"
                        : editingId > 0 ? "Editar objetivo" : "Nuevo objetivo",
                processText, hostText, minutesText, entriesText, passive, active,
                validationError, canSave(), day, historical, historyNote, revisionLines);
    }

    private String todaySummary() {
        long activeCount = goals.stream().filter(Goal::active).count();
        return goals.isEmpty()
                ? "Sin objetivos"
                : goals.size() + (goals.size() == 1 ? " objetivo · " : " objetivos · ")
                + activeCount + (activeCount == 1 ? " activo" : " activos");
    }

    private String historySummary() {
        long archived = goals.stream().filter(goal -> archivedGoalIds.contains(goal.id())).count();
        String summary = "Historial de " + dayFormat.format(selectedDate) + " · "
                + goals.size() + (goals.size() == 1 ? " objetivo" : " objetivos");
        return archived == 0 ? summary
                : summary + " · " + archived + (archived == 1 ? " borrado" : " borrados");
    }

    private List<String> revisionLines(GoalsService.DayHistory history) {
        DayWindow window = DayWindow.of(selectedDate, zone);
        List<GoalRevision> changes = new ArrayList<>();
        for (List<GoalRevision> revisions : history.revisions().values())
            for (GoalRevision revision : revisions)
                if (!revision.recordedAt().isBefore(window.start())
                        && revision.recordedAt().isBefore(window.end()))
                    changes.add(revision);
        changes.sort(Comparator.comparing(GoalRevision::recordedAt));
        if (changes.isEmpty())
            return List.of("Sin cambios de configuración registrados ese día.");
        List<String> lines = new ArrayList<>(changes.size());
        for (GoalRevision revision : changes) {
            Goal goal = revision.goal();
            String kind = revision.baseline()
                    ? "Base registrada al actualizar Deriva"
                    : revision.archived() ? "Objetivo borrado" : "Configuración actualizada";
            String detail = revision.archived()
                    ? "; configuración conservada: " + limitsText(goal)
                    : ": " + limitsText(goal) + " · " + (goal.active() ? "Activo" : "Inactivo");
            lines.add(timeFormat.format(revision.recordedAt()) + " · "
                    + contextLabel(goal.context()) + " · " + kind + detail);
        }
        return List.copyOf(lines);
    }

    private boolean isHistorical() {
        return selectedDate != null && !selectedDate.equals(service.today(zone));
    }

    private Outcome readOnly() {
        return new Outcome(page(), "Los días pasados son de solo lectura.", true);
    }

    private boolean canSave() {
        return !isHistorical() && !processText.isBlank()
                && (!minutesText.isBlank() || !entriesText.isBlank());
    }

    private Result validateNewGoal() {
        Result limits = validateLimits();
        if (limits.error() != null) return limits;

        if (processText == null || processText.isBlank())
            return new Result(null, null, null, "Escribe el proceso, por ejemplo code.exe.");
        if (processText.indexOf('/') >= 0 || processText.indexOf('\\') >= 0)
            return new Result(null, null, null,
                    "El proceso debe ser solo el nombre del ejecutable, sin / ni \\.");
        String host = hostText == null || hostText.isBlank() ? null : hostText.trim();
        if (host != null && !validHostText(host))
            return new Result(null, null, null,
                    "El host debe ser un dominio, sin dirección completa, ruta, consulta, puerto ni espacios.");
        try {
            ContextKey context = new ContextKey(processText, host);
            for (Goal goal : goals) {
                if (goal.context().equals(context))
                    return new Result(null, null, null,
                            "Ya existe un objetivo para ese contexto. Edítalo o actívalo.");
            }
            return new Result(context, limits.minutes(), limits.entries(), null);
        } catch (IllegalArgumentException e) {
            return new Result(null, null, null,
                    "El proceso o el host no son válidos. Revisa el nombre del ejecutable y el dominio.");
        }
    }

    private Result validateLimits() {
        Integer minutes;
        try {
            minutes = parse(minutesText);
        } catch (NumberFormatException e) {
            return new Result(null, null, null, "Usa números enteros en los topes.");
        }
        Integer entries;
        try {
            entries = parse(entriesText);
        } catch (NumberFormatException e) {
            return new Result(null, null, null, "Usa números enteros en los topes.");
        }
        if (minutes == null && entries == null)
            return new Result(null, null, null,
                    "Indica al menos un tope: minutos, entradas o ambos.");
        if ((minutes != null && minutes <= 0) || (entries != null && entries <= 0))
            return new Result(null, null, null, "Los topes deben ser mayores que cero.");
        return new Result(null, minutes, entries, null);
    }

    private static Integer parse(String text) {
        if (text == null || text.isBlank()) return null;
        return Integer.valueOf(text.trim());
    }

    private static boolean validHostText(String host) {
        if (host.indexOf("://") >= 0) return false;
        for (char forbidden : new char[]{'/', '\\', '?', '#', ':', ' '}) {
            if (host.indexOf(forbidden) >= 0) return false;
        }
        return !host.chars().anyMatch(Character::isWhitespace);
    }

    private GoalRow row(Goal goal, GoalProgress progress, List<GoalAlert> fullAlerts,
                        boolean historical, boolean archived) {
        String contextLabel = contextLabel(goal.context());
        String limits = limitsText(goal);
        String configurationNote = historical
                ? "Configuración usada para el resumen: " + limits
                + (archived ? " · Borrado ese día" : "")
                : null;
        if (!goal.active() || progress == null) {
            return new GoalRow(goal.id(), contextLabel, limits,
                    "Sin progreso mientras está desactivado.", null, "Desactivado", "—",
                    "inactive", 0, false, goal.passive(), false, null,
                    alertOutcomeText(goal.id()), null, alertLines(goal.id()), configurationNote);
        }
        GoalState state = progress.state();
        String label = stateLabel(state);
        String icon = stateIcon(state);
        String key = stateKey(state);
        boolean anyJustified = fullAlerts.stream().anyMatch(GoalAlert::isJustified);
        boolean anyUnanswered = fullAlerts.stream().anyMatch(alert -> !alert.isJustified());
        if (state == GoalState.EXCEEDED && anyUnanswered) {
            label = "Sin responder";
            icon = "○";
            key = "unanswered";
        }
        return new GoalRow(goal.id(), contextLabel, limits, progressText(goal, progress), state,
                label, icon, key, ratio(goal, progress, state), true, goal.passive(),
                progress.unmeasurable(),
                justificationView(goal, progress, fullAlerts, anyJustified, anyUnanswered),
                alertOutcomeText(goal.id()), balanceText(goal, progress), alertLines(goal.id()),
                configurationNote);
    }

    /**
     * Separates the certified-by-the-user minutes from the minutes still over the limit.
     * The justification only covers the excess accumulated when it was marked.
     */
    private static String balanceText(Goal goal, GoalProgress progress) {
        if (goal.dailyMinutes() == null) return null;
        long limit = goal.dailyMinutes() * 60_000L;
        long excess = Math.max(0, progress.usedMillis() - limit);
        long justified = Math.min(excess, progress.justifiedMillis());
        long uncovered = excess - justified;
        return "Justificado: " + minutesText(justified)
                + " · Excedido sin justificar: " + minutesText(uncovered);
    }

    private static String minutesText(long millis) {
        return millis == 0 ? "0 min" : DisplayFormats.duration(millis);
    }

    /** One line per recorded alert, with its threshold, local time and later observation. */
    private List<String> alertLines(long goalId) {
        return alertsByGoal.getOrDefault(goalId, List.of()).stream()
                .sorted(Comparator.comparing(GoalAlert::triggeredAt))
                .map(this::alertLine)
                .toList();
    }

    private String alertLine(GoalAlert alert) {
        String metric = alert.metric() == GoalMetric.MINUTES ? "minutos" : "entradas";
        String result;
        if (alert.isPendingNotice()) {
            result = PENDING_NOTICE_TEXT;
        } else if (alert.outcome() != null) {
            result = outcomeText(alert);
        } else if (alert.isObserving()) {
            result = OBSERVING_TEXT;
        } else {
            result = NO_OUTCOME_TEXT;
        }
        return "Aviso " + alert.thresholdPercent() + " % de " + metric + " · "
                + timeFormat.format(alert.triggeredAt()) + " — " + result;
    }

    private String alertOutcomeText(long goalId) {
        List<GoalAlert> alerts = alertsByGoal.getOrDefault(goalId, List.of());
        if (alerts.stream().anyMatch(GoalAlert::isPendingNotice)) return PENDING_NOTICE_TEXT;
        return alerts.stream()
                .filter(alert -> alert.outcome() != null)
                .max(Comparator.comparing(alert -> alert.outcomeAt() == null
                        ? alert.triggeredAt() : alert.outcomeAt()))
                .map(GoalsViewModel::outcomeText)
                .orElse(null);
    }

    /** Presentation text for a stored observation; it never claims a cause. */
    static String outcomeText(GoalAlert alert) {
        if (alert.outcome() == null) return null;
        return switch (alert.outcome()) {
            case GoalAlert.OUTCOME_STABLE -> "Después del aviso, volvió a estabilidad en "
                    + DisplayFormats.duration(alert.outcomeAfterMs() == null
                    ? 0L : alert.outcomeAfterMs()) + ".";
            case GoalAlert.OUTCOME_NO_DRIFT -> NO_DRIFT_TEXT;
            case GoalAlert.OUTCOME_NEW_DRIFT -> NEW_DRIFT_TEXT;
            case GoalAlert.OUTCOME_STILL_DRIFTING -> STILL_DRIFTING_TEXT;
            case GoalAlert.OUTCOME_NOT_OBSERVED -> NOT_OBSERVED_TEXT;
            case GoalAlert.OUTCOME_DISCARDED -> DISCARDED_TEXT;
            default -> null;
        };
    }

    private static GoalJustificationView justificationView(
            Goal goal, GoalProgress progress, List<GoalAlert> fullAlerts,
            boolean anyJustified, boolean anyUnanswered) {
        if (fullAlerts.isEmpty()) return null;

        String reason = null;
        String coverage = null;
        if (anyJustified) {
            reason = fullAlerts.stream()
                    .filter(GoalAlert::isJustified)
                    .map(GoalAlert::justification)
                    .filter(Objects::nonNull)
                    .filter(text -> !text.isBlank())
                    .findFirst()
                    .orElse("Sin motivo");
            coverage = coverageText(fullAlerts);
        }
        String note = null;
        if (progress.state() == GoalState.EXCEEDED && anyUnanswered)
            note = UNANSWERED_NOTE;
        else if (progress.state() == GoalState.EXCEEDED && anyJustified)
            note = EXHAUSTED_NOTE;

        boolean canJustify = goal.active() && progress.state() == GoalState.EXCEEDED;
        String actionLabel = canJustify
                ? (anyJustified ? "Justificar de nuevo" : "Justificar exceso")
                : null;
        return new GoalJustificationView(reason, coverage, note, canJustify, actionLabel,
                anyJustified);
    }

    private static String coverageText(List<GoalAlert> fullAlerts) {
        long millis = fullAlerts.stream()
                .filter(GoalAlert::isJustified)
                .filter(alert -> alert.metric() == GoalMetric.MINUTES)
                .mapToLong(GoalAlert::justifiedExcess)
                .sum();
        long entries = fullAlerts.stream()
                .filter(GoalAlert::isJustified)
                .filter(alert -> alert.metric() == GoalMetric.ENTRIES)
                .mapToLong(GoalAlert::justifiedExcess)
                .sum();
        List<String> parts = new ArrayList<>(2);
        if (millis > 0) parts.add(DisplayFormats.duration(millis));
        if (entries > 0) parts.add(entries + (entries == 1 ? " entrada" : " entradas"));
        if (parts.isEmpty()) return "Sin exceso acumulado al marcar.";
        return "Cubierto: " + String.join(" · ", parts);
    }

    private static String limitsText(Goal goal) {
        List<String> parts = new ArrayList<>();
        if (goal.dailyMinutes() != null) parts.add(goal.dailyMinutes() + " min");
        if (goal.dailyEntries() != null) parts.add(goal.dailyEntries() + " entradas");
        if (goal.passive()) parts.add("Pasivo");
        return String.join(" · ", parts);
    }

    private static String progressText(Goal goal, GoalProgress progress) {
        List<String> parts = new ArrayList<>();
        if (goal.dailyMinutes() != null)
            parts.add(progress.usedMinutes() + " de " + goal.dailyMinutes() + " min");
        if (goal.dailyEntries() != null)
            parts.add(progress.entries() + " de " + goal.dailyEntries() + " entradas");
        return String.join(" · ", parts);
    }

    private static double ratio(Goal goal, GoalProgress progress, GoalState state) {
        double ratio = 0;
        if (goal.dailyMinutes() != null)
            ratio = Math.max(ratio, progress.usedMillis()
                    / (double) (goal.dailyMinutes() * 60_000L));
        if (goal.dailyEntries() != null)
            ratio = Math.max(ratio, progress.entries() / (double) goal.dailyEntries());
        if (state == GoalState.EXCEEDED || state == GoalState.JUSTIFIED) return 1;
        return Math.min(1, ratio);
    }

    public static String stateLabel(GoalState state) {
        return switch (state) {
            case IN_PROGRESS -> "En curso";
            case NEAR -> "Cerca del tope";
            case JUSTIFIED -> "Justificado por ti";
            case EXCEEDED -> "Excedido";
        };
    }

    public static String stateIcon(GoalState state) {
        return switch (state) {
            case IN_PROGRESS -> "▲";
            case NEAR -> "◆";
            case JUSTIFIED -> "✓";
            case EXCEEDED -> "●";
        };
    }

    public static String stateKey(GoalState state) {
        return switch (state) {
            case IN_PROGRESS -> "in-progress";
            case NEAR -> "near";
            case JUSTIFIED -> "justified";
            case EXCEEDED -> "exceeded";
        };
    }

    public static String contextLabel(ContextKey context) {
        if (context.siteHost() == null) return DisplayFormats.app(context.processName());
        return DisplayFormats.site(context.siteHost()) + " · "
                + DisplayFormats.app(context.processName());
    }

    private static List<GoalContextOption> buildContexts(List<ContextKey> observed) {
        Map<ContextKey, GoalContextOption> options = new LinkedHashMap<>();
        for (ContextKey context : observed) {
            options.putIfAbsent(context,
                    new GoalContextOption(context, contextLabel(context)));
        }
        return List.copyOf(options.values());
    }

    private Goal find(long id) {
        for (Goal goal : goals) if (goal.id() == id) return goal;
        return null;
    }

    private void resetForm() {
        editingId = 0;
        processText = "";
        hostText = "";
        minutesText = "";
        entriesText = "";
        passive = false;
        active = true;
        validationError = null;
    }

    private static String message(Throwable error) {
        Throwable cause = error;
        while (cause.getCause() != null) cause = cause.getCause();
        return cause.getMessage() == null ? cause.getClass().getSimpleName() : cause.getMessage();
    }

    private record Result(ContextKey context, Integer minutes, Integer entries, String error) {}
}
