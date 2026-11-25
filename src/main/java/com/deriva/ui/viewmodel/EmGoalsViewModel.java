package com.deriva.ui.viewmodel;

import com.deriva.analysis.EmDayWindow;
import com.deriva.app.usecase.EmGoalsService;
import com.deriva.goals.EmOpenSegmentProvider;
import com.deriva.goals.model.EmGoal;
import com.deriva.goals.model.EmGoalAlert;
import com.deriva.goals.model.EmGoalMetric;
import com.deriva.goals.model.EmGoalProgress;
import com.deriva.goals.model.EmGoalRevision;
import com.deriva.goals.model.EmGoalState;
import com.deriva.tracking.model.EmContextKey;

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
public final class EmGoalsViewModel {
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
            "Los días pasados y los periodos son de solo lectura. Solo se conserva la configuración registrada "
                    + "desde que se añadió el historial; los objetivos borrados antes no se pueden recuperar.";
    public static final String RANGE_NOTE =
            "Cada día usa la configuración vigente ese día y solo los segmentos cerrados; "
                    + "un objetivo borrado deja de sumar en el momento del borrado.";
    public static final String HISTORY_EMPTY_TEXT =
            "No hay objetivos con historial en estas fechas. Los objetivos borrados antes de que "
                    + "existiera el historial no se pueden recuperar.";
    public static final String RANGE_ORDER_ERROR = "«Desde» no puede ser posterior a «Hasta».";
    public static final String RANGE_LENGTH_ERROR =
            "El periodo no puede superar " + EmGoalsService.MAX_RANGE_DAYS + " días.";

    public record Outcome(EmGoalsPage page, String message, boolean error) {
        public boolean saved() {
            return !error;
        }
    }

    private final EmGoalsService service;
    private final ZoneId zone;
    private final EmOpenSegmentProvider openSegments;
    private final DateTimeFormatter timeFormat;
    private final DateTimeFormatter dayFormat = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private List<EmGoal> goals = List.of();
    private List<EmGoalContextOption> contexts = List.of();
    private Map<Long, EmGoalProgress> progressByGoal = Map.of();
    private Map<Long, List<EmGoalAlert>> alertsByGoal = Map.of();
    private Map<Long, List<EmGoalAlert>> fullAlertsByGoal = Map.of();
    private long editingId;
    private String processText = "";
    private String hostText = "";
    private String minutesText = "";
    private String entriesText = "";
    private boolean passive;
    private boolean active = true;
    private String validationError;
    private LocalDate selectedDate;
    private LocalDate rangeTo;
    private boolean followToday = true;
    private boolean showArchived = true;
    private EmGoalsService.RangeHistory rangeHistory;
    private List<EmGoal> archivedToday = List.of();
    private String historyNote;
    private List<String> revisionLines = List.of();
    private Set<Long> archivedGoalIds = Set.of();

    public EmGoalsViewModel(EmGoalsService service, ZoneId zone) {
        this(service, zone, now -> Optional.empty());
    }

    public EmGoalsViewModel(EmGoalsService service, ZoneId zone, EmOpenSegmentProvider openSegments) {
        this.service = Objects.requireNonNull(service, "service");
        this.zone = Objects.requireNonNull(zone, "zone");
        this.openSegments = Objects.requireNonNull(openSegments, "openSegments");
        this.timeFormat = DateTimeFormatter.ofPattern("HH:mm").withZone(zone);
    }

