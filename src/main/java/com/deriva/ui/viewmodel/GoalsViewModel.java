package com.deriva.ui.viewmodel;

import com.deriva.app.usecase.GoalsService;
import com.deriva.goals.model.Goal;
import com.deriva.goals.model.GoalProgress;
import com.deriva.goals.model.GoalState;
import com.deriva.tracking.model.ContextKey;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Pure presentation and validation model for daily goals. It never touches JavaFX;
 * the controller supplies plain strings and runs the blocking calls off the FX thread.
 */
public final class GoalsViewModel {
    public record Outcome(GoalsPage page, String message, boolean error) {
        public boolean saved() {
            return !error;
        }
    }

    private final GoalsService service;
    private final ZoneId zone;

    private List<Goal> goals = List.of();
    private List<GoalContextOption> contexts = List.of();
    private Map<Long, GoalProgress> progressByGoal = Map.of();
    private long editingId;
    private String processText = "";
    private String hostText = "";
    private String minutesText = "";
    private String entriesText = "";
    private boolean passive;
    private boolean active = true;
    private String validationError;

    public GoalsViewModel(GoalsService service, ZoneId zone) {
        this.service = Objects.requireNonNull(service, "service");
        this.zone = Objects.requireNonNull(zone, "zone");
    }

    public GoalsPage load() {
        goals = service.listAll();
        contexts = buildContexts(service.observedContexts());
        LocalDate today = service.today(zone);
        progressByGoal = new LinkedHashMap<>();
        for (GoalProgress progress : service.progressForDay(today, zone))
            progressByGoal.put(progress.goal().id(), progress);
        if (editingId > 0 && find(editingId) == null) resetForm();
        return page();
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

    public Outcome toggleActive(long id) {
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
        List<GoalRow> rows = new ArrayList<>(goals.size());
        for (Goal goal : goals) {
            GoalProgress progress = progressByGoal.get(goal.id());
            rows.add(row(goal, progress));
        }
        long activeCount = goals.stream().filter(Goal::active).count();
        String summary = goals.isEmpty()
                ? "Sin objetivos"
                : goals.size() + (goals.size() == 1 ? " objetivo · " : " objetivos · ")
                + activeCount + (activeCount == 1 ? " activo" : " activos");
        return new GoalsPage(rows, contexts, summary, !goals.isEmpty(), editingId > 0,
                editingId > 0, editingId > 0 ? "Editar objetivo" : "Nuevo objetivo",
                processText, hostText, minutesText, entriesText, passive, active,
                validationError, canSave());
    }

    private boolean canSave() {
        return !processText.isBlank()
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

    private GoalRow row(Goal goal, GoalProgress progress) {
        String contextLabel = contextLabel(goal.context());
        String limits = limitsText(goal);
        if (!goal.active() || progress == null) {
            return new GoalRow(goal.id(), contextLabel, limits,
                    "Sin progreso mientras está desactivado.", null, "Desactivado", "—",
                    "inactive", 0, false, goal.passive(), false);
        }
        GoalState state = progress.state();
        String progressText = progressText(goal, progress);
        double ratio = ratio(goal, progress, state);
        return new GoalRow(goal.id(), contextLabel, limits, progressText, state,
                stateLabel(state), stateIcon(state), stateKey(state), ratio, true,
                goal.passive(), progress.unmeasurable());
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
        if (state == GoalState.EXCEEDED) return 1;
        return Math.min(1, ratio);
    }

    public static String stateLabel(GoalState state) {
        return switch (state) {
            case IN_PROGRESS -> "En curso";
            case NEAR -> "Cerca del tope";
            case EXCEEDED -> "Tope superado";
        };
    }

    public static String stateIcon(GoalState state) {
        return switch (state) {
            case IN_PROGRESS -> "▲";
            case NEAR -> "◆";
            case EXCEEDED -> "●";
        };
    }

    public static String stateKey(GoalState state) {
        return switch (state) {
            case IN_PROGRESS -> "in-progress";
            case NEAR -> "near";
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
