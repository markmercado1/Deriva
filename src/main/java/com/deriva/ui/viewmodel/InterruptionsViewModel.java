package com.deriva.ui.viewmodel;

import com.deriva.app.usecase.InterruptionsService;
import com.deriva.coach.model.CloseReason;
import com.deriva.coach.model.DriftEvent;
import com.deriva.coach.model.DriftStatus;
import com.deriva.tracking.model.ContextKey;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/** Pure presentation model for the Interrupciones screen. */
public final class InterruptionsViewModel {
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("dd/MM HH:mm");
    private static final List<ReasonOption> REASONS = List.of(
            new ReasonOption("SEARCHING_INFO", "Buscando información"),
            new ReasonOption("MESSAGES", "Mensajes"),
            new ReasonOption("BREAK", "Descanso"),
            new ReasonOption("BOREDOM", "Aburrimiento"),
            new ReasonOption("OTHER", "Otro"));
    private static final Map<String, String> REASON_LABELS = Map.of(
            "SEARCHING_INFO", "Buscando información",
            "MESSAGES", "Mensajes",
            "BREAK", "Descanso",
            "BOREDOM", "Aburrimiento",
            "OTHER", "Otro");

    private final InterruptionsService service;
    private final ZoneId zone;

    public InterruptionsViewModel(InterruptionsService service, ZoneId zone) {
        this.service = Objects.requireNonNull(service, "service");
        this.zone = Objects.requireNonNull(zone, "zone");
    }

    public InterruptionsPage load(LocalDate fromDay, LocalDate toDay, StatusFilter filter,
                                  boolean includeJustified) {
        Objects.requireNonNull(fromDay, "fromDay");
        Objects.requireNonNull(toDay, "toDay");
        Objects.requireNonNull(filter, "filter");
        List<DriftEvent> events = service.forRange(fromDay, toDay, zone);
        int open = (int) events.stream().filter(event -> event.status() == DriftStatus.OPEN).count();
        List<InterruptionRow> rows = events.stream()
                .filter(event -> includeJustified || !event.justified())
                .filter(event -> filter.matches(event.status()))
                .map(this::row)
                .toList();
        return new InterruptionsPage(rows, events.size(), open);
    }

    public InterruptionsPage load(LocalDate day, StatusFilter filter, boolean includeJustified) {
        return load(day, day, filter, includeJustified);
    }

    /**
     * In-memory examples for checking the screen. It never queries or writes the
     * repository; the controller disables annotation while this page is shown.
     */
    public InterruptionsPage demoPage(LocalDate day, StatusFilter filter, boolean includeJustified) {
        Objects.requireNonNull(day, "day");
        Objects.requireNonNull(filter, "filter");
        List<DriftEvent> events = demoEvents(day).stream()
                .sorted(Comparator.comparing(DriftEvent::startTs).reversed())
                .toList();
        int open = (int) events.stream().filter(event -> event.status() == DriftStatus.OPEN).count();
        List<InterruptionRow> rows = events.stream()
                .filter(event -> includeJustified || !event.justified())
                .filter(event -> filter.matches(event.status()))
                .map(this::row)
                .toList();
        return new InterruptionsPage(rows, events.size(), open);
    }

    private List<DriftEvent> demoEvents(LocalDate day) {
        ContextKey whatsapp = new ContextKey("chrome.exe", "web.whatsapp.com");
        ContextKey instagram = new ContextKey("chrome.exe", "instagram.com");
        ContextKey youtube = new ContextKey("chrome.exe", "youtube.com");
        ContextKey docs = new ContextKey("chrome.exe", "docs.google.com");
        ContextKey gmail = new ContextKey("chrome.exe", "mail.google.com");
        ContextKey chrome = new ContextKey("chrome.exe", null);
        ContextKey vscode = new ContextKey("code.exe", null);
        ContextKey terminal = new ContextKey("windowsterminal.exe", null);

        return List.of(
                demoStable(900001, day, 11, 8, 194_000, 10, instagram,
                        Set.of(instagram, vscode, docs), "MESSAGES", true),
                demoStable(900002, day, 9, 12, 218_000, 11, docs,
                        Set.of(docs, chrome), "SEARCHING_INFO", true),
                demoUnconfirmed(900003, day, 9, 41, 14, chrome,
                        Set.of(chrome, vscode), CloseReason.IDLE),
                demoStable(900004, day, 8, 36, 252_000, 10, youtube,
                        Set.of(youtube, gmail), "BREAK", false),
                demoOpen(900005, day, 14, 32, 12, whatsapp,
                        Set.of(whatsapp, instagram, docs)),
                demoStable(900006, day, 8, 14, 192_000, 13, vscode,
                        Set.of(vscode, terminal), "BREAK", false));
    }