    public EmGoalsPage load() {
        LocalDate today = service.today(zone);
        if (followToday || selectedDate == null || rangeTo == null) {
            selectedDate = today;
            rangeTo = today;
        }
        contexts = buildContexts(service.observedContexts());
        archivedToday = List.of();
        rangeHistory = null;
        if (isRange()) {
            rangeHistory = service.history(selectedDate, rangeTo, zone);
            goals = List.of();
            archivedGoalIds = Set.of();
            progressByGoal = Map.of();
            alertsByGoal = Map.of();
            fullAlertsByGoal = Map.of();
            EmDayWindow first = EmDayWindow.of(selectedDate, zone);
            EmDayWindow last = EmDayWindow.of(rangeTo, zone);
            revisionLines = revisionLines(rangeHistory.revisions(), first.start(), last.end(), true);
            historyNote = HISTORY_NOTE + " " + RANGE_NOTE;
            resetForm();
        } else if (isHistorical()) {
            EmGoalsService.DayHistory history = service.history(selectedDate, zone);
            goals = history.goals();
            archivedGoalIds = history.archived();
            progressByGoal = new LinkedHashMap<>();
            for (EmGoalProgress progress : history.progress())
                progressByGoal.put(progress.goal().id(), progress);
            loadAlerts(selectedDate);
            EmDayWindow window = EmDayWindow.of(selectedDate, zone);
            revisionLines = revisionLines(history.revisions(), window.start(), window.end(), false);
            historyNote = HISTORY_NOTE;
            resetForm();
        } else {
            goals = service.listAll();
            archivedGoalIds = Set.of();
            progressByGoal = new LinkedHashMap<>();
            for (EmGoalProgress progress : service.progressForDay(today, zone, openSegments))
                progressByGoal.put(progress.goal().id(), progress);
            // Goals deleted today stay visible read-only, frozen at deletion.
            EmGoalsService.DayHistory todayHistory = service.history(today, zone);
            List<EmGoal> deleted = new ArrayList<>();
            for (EmGoal goal : todayHistory.goals())
                if (todayHistory.archived().contains(goal.id())) deleted.add(goal);
            for (EmGoalProgress progress : todayHistory.progress())
                if (todayHistory.archived().contains(progress.goal().id()))
                    progressByGoal.put(progress.goal().id(), progress);
            archivedToday = List.copyOf(deleted);
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
        selectRange(day, day);
    }

    /**
     * Selects an inclusive local period. Returns an error text and keeps the previous
     * selection when the period is invalid; otherwise the controller reloads.
     */
    public String selectRange(LocalDate from, LocalDate to) {
        Objects.requireNonNull(from, "from");
        Objects.requireNonNull(to, "to");
        if (from.isAfter(to)) return RANGE_ORDER_ERROR;
        if (java.time.temporal.ChronoUnit.DAYS.between(from, to) + 1 > EmGoalsService.MAX_RANGE_DAYS)
            return RANGE_LENGTH_ERROR;
        LocalDate today = service.today(zone);
        selectedDate = from;
        rangeTo = to;
        followToday = from.equals(today) && to.equals(today);
        if (!followToday) resetForm();
        return null;
    }

    public EmGoalsPage load(LocalDate from, LocalDate to) {
        String error = selectRange(from, to);
        if (error != null) throw new IllegalArgumentException(error);
        return load();
    }

    /** Local today according to the service clock. */
    public LocalDate today() {
        return service.today(zone);
    }

    /** Shows or hides deleted goals; they are always read-only. */
    public EmGoalsPage setShowArchived(boolean show) {
        showArchived = show;
        return page();
    }

    public EmGoalsPage load(LocalDate day) {
        selectDate(day);
        return load();
    }

    private void loadAlerts(LocalDate day) {
        List<EmGoalAlert> dayAlerts = service.alertsForDay(day, zone);
        fullAlertsByGoal = dayAlerts.stream()
                .filter(alert -> alert.thresholdPercent() == 100)
                .collect(Collectors.groupingBy(EmGoalAlert::goalId, LinkedHashMap::new,
                        Collectors.toList()));
        alertsByGoal = dayAlerts.stream()
                .collect(Collectors.groupingBy(EmGoalAlert::goalId, LinkedHashMap::new,
                        Collectors.toList()));
    }

    public EmGoalsPage updateForm(String process, String host, String minutes,
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

    public EmGoalsPage selectContext(EmGoalContextOption option) {
        Objects.requireNonNull(option, "option");
        processText = option.context().processName();
        hostText = option.context().siteHost() == null ? "" : option.context().siteHost();
        validationError = null;
        return page();
    }

    public EmGoalsPage beginEdit(long id) {
        if (isHistorical()) return page();
        EmGoal goal = find(id);
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

    public EmGoalsPage cancelEdit() {
        resetForm();
        return page();
    }

    public Outcome save() {
        if (isHistorical()) return readOnly();
        EmGoal existing = editingId == 0 ? null : find(editingId);
        Result result;
        if (existing != null) {
            result = validateLimits();
            if (result.error() != null) {
                validationError = result.error();
                return new Outcome(page(), result.error(), true);
            }
            try {
                service.update(new EmGoal(existing.id(), existing.context(), result.minutes(),
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
            service.create(new EmGoal(0, result.context(), result.minutes(), result.entries(),
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
        EmGoal goal = find(id);
        if (goal == null) return new Outcome(page(), "El objetivo ya no existe.", true);
        if (!goal.active())
            return new Outcome(page(), "Activa el objetivo para justificar su exceso.", true);
        if (fullAlertsByGoal.getOrDefault(id, List.of()).isEmpty())
            return new Outcome(page(), "Este objetivo no tiene una alerta al 100 % hoy.", true);
        if (reason != null && reason.length() > EmGoalAlert.MAX_JUSTIFICATION_LENGTH)
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
        EmGoal goal = find(id);
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

    public EmGoalsPage page() {
        boolean historical = isHistorical();
        boolean range = isRange();
        List<EmGoalRow> rows = new ArrayList<>(goals.size());
        if (range) {
            rows.addAll(rangeRows());
        } else {
            for (EmGoal goal : goals) {
                boolean archived = archivedGoalIds.contains(goal.id());
                if (archived && !showArchived) continue;
                EmGoalProgress progress = progressByGoal.get(goal.id());
                rows.add(row(goal, progress, fullAlertsByGoal.getOrDefault(goal.id(), List.of()),
                        historical, archived));
            }
            if (!historical && showArchived) {
                for (EmGoal goal : archivedToday)
                    rows.add(row(goal, progressByGoal.get(goal.id()),
                            fullAlertsByGoal.getOrDefault(goal.id(), List.of()), true, true));
            }
        }
        String summary = range ? rangeSummary() : historical ? historySummary() : todaySummary();
        LocalDate day = selectedDate == null ? service.today(zone) : selectedDate;
        LocalDate to = rangeTo == null ? day : rangeTo;
        return new EmGoalsPage(rows, contexts, summary, !rows.isEmpty(),
                !historical && editingId > 0, !historical && editingId > 0,
                historical ? "Consulta histórica"
                        : editingId > 0 ? "Editar objetivo" : "Nuevo objetivo",
                processText, hostText, minutesText, entriesText, passive, active,
                validationError, canSave(), day, historical, historyNote, revisionLines,
                to, range, showArchived,
                range ? "Objetivos del periodo" : "Objetivos del día",
                range ? "Detalle por día" : historical ? "Resumen del día" : "Resumen de hoy");
    }

    private String todaySummary() {
        long activeCount = goals.stream().filter(EmGoal::active).count();
        String summary = goals.isEmpty()
                ? "Sin objetivos"
                : goals.size() + (goals.size() == 1 ? " objetivo · " : " objetivos · ")
                + activeCount + (activeCount == 1 ? " activo" : " activos");
        return withArchived(summary, archivedToday.size(), " hoy");
    }

    private String historySummary() {
        long archived = goals.stream().filter(goal -> archivedGoalIds.contains(goal.id())).count();
        String summary = "Historial de " + dayFormat.format(selectedDate) + " · "
                + goals.size() + (goals.size() == 1 ? " objetivo" : " objetivos");
        return withArchived(summary, archived, "");
    }

    private String rangeSummary() {
        long days = java.time.temporal.ChronoUnit.DAYS.between(selectedDate, rangeTo) + 1;
        int total = rangeHistory == null ? 0 : rangeHistory.days().size();
        long archived = rangeHistory == null ? 0 : rangeHistory.days().values().stream()
                .filter(list -> list.getLast().archivedThatDay()).count();
        String summary = dayFormat.format(selectedDate) + " – " + dayFormat.format(rangeTo)
                + " · " + days + " días · " + total + (total == 1 ? " objetivo" : " objetivos");
        return withArchived(summary, archived, "");
    }

    private String withArchived(String summary, long archived, String suffix) {
        if (archived == 0) return summary;
        return summary + " · " + archived + (archived == 1 ? " borrado" : " borrados") + suffix
                + (showArchived ? "" : (archived == 1 ? " oculto" : " ocultos"));
    }

    /** One read-only card per goal that existed in the period, with one line per day. */
    private List<EmGoalRow> rangeRows() {
        if (rangeHistory == null) return List.of();
        Map<Long, Map<LocalDate, List<EmGoalAlert>>> fullAlerts = new LinkedHashMap<>();
        for (EmGoalAlert alert : rangeHistory.alerts()) {
            if (alert.thresholdPercent() != 100) continue;
            fullAlerts.computeIfAbsent(alert.goalId(), ignored -> new LinkedHashMap<>())
                    .computeIfAbsent(alert.triggeredAt().atZone(zone).toLocalDate(),
                            ignored -> new ArrayList<>())
                    .add(alert);
        }
        List<EmGoalRow> rows = new ArrayList<>();
        for (Map.Entry<Long, List<EmGoalsService.GoalDay>> entry : rangeHistory.days().entrySet()) {
            List<EmGoalsService.GoalDay> days = entry.getValue();
            EmGoalsService.GoalDay last = days.getLast();
            boolean archived = last.archivedThatDay();
            if (archived && !showArchived) continue;
            EmGoal goal = last.goal();
            int counted = 0;
            int exceeded = 0;
            int unjustified = 0;
            long used = 0;
            long entries = 0;
            long excess = 0;
            long justified = 0;
            boolean anyMinutes = false;
            List<String> lines = new ArrayList<>(days.size());
            for (EmGoalsService.GoalDay day : days) {
                String date = dayFormat.format(day.day());
                String deleted = day.archivedThatDay() && day.archivedAt() != null
                        ? " · Borrado a las " + timeFormat.format(day.archivedAt()) : "";
                if (!day.goal().active() || day.progress() == null) {
                    lines.add(date + " · Desactivado" + deleted);
                    continue;
                }
                EmGoalProgress progress = day.progress();
                counted++;
                used += progress.usedMillis();
                entries += progress.entries();
                EmGoalState state = progress.state();
                if (state == EmGoalState.EXCEEDED || state == EmGoalState.JUSTIFIED) exceeded++;
                if (state == EmGoalState.EXCEEDED) unjustified++;
                if (day.goal().dailyMinutes() != null) {
                    anyMinutes = true;
                    long dayExcess = Math.max(0,
                            progress.usedMillis() - day.goal().dailyMinutes() * 60_000L);
                    excess += dayExcess;
                    justified += Math.min(dayExcess, progress.justifiedMillis());
                }
                String reason = fullAlerts.getOrDefault(goal.id(), Map.of())
                        .getOrDefault(day.day(), List.of()).stream()
                        .filter(EmGoalAlert::isJustified)
                        .map(EmGoalAlert::justification)
                        .filter(Objects::nonNull)
                        .filter(text -> !text.isBlank())
                        .findFirst()
                        .map(text -> " · Motivo: " + text)
                        .orElse("");
                lines.add(date + " · " + progressText(day.goal(), progress) + " · "
                        + stateLabel(state) + reason + deleted);
            }
            String label;
            String icon;
            String key;
            if (counted == 0) {
                label = "Desactivado";
                icon = "—";
                key = "inactive";
            } else if (exceeded == 0) {
                label = "Sin excesos";
                icon = stateIcon(EmGoalState.IN_PROGRESS);
                key = stateKey(EmGoalState.IN_PROGRESS);
            } else if (unjustified > 0) {
                label = "Tope superado " + exceeded + " de " + counted + (counted == 1 ? " día" : " días");
                icon = stateIcon(EmGoalState.EXCEEDED);
                key = stateKey(EmGoalState.EXCEEDED);
            } else {
                label = "Justificado " + exceeded + " de " + counted + (counted == 1 ? " día" : " días");
                icon = stateIcon(EmGoalState.JUSTIFIED);
                key = stateKey(EmGoalState.JUSTIFIED);
            }
            String progressText = "Total: " + minutesText(used) + " · " + entries
                    + (entries == 1 ? " entrada" : " entradas") + " · " + counted
                    + (counted == 1 ? " día medido" : " días medidos");
            String balance = anyMinutes
                    ? "Justificado: " + minutesText(justified)
                    + " · Excedido sin justificar: " + minutesText(excess - justified)
                    : null;
            String note = "Configuración al final del periodo: " + limitsText(goal)
                    + (archived && last.archivedAt() != null
                    ? " · Borrado el " + dayFormat.format(last.archivedAt().atZone(zone).toLocalDate())
                    + " · solo lectura"
                    : "");
            double ratio = counted == 0 ? 0 : exceeded / (double) counted;
            rows.add(new EmGoalRow(goal.id(), contextLabel(goal.context()), limitsText(goal),
                    progressText, null, label, icon, key, ratio, counted > 0, goal.passive(),
                    false, null, null, balance, lines, note, archived));
        }
        return rows;
    }

    private List<String> revisionLines(Map<Long, List<EmGoalRevision>> revisionsByGoal,
                                       java.time.Instant start, java.time.Instant end,
                                       boolean withDate) {
        List<EmGoalRevision> changes = new ArrayList<>();
        for (List<EmGoalRevision> revisions : revisionsByGoal.values())
            for (EmGoalRevision revision : revisions)
                if (!revision.recordedAt().isBefore(start) && revision.recordedAt().isBefore(end))
                    changes.add(revision);
        changes.sort(Comparator.comparing(EmGoalRevision::recordedAt));
        if (changes.isEmpty())
            return List.of(withDate ? "Sin cambios de configuración registrados en el periodo."
                    : "Sin cambios de configuración registrados ese día.");
        List<String> lines = new ArrayList<>(changes.size());
        for (EmGoalRevision revision : changes) {
            EmGoal goal = revision.goal();
            String kind = revision.baseline()
                    ? "Base registrada al actualizar Deriva"
                    : revision.archived() ? "Objetivo borrado" : "Configuración actualizada";
            String detail = revision.archived()
                    ? "; configuración conservada: " + limitsText(goal)
                    : ": " + limitsText(goal) + " · " + (goal.active() ? "Activo" : "Inactivo");
            String when = withDate
                    ? dayFormat.format(revision.recordedAt().atZone(zone).toLocalDate()) + " "
                    + timeFormat.format(revision.recordedAt())
                    : timeFormat.format(revision.recordedAt());
            lines.add(when + " · "
                    + contextLabel(goal.context()) + " · " + kind + detail);
        }
        return List.copyOf(lines);
    }

    private boolean isRange() {
        return selectedDate != null && rangeTo != null && !selectedDate.equals(rangeTo);
    }

    private boolean isHistorical() {
        return isRange() || (selectedDate != null && !selectedDate.equals(service.today(zone)));
    }

    private Outcome readOnly() {
        return new Outcome(page(), "Los días pasados y los periodos son de solo lectura.", true);
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
            EmContextKey context = new EmContextKey(processText, host);
            for (EmGoal goal : goals) {
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

    private EmGoalRow row(EmGoal goal, EmGoalProgress progress, List<EmGoalAlert> fullAlerts,
                        boolean historical, boolean archived) {
        String contextLabel = contextLabel(goal.context());
        String limits = limitsText(goal);
        String configurationNote = historical
                ? "Configuración usada para el resumen: " + limits
                + (archived ? " · Borrado ese día · solo lectura" : "")
                : null;
        if (!goal.active() || progress == null) {
            return new EmGoalRow(goal.id(), contextLabel, limits,
                    "Sin progreso mientras está desactivado.", null, "Desactivado", "—",
                    "inactive", 0, false, goal.passive(), false, null,
                    alertOutcomeText(goal.id()), null, alertLines(goal.id()), configurationNote,
                    archived);
        }
        EmGoalState state = progress.state();
        String label = stateLabel(state);
        String icon = stateIcon(state);
        String key = stateKey(state);
        boolean anyJustified = fullAlerts.stream().anyMatch(EmGoalAlert::isJustified);
        boolean anyUnanswered = fullAlerts.stream().anyMatch(alert -> !alert.isJustified());
        if (state == EmGoalState.EXCEEDED && anyUnanswered) {
            label = "Sin responder";
            icon = "○";
            key = "unanswered";
        }
        return new EmGoalRow(goal.id(), contextLabel, limits, progressText(goal, progress), state,
                label, icon, key, ratio(goal, progress, state), true, goal.passive(),
                progress.unmeasurable(),
                justificationView(goal, progress, fullAlerts, anyJustified, anyUnanswered),
                alertOutcomeText(goal.id()), balanceText(goal, progress), alertLines(goal.id()),
                configurationNote, archived);
    }

    /**
     * Separates the certified-by-the-user minutes from the minutes still over the limit.
     * The justification only covers the excess accumulated when it was marked.
     */
    private static String balanceText(EmGoal goal, EmGoalProgress progress) {
        if (goal.dailyMinutes() == null) return null;
        long limit = goal.dailyMinutes() * 60_000L;
        long excess = Math.max(0, progress.usedMillis() - limit);
        long justified = Math.min(excess, progress.justifiedMillis());
        long uncovered = excess - justified;
        return "Justificado: " + minutesText(justified)
                + " · Excedido sin justificar: " + minutesText(uncovered);
    }

    private static String minutesText(long millis) {
        return millis == 0 ? "0 min" : EmDisplayFormats.duration(millis);
    }

    /** One line per recorded alert, with its threshold, local time and later observation. */
    private List<String> alertLines(long goalId) {
        return alertsByGoal.getOrDefault(goalId, List.of()).stream()
                .sorted(Comparator.comparing(EmGoalAlert::triggeredAt))
                .map(this::alertLine)
                .toList();
    }

    private String alertLine(EmGoalAlert alert) {
        String metric = alert.metric() == EmGoalMetric.MINUTES ? "minutos" : "entradas";
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
        List<EmGoalAlert> alerts = alertsByGoal.getOrDefault(goalId, List.of());
        if (alerts.stream().anyMatch(EmGoalAlert::isPendingNotice)) return PENDING_NOTICE_TEXT;
        return alerts.stream()
                .filter(alert -> alert.outcome() != null)
                .max(Comparator.comparing(alert -> alert.outcomeAt() == null
                        ? alert.triggeredAt() : alert.outcomeAt()))
                .map(EmGoalsViewModel::outcomeText)
                .orElse(null);
    }

    /** Presentation text for a stored observation; it never claims a cause. */
    static String outcomeText(EmGoalAlert alert) {
        if (alert.outcome() == null) return null;
        return switch (alert.outcome()) {
            case EmGoalAlert.OUTCOME_STABLE -> "Después del aviso, volvió a estabilidad en "
                    + EmDisplayFormats.duration(alert.outcomeAfterMs() == null
                    ? 0L : alert.outcomeAfterMs()) + ".";
            case EmGoalAlert.OUTCOME_NO_DRIFT -> NO_DRIFT_TEXT;
            case EmGoalAlert.OUTCOME_NEW_DRIFT -> NEW_DRIFT_TEXT;
            case EmGoalAlert.OUTCOME_STILL_DRIFTING -> STILL_DRIFTING_TEXT;
            case EmGoalAlert.OUTCOME_NOT_OBSERVED -> NOT_OBSERVED_TEXT;
            case EmGoalAlert.OUTCOME_DISCARDED -> DISCARDED_TEXT;
            default -> null;
        };
    }

    private static EmGoalJustificationView justificationView(
            EmGoal goal, EmGoalProgress progress, List<EmGoalAlert> fullAlerts,
            boolean anyJustified, boolean anyUnanswered) {
        if (fullAlerts.isEmpty()) return null;

        String reason = null;
        String coverage = null;
        if (anyJustified) {
            reason = fullAlerts.stream()
                    .filter(EmGoalAlert::isJustified)
                    .map(EmGoalAlert::justification)
                    .filter(Objects::nonNull)
                    .filter(text -> !text.isBlank())
                    .findFirst()
                    .orElse("Sin motivo");
            coverage = coverageText(fullAlerts);
        }
        String note = null;
        if (progress.state() == EmGoalState.EXCEEDED && anyUnanswered)
            note = UNANSWERED_NOTE;
        else if (progress.state() == EmGoalState.EXCEEDED && anyJustified)
            note = EXHAUSTED_NOTE;

        boolean canJustify = goal.active() && progress.state() == EmGoalState.EXCEEDED;
        String actionLabel = canJustify
                ? (anyJustified ? "Justificar de nuevo" : "Justificar exceso")
                : null;
        return new EmGoalJustificationView(reason, coverage, note, canJustify, actionLabel,
                anyJustified);
    }

    private static String coverageText(List<EmGoalAlert> fullAlerts) {
        long millis = fullAlerts.stream()
                .filter(EmGoalAlert::isJustified)
                .filter(alert -> alert.metric() == EmGoalMetric.MINUTES)
                .mapToLong(EmGoalAlert::justifiedExcess)
                .sum();
        long entries = fullAlerts.stream()
                .filter(EmGoalAlert::isJustified)
                .filter(alert -> alert.metric() == EmGoalMetric.ENTRIES)
                .mapToLong(EmGoalAlert::justifiedExcess)
                .sum();
        List<String> parts = new ArrayList<>(2);
        if (millis > 0) parts.add(EmDisplayFormats.duration(millis));
        if (entries > 0) parts.add(entries + (entries == 1 ? " entrada" : " entradas"));
        if (parts.isEmpty()) return "Sin exceso acumulado al marcar.";
        return "Cubierto: " + String.join(" · ", parts);
    }

    private static String limitsText(EmGoal goal) {
        List<String> parts = new ArrayList<>();
        if (goal.dailyMinutes() != null) parts.add(goal.dailyMinutes() + " min");
        if (goal.dailyEntries() != null) parts.add(goal.dailyEntries() + " entradas");
        if (goal.passive()) parts.add("Pasivo");
        return String.join(" · ", parts);
    }

    private static String progressText(EmGoal goal, EmGoalProgress progress) {
        List<String> parts = new ArrayList<>();
        if (goal.dailyMinutes() != null)
            parts.add(progress.usedMinutes() + " de " + goal.dailyMinutes() + " min");
        if (goal.dailyEntries() != null)
            parts.add(progress.entries() + " de " + goal.dailyEntries() + " entradas");
        return String.join(" · ", parts);
    }

    private static double ratio(EmGoal goal, EmGoalProgress progress, EmGoalState state) {
        double ratio = 0;
        if (goal.dailyMinutes() != null)
            ratio = Math.max(ratio, progress.usedMillis()
                    / (double) (goal.dailyMinutes() * 60_000L));
        if (goal.dailyEntries() != null)
            ratio = Math.max(ratio, progress.entries() / (double) goal.dailyEntries());
        if (state == EmGoalState.EXCEEDED || state == EmGoalState.JUSTIFIED) return 1;
        return Math.min(1, ratio);
    }

    public static String stateLabel(EmGoalState state) {
        return switch (state) {
            case IN_PROGRESS -> "En curso";
            case NEAR -> "Cerca del tope";
            case JUSTIFIED -> "Justificado por ti";
            case EXCEEDED -> "Excedido";
        };
    }

    public static String stateIcon(EmGoalState state) {
        return switch (state) {
            case IN_PROGRESS -> "▲";
            case NEAR -> "◆";
            case JUSTIFIED -> "✓";
            case EXCEEDED -> "●";
        };
    }

    public static String stateKey(EmGoalState state) {
        return switch (state) {
            case IN_PROGRESS -> "in-progress";
            case NEAR -> "near";
            case JUSTIFIED -> "justified";
            case EXCEEDED -> "exceeded";
        };
    }

    public static String contextLabel(EmContextKey context) {
        if (context.siteHost() == null) return EmDisplayFormats.app(context.processName());
        return EmDisplayFormats.site(context.siteHost()) + " · "
                + EmDisplayFormats.app(context.processName());
    }

    private static List<EmGoalContextOption> buildContexts(List<EmContextKey> observed) {
        Map<EmContextKey, EmGoalContextOption> options = new LinkedHashMap<>();
        for (EmContextKey context : observed) {
            options.putIfAbsent(context,
                    new EmGoalContextOption(context, contextLabel(context)));
        }
        return List.copyOf(options.values());
    }

    private EmGoal find(long id) {
        for (EmGoal goal : goals) if (goal.id() == id) return goal;
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

    private record Result(EmContextKey context, Integer minutes, Integer entries, String error) {}
}
