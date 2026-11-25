package com.deriva.app;

import com.deriva.privacy.EmObservationKind;
import com.deriva.tracking.model.EmSegment;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Temporary console aggregate for F3.1b; not the final DailyUsageCalculator or UI. */
public final class EmConsoleSummary {
    public record Summary(
            Map<String, Duration> observedByApp,
            Map<String, Duration> observedBySite,
            Duration idle,
            Duration excluded,
            Duration unavailable,
            Duration uncovered) {

        public Summary {
            observedByApp = Map.copyOf(Objects.requireNonNull(observedByApp, "observedByApp"));
            observedBySite = Map.copyOf(Objects.requireNonNull(observedBySite, "observedBySite"));
            idle = Objects.requireNonNull(idle, "idle");
            excluded = Objects.requireNonNull(excluded, "excluded");
            unavailable = Objects.requireNonNull(unavailable, "unavailable");
            uncovered = Objects.requireNonNull(uncovered, "uncovered");
        }

        public Duration observedTotal() {
            Duration total = Duration.ZERO;
            for (Duration duration : observedByApp.values()) total = total.plus(duration);
            return total;
        }
    }

    private EmConsoleSummary() {}

    public static Summary of(List<EmSegment> segments, Instant from, Instant to) {
        Objects.requireNonNull(segments, "segments");
        Objects.requireNonNull(from, "from");
        Objects.requireNonNull(to, "to");
        if (!from.isBefore(to)) throw new IllegalArgumentException("from must be before to");

        Map<String, Duration> apps = new HashMap<>();
        Map<String, Duration> sites = new HashMap<>();
        Duration idle = Duration.ZERO;
        Duration excluded = Duration.ZERO;
        Duration unavailable = Duration.ZERO;
        Instant coverageStart = null;

        for (EmSegment segment : segments) {
            Instant start = segment.start().isBefore(from) ? from : segment.start();
            Instant end = segment.end().isAfter(to) ? to : segment.end();
            if (!end.isAfter(start)) continue;
            if (coverageStart == null || start.isBefore(coverageStart)) coverageStart = start;
            Duration duration = Duration.between(start, end);

            switch (segment.kind()) {
                case OBSERVED -> {
                    apps.merge(segment.context().processName(), duration, Duration::plus);
                    if (segment.context().siteHost() != null)
                        sites.merge(segment.context().siteHost(), duration, Duration::plus);
                }
                case IDLE -> idle = idle.plus(duration);
                case EXCLUDED -> excluded = excluded.plus(duration);
                case UNAVAILABLE -> unavailable = unavailable.plus(duration);
            }
        }

        Duration observed = Duration.ZERO;
        for (Duration duration : apps.values()) observed = observed.plus(duration);
        Duration covered = observed.plus(idle).plus(excluded).plus(unavailable);
        Instant effectiveStart = coverageStart == null ? from : coverageStart;
        Duration total = Duration.between(effectiveStart, to);
        Duration uncovered = total.minus(covered).isNegative() ? Duration.ZERO : total.minus(covered);
        return new Summary(apps, sites, idle, excluded, unavailable, uncovered);
    }

    public static String format(Summary summary) {
        StringBuilder text = new StringBuilder("Resumen del dia\n");
        text.append("Observado por app:\n");
        appendMap(text, summary.observedByApp());
        text.append("Sitios (desglose del navegador, no sumar aparte):\n");
        appendMap(text, summary.observedBySite());
        text.append("Total observado: ").append(duration(summary.observedTotal())).append('\n');
        text.append("IDLE: ").append(duration(summary.idle())).append('\n');
        text.append("EXCLUDED: ").append(duration(summary.excluded())).append('\n');
        text.append("Sin datos (UNAVAILABLE): ").append(duration(summary.unavailable())).append('\n');
        text.append("Sin registro (huecos desde el inicio de la sesion): ").append(duration(summary.uncovered())).append('\n');
        return text.toString();
    }

    private static void appendMap(StringBuilder text, Map<String, Duration> values) {
        if (values.isEmpty()) {
            text.append("  (sin datos)\n");
            return;
        }
        List<Map.Entry<String, Duration>> entries = new ArrayList<>(values.entrySet());
        entries.sort(Map.Entry.<String, Duration>comparingByValue(Comparator.reverseOrder())
                .thenComparing(Map.Entry.comparingByKey()));
        for (Map.Entry<String, Duration> entry : entries)
            text.append("  ").append(entry.getKey()).append("  ").append(duration(entry.getValue())).append('\n');
    }

    public static String duration(Duration value) {
        long seconds = Math.max(0, value.getSeconds());
        return String.format("%02d:%02d:%02d", seconds / 3600, (seconds % 3600) / 60, seconds % 60);
    }
}