    private DriftEvent demoStable(long id, LocalDate day, int hour, int minute, long stabilityMs,
                                  int switches, ContextKey first, Set<ContextKey> contexts,
                                  String reason, boolean justified) {
        Instant start = day.atTime(hour, minute).atZone(zone).toInstant();
        Instant stable = start.plusMillis(stabilityMs);
        return new DriftEvent(id, start, start.plusSeconds(60), stable, stable, stable,
                DriftStatus.STABLE, null, switches, first, stabilityMs, reason, justified,
                justified ? stable : null, contexts);
    }

    private DriftEvent demoOpen(long id, LocalDate day, int hour, int minute, int switches,
                                ContextKey first, Set<ContextKey> contexts) {
        Instant start = day.atTime(hour, minute).atZone(zone).toInstant();
        return new DriftEvent(id, start, start.plusSeconds(60), start.plusSeconds(180),
                null, null, DriftStatus.OPEN, null, switches, first, null, null, false, null, contexts);
    }

    private DriftEvent demoUnconfirmed(long id, LocalDate day, int hour, int minute, int switches,
                                       ContextKey first, Set<ContextKey> contexts, CloseReason reason) {
        Instant start = day.atTime(hour, minute).atZone(zone).toInstant();
        Instant closed = start.plusSeconds(120);
        return new DriftEvent(id, start, start.plusSeconds(60), closed, null, closed,
                DriftStatus.UNCONFIRMED, reason, switches, first, null, null, false, null, contexts);
    }

    public void annotate(long id, String reasonCode, boolean justified) {
        service.annotate(id, reasonCode, justified);
    }

    public List<ReasonOption> reasons() {
        return REASONS;
    }

    public ReasonOption reason(String code) {
        if (code == null) return null;
        return REASONS.stream().filter(option -> option.code().equals(code)).findFirst().orElse(null);
    }

    private InterruptionRow row(DriftEvent event) {
        String statusText = switch (event.status()) {
            case OPEN -> "En curso";
            case STABLE -> "Estable";
            case UNCONFIRMED -> "No comprobada";
        };
        String stability = event.status() == DriftStatus.STABLE
                ? duration(event.timeToStabilityMs())
                : "—";
        String annotation = event.answeredAt() == null ? "Pendiente" : "Guardada";
        return new InterruptionRow(
                event.id(),
                event.startTs(),
                DATE_TIME.format(event.startTs().atZone(zone)),
                event.status(),
                statusText,
                display(event.firstChangeContext()),
                event.switchCount(),
                stability,
                annotation,
                event.answeredAt(),
                event.reason(),
                event.reason() == null ? "Sin motivo" : REASON_LABELS.getOrDefault(event.reason(), event.reason()),
                event.justified(),
                List.copyOf(event.contexts()),
                event.closeReason());
    }

    public String display(ContextKey context) {
        return DisplayFormats.context(context);
    }

    public String closeReasonText(CloseReason reason) {
        if (reason == null) return "";
        return switch (reason) {
            case IDLE -> "el cierre por inactividad";
            case EXCLUSION -> "el cierre por exclusión";
            case UNAVAILABLE -> "la falta de datos";
            case PAUSE -> "el cierre por pausa";
            case LOCK -> "el cierre por bloqueo";
            case EXIT -> "el cierre al salir de la aplicación";
            case GAP -> "un hueco de datos";
            case RESTART -> "el reinicio de la captura";
        };
    }

    /** Why stability was or was not checked, using the persisted close reason. */
    public String stabilityExplanation(InterruptionRow row) {
        if (row == null) return "";
        return switch (row.status()) {
            case STABLE -> "Estabilidad comprobada: el contexto se mantuvo sin cambios hasta la comprobación automática.";
            case OPEN -> "Estabilidad aún no comprobada: el episodio sigue en curso.";
            case UNCONFIRMED -> {
                String reason = closeReasonText(row.closeReason());
                yield reason.isBlank()
                        ? "No se comprobó la estabilidad: no hay motivo de cierre registrado."
                        : "No se comprobó la estabilidad: " + reason + ".";
            }
        };
    }

    /** Persisted annotation state; answeredAt is written only after a successful save. */
    public String annotationDetail(InterruptionRow row) {
        if (row == null || row.answeredAt() == null) return "Pendiente de guardar";
        return "Guardada · " + DATE_TIME.format(row.answeredAt().atZone(zone));
    }

    public static String duration(Long millis) {
        return DisplayFormats.duration(millis);
    }
}
