package com.deriva.ui.viewmodel;

import com.deriva.app.usecase.EmCaptureControl;
import com.deriva.app.usecase.EmCaptureSnapshot;
import com.deriva.app.usecase.EmDailyUsage;
import com.deriva.app.usecase.EmDailyUsageService;
import com.deriva.tracking.model.EmSegment;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

/** Presentation model for the Uso diario screen. */
public final class EmDailyUsageViewModel {
    private static final DateTimeFormatter SNAPSHOT_TIME = DateTimeFormatter.ofPattern("HH:mm:ss");

    private final EmDailyUsageService service;
    private final ZoneId zone;
    private final EmCaptureControl capture;

    public EmDailyUsageViewModel(EmDailyUsageService service, ZoneId zone) {
        this(service, zone, null);
    }

    public EmDailyUsageViewModel(EmDailyUsageService service, ZoneId zone, EmCaptureControl capture) {
        this.service = Objects.requireNonNull(service, "service");
        this.zone = Objects.requireNonNull(zone, "zone");
        this.capture = capture;
    }

    public EmUsagePage load(LocalDate day) {
        return load(day, day);
    }

    public EmUsagePage load(LocalDate day, LocalDate through) {
        Objects.requireNonNull(day, "day");
        EmCaptureSnapshot snapshot = capture == null ? EmCaptureSnapshot.stopped() : capture.snapshot();
        EmDailyUsage usage = service.forRange(day, through, zone, snapshot);
        List<EmUsageRow> apps = rows(usage.appMillis(), EmDisplayFormats::app);
        List<EmUsageRow> sites = rows(usage.siteMillis(), EmDisplayFormats::site);
        String total = usage.hasSegments()
                ? EmDisplayFormats.duration(usage.observedMillis())
                : "Sin datos";
        String coverage = usage.hasSegments()
                ? "Registrado " + EmDisplayFormats.duration(usage.recordedMillis())
                + " de " + EmDisplayFormats.duration(usage.elapsedMillis())
                + " · Sin registro " + EmDisplayFormats.duration(usage.unrecordedMillis())
                : "Sin registro para este periodo";
        return new EmUsagePage(day, "Zona horaria: " + zone.getId(), total, coverage,
                status(usage), apps, sites, usage.hasSegments());
    }

    private String status(EmDailyUsage usage) {
        EmCaptureSnapshot snapshot = usage.snapshot();
        if (snapshot.captureRunning()) {
            String base = snapshot.capturedAt() == null
                    ? "Captura activa"
                    : "Instantánea " + SNAPSHOT_TIME.format(snapshot.capturedAt().atZone(zone));
            if (snapshot.open() != null) base += " · abierto: " + liveLabel(snapshot.open());
            if (snapshot.pending() != null)
                base += " · pendiente: " + liveLabel(snapshot.pending())
                        + " (no suma hasta confirmarse)";
            if (snapshot.open() == null && snapshot.pending() == null)
                base += " · sin segmento abierto todavía";
            return base + ".";
        }
        if (!usage.hasSegments()) {
            return "Sin datos para este periodo. No hay segmentos guardados.";
        }
        if (usage.observedMillis() == 0) {
            return "Sin uso en primer plano; solo hay inactividad, exclusiones o datos no disponibles.";
        }
        return "Datos persistidos; el segmento abierto no se incluye.";
    }

    private static String liveLabel(EmSegment segment) {
        if (segment.kind() != com.deriva.privacy.EmObservationKind.OBSERVED)
            return switch (segment.kind()) {
                case IDLE -> "inactividad";
                case EXCLUDED -> "exclusión";
                case UNAVAILABLE -> "datos no disponibles";
                case OBSERVED -> "observado";
            };
        return EmDisplayFormats.context(segment.context());
    }

    private static List<EmUsageRow> rows(Map<String, Long> values, Function<String, String> naming) {
        long max = values.values().stream().mapToLong(Long::longValue).max().orElse(0L);
        return values.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed()
                        .thenComparing(Map.Entry.comparingByKey()))
                .map(entry -> new EmUsageRow(naming.apply(entry.getKey()), entry.getValue(),
                        EmDisplayFormats.duration(entry.getValue()),
                        max == 0 ? 0 : entry.getValue() / (double) max))
                .toList();
    }
}
