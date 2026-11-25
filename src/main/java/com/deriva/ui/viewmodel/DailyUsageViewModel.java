package com.deriva.ui.viewmodel;

import com.deriva.app.usecase.CaptureControl;
import com.deriva.app.usecase.CaptureSnapshot;
import com.deriva.app.usecase.DailyUsage;
import com.deriva.app.usecase.DailyUsageService;
import com.deriva.tracking.model.Segment;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

/** Presentation model for the Uso diario screen. */
public final class DailyUsageViewModel {
    private static final DateTimeFormatter SNAPSHOT_TIME = DateTimeFormatter.ofPattern("HH:mm:ss");

    private final DailyUsageService service;
    private final ZoneId zone;
    private final CaptureControl capture;

    public DailyUsageViewModel(DailyUsageService service, ZoneId zone) {
        this(service, zone, null);
    }

    public DailyUsageViewModel(DailyUsageService service, ZoneId zone, CaptureControl capture) {
        this.service = Objects.requireNonNull(service, "service");
        this.zone = Objects.requireNonNull(zone, "zone");
        this.capture = capture;
    }

    public UsagePage load(LocalDate day) {
        return load(day, day);
    }

    public UsagePage load(LocalDate day, LocalDate through) {
        Objects.requireNonNull(day, "day");
        CaptureSnapshot snapshot = capture == null ? CaptureSnapshot.stopped() : capture.snapshot();
        DailyUsage usage = service.forRange(day, through, zone, snapshot);
        List<UsageRow> apps = rows(usage.appMillis(), DisplayFormats::app);
        List<UsageRow> sites = rows(usage.siteMillis(), DisplayFormats::site);
        String total = usage.hasSegments()
                ? DisplayFormats.duration(usage.observedMillis())
                : "Sin datos";
        String coverage = usage.hasSegments()
                ? "Registrado " + DisplayFormats.duration(usage.recordedMillis())
                + " de " + DisplayFormats.duration(usage.elapsedMillis())
                + " · Sin registro " + DisplayFormats.duration(usage.unrecordedMillis())
                : "Sin registro para este periodo";
        return new UsagePage(day, "Zona horaria: " + zone.getId(), total, coverage,
                status(usage), apps, sites, usage.hasSegments());
    }

    private String status(DailyUsage usage) {
        CaptureSnapshot snapshot = usage.snapshot();
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

    private static String liveLabel(Segment segment) {
        if (segment.kind() != com.deriva.privacy.ObservationKind.OBSERVED)
            return switch (segment.kind()) {
                case IDLE -> "inactividad";
                case EXCLUDED -> "exclusión";
                case UNAVAILABLE -> "datos no disponibles";
                case OBSERVED -> "observado";
            };
        return DisplayFormats.context(segment.context());
    }

    private static List<UsageRow> rows(Map<String, Long> values, Function<String, String> naming) {
        long max = values.values().stream().mapToLong(Long::longValue).max().orElse(0L);
        return values.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed()
                        .thenComparing(Map.Entry.comparingByKey()))
                .map(entry -> new UsageRow(naming.apply(entry.getKey()), entry.getValue(),
                        DisplayFormats.duration(entry.getValue()),
                        max == 0 ? 0 : entry.getValue() / (double) max))
                .toList();
    }
}
