package com.deriva.ui.viewmodel;

import com.deriva.app.usecase.EmInterruptionsService;
import com.deriva.coach.model.EmCloseReason;
import com.deriva.coach.model.EmDriftEvent;
import com.deriva.coach.model.EmDriftStatus;
import com.deriva.tracking.model.EmContextKey;

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
public final class EmInterruptionsViewModel {
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("dd/MM HH:mm");
    private static final List<EmReasonOption> REASONS = List.of(
            new EmReasonOption("SEARCHING_INFO", "Buscando información"),
            new EmReasonOption("MESSAGES", "Mensajes"),
            new EmReasonOption("BREAK", "Descanso"),
            new EmReasonOption("BOREDOM", "Aburrimiento"),
            new EmReasonOption("OTHER", "Otro"));
    private static final Map<String, String> REASON_LABELS = Map.of(
            "SEARCHING_INFO", "Buscando información",
            "MESSAGES", "Mensajes",
            "BREAK", "Descanso",
            "BOREDOM", "Aburrimiento",
            "OTHER", "Otro");

    private final EmInterruptionsService service;
    private final ZoneId zone;

    public EmInterruptionsViewModel(EmInterruptionsService service, ZoneId zone) {
        this.service = Objects.requireNonNull(service, "service");
        this.zone = Objects.requireNonNull(zone, "zone");
    }

    public EmInterruptionsPage load(LocalDate fromDay, LocalDate toDay, EmStatusFilter filter,
                                  boolean includeJustified) {
        Objects.requireNonNull(fromDay, "fromDay");
        Objects.requireNonNull(toDay, "toDay");
        Objects.requireNonNull(filter, "filter");
        List<EmDriftEvent> events = service.forRange(fromDay, toDay, zone);
        int open = (int) events.stream().filter(event -> event.status() == EmDriftStatus.OPEN).count();
        List<EmInterruptionRow> rows = events.stream()
                .filter(event -> includeJustified || !event.justified())
                .filter(event -> filter.matches(event.status()))
                .map(this::row)
                .toList();
        return new EmInterruptionsPage(rows, events.size(), open);
    }

    public EmInterruptionsPage load(LocalDate day, EmStatusFilter filter, boolean includeJustified) {
        return load(day, day, filter, includeJustified);
    }

    /**
     * In-memory examples for checking the screen. It never queries or writes the
     * repository; the controller disables annotation while this page is shown.
     */
    public EmInterruptionsPage demoPage(LocalDate day, EmStatusFilter filter, boolean includeJustified) {
        Objects.requireNonNull(day, "day");
        Objects.requireNonNull(filter, "filter");
        List<EmDriftEvent> events = demoEvents(day).stream()
                .sorted(Comparator.comparing(EmDriftEvent::startTs).reversed())
                .toList();
        int open = (int) events.stream().filter(event -> event.status() == EmDriftStatus.OPEN).count();
        List<EmInterruptionRow> rows = events.stream()
                .filter(event -> includeJustified || !event.justified())
                .filter(event -> filter.matches(event.status()))
                .map(this::row)
                .toList();
        return new EmInterruptionsPage(rows, events.size(), open);
    }

    private List<EmDriftEvent> demoEvents(LocalDate day) {
        EmContextKey whatsapp = new EmContextKey("chrome.exe", "web.whatsapp.com");
        EmContextKey instagram = new EmContextKey("chrome.exe", "instagram.com");
        EmContextKey youtube = new EmContextKey("chrome.exe", "youtube.com");
        EmContextKey docs = new EmContextKey("chrome.exe", "docs.google.com");
        EmContextKey gmail = new EmContextKey("chrome.exe", "mail.google.com");
        EmContextKey chrome = new EmContextKey("chrome.exe", null);
        EmContextKey vscode = new EmContextKey("code.exe", null);
        EmContextKey terminal = new EmContextKey("windowsterminal.exe", null);

        return List.of(
                demoStable(900001, day, 11, 8, 194_000, 10, instagram,
                        Set.of(instagram, vscode, docs), "MESSAGES", true),
                demoStable(900002, day, 9, 12, 218_000, 11, docs,
                        Set.of(docs, chrome), "SEARCHING_INFO", true),
                demoUnconfirmed(900003, day, 9, 41, 14, chrome,
                        Set.of(chrome, vscode), EmCloseReason.IDLE),
                demoStable(900004, day, 8, 36, 252_000, 10, youtube,
                        Set.of(youtube, gmail), "BREAK", false),
                demoOpen(900005, day, 14, 32, 12, whatsapp,
                        Set.of(whatsapp, instagram, docs)),
                demoStable(900006, day, 8, 14, 192_000, 13, vscode,
                        Set.of(vscode, terminal), "BREAK", false));
    }

    private EmDriftEvent demoStable(long id, LocalDate day, int hour, int minute, long stabilityMs,
                                  int switches, EmContextKey first, Set<EmContextKey> contexts,
                                  String reason, boolean justified) {
        Instant start = day.atTime(hour, minute).atZone(zone).toInstant();
        Instant stable = start.plusMillis(stabilityMs);
        return new EmDriftEvent(id, start, start.plusSeconds(60), stable, stable, stable,
                EmDriftStatus.STABLE, null, switches, first, stabilityMs, reason, justified,
                justified ? stable : null, contexts);
    }

    private EmDriftEvent demoOpen(long id, LocalDate day, int hour, int minute, int switches,
                                EmContextKey first, Set<EmContextKey> contexts) {
        Instant start = day.atTime(hour, minute).atZone(zone).toInstant();
        return new EmDriftEvent(id, start, start.plusSeconds(60), start.plusSeconds(180),
                null, null, EmDriftStatus.OPEN, null, switches, first, null, null, false, null, contexts);
    }

    private EmDriftEvent demoUnconfirmed(long id, LocalDate day, int hour, int minute, int switches,
                                       EmContextKey first, Set<EmContextKey> contexts, EmCloseReason reason) {
        Instant start = day.atTime(hour, minute).atZone(zone).toInstant();
        Instant closed = start.plusSeconds(120);
        return new EmDriftEvent(id, start, start.plusSeconds(60), closed, null, closed,
                EmDriftStatus.UNCONFIRMED, reason, switches, first, null, null, false, null, contexts);
    }

    public void annotate(long id, String reasonCode, boolean justified) {
        service.annotate(id, reasonCode, justified);
    }

    public List<EmReasonOption> reasons() {
        return REASONS;
    }

    public EmReasonOption reason(String code) {
        if (code == null) return null;
        return REASONS.stream().filter(option -> option.code().equals(code)).findFirst().orElse(null);
    }

    private EmInterruptionRow row(EmDriftEvent event) {
        String statusText = switch (event.status()) {
            case OPEN -> "En curso";
            case STABLE -> "Estable";
            case UNCONFIRMED -> "No comprobada";
        };
        String stability = event.status() == EmDriftStatus.STABLE
                ? duration(event.timeToStabilityMs())
                : "—";
        String annotation = event.answeredAt() == null ? "Pendiente" : "Guardada";
        return new EmInterruptionRow(
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

    public String display(EmContextKey context) {
        return EmDisplayFormats.context(context);
    }

    public String closeReasonText(EmCloseReason reason) {
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
    public String stabilityExplanation(EmInterruptionRow row) {
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
    public String annotationDetail(EmInterruptionRow row) {
        if (row == null || row.answeredAt() == null) return "Pendiente de guardar";
        return "Guardada · " + DATE_TIME.format(row.answeredAt().atZone(zone));
    }

    public static String duration(Long millis) {
        return EmDisplayFormats.duration(millis);
    }
}
