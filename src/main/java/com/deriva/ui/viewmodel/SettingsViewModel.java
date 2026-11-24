package com.deriva.ui.viewmodel;

import com.deriva.app.usecase.SettingsSnapshot;
import com.deriva.coach.model.DetectionSettings;

import java.time.Duration;
import java.util.List;
import java.util.Objects;

/** Pure presentation and validation model for the four detection settings. */
public final class SettingsViewModel {
    public record Page(
            String switchesText,
            String windowText,
            String stabilityText,
            String idleText,
            String effectiveSummary,
            String savedSummary,
            String validationError,
            boolean unsavedChanges,
            boolean pending,
            boolean canSave,
            boolean idleWarning,
            List<String> loadWarnings) {

        public Page {
            loadWarnings = List.copyOf(loadWarnings);
        }

        public boolean valid() {
            return validationError == null;
        }
    }

    private String switchesText;
    private String windowText;
    private String stabilityText;
    private String idleText;
    private DetectionSettings saved;
    private DetectionSettings effective;
    private List<String> loadWarnings;

    public SettingsViewModel() {
        this(DetectionSettings.defaults());
    }

    private SettingsViewModel(DetectionSettings initial) {
        saved = initial;
        effective = initial;
        loadWarnings = List.of();
        setForm(initial);
    }

    public void load(SettingsSnapshot snapshot, DetectionSettings effectiveSettings) {
        Objects.requireNonNull(snapshot, "snapshot");
        saved = snapshot.values();
        effective = Objects.requireNonNull(effectiveSettings, "effectiveSettings");
        loadWarnings = snapshot.warnings();
        setForm(saved);
    }

    public void applySaved(SettingsSnapshot snapshot) {
        Objects.requireNonNull(snapshot, "snapshot");
        saved = snapshot.values();
        loadWarnings = snapshot.warnings();
    }

    public void setEffective(DetectionSettings effectiveSettings) {
        effective = Objects.requireNonNull(effectiveSettings, "effectiveSettings");
    }

    public void update(String switches, String window, String stability, String idle) {
        switchesText = switches == null ? "" : switches;
        windowText = window == null ? "" : window;
        stabilityText = stability == null ? "" : stability;
        idleText = idle == null ? "" : idle;
    }

    public void restoreDefaults() {
        setForm(DetectionSettings.defaults());
    }

    public DetectionSettings validatedValues() {
        Result result = result();
        if (result.error() != null) throw new IllegalStateException(result.error());
        return result.values();
    }

    public Page page() {
        Result result = result();
        boolean unsaved = result.values() != null && !result.values().equals(saved);
        return new Page(switchesText, windowText, stabilityText, idleText,
                summary(effective), summary(saved), result.error(),
                unsaved, !saved.equals(effective),
                result.error() == null && unsaved,
                result.values() != null && result.values().idleAtOrBelowStability(),
                loadWarnings);
    }

    private Result result() {
        Integer switches = parse(switchesText);
        Integer window = parse(windowText);
        Integer stability = parse(stabilityText);
        Integer idle = parse(idleText);
        if (switches == null || window == null || stability == null || idle == null)
            return new Result(null, "Usa números enteros en los cuatro campos.");

        if (switches < DetectionSettings.MIN_SWITCHES || switches > DetectionSettings.MAX_SWITCHES)
            return new Result(null, "Cambios para abrir un episodio: entre "
                    + DetectionSettings.MIN_SWITCHES + " y " + DetectionSettings.MAX_SWITCHES + ".");
        if (window < DetectionSettings.MIN_WINDOW_SECONDS || window > DetectionSettings.MAX_WINDOW_SECONDS)
            return new Result(null, "Ventana de tiempo: entre "
                    + DetectionSettings.MIN_WINDOW_SECONDS + " y "
                    + DetectionSettings.MAX_WINDOW_SECONDS + " segundos.");
        if (stability < DetectionSettings.MIN_STABILITY_SECONDS
                || stability > DetectionSettings.MAX_STABILITY_SECONDS)
            return new Result(null, "Tiempo de estabilidad: entre "
                    + DetectionSettings.MIN_STABILITY_SECONDS + " y "
                    + DetectionSettings.MAX_STABILITY_SECONDS + " segundos.");
        if (idle < DetectionSettings.MIN_IDLE_SECONDS || idle > DetectionSettings.MAX_IDLE_SECONDS)
            return new Result(null, "Tiempo de inactividad: entre "
                    + DetectionSettings.MIN_IDLE_SECONDS + " y "
                    + DetectionSettings.MAX_IDLE_SECONDS + " segundos.");

        try {
            return new Result(new DetectionSettings(switches,
                    Duration.ofSeconds(window), Duration.ofSeconds(stability),
                    Duration.ofSeconds(idle)), null);
        } catch (IllegalArgumentException e) {
            return new Result(null, "Revisa los valores: hay una combinación no válida.");
        }
    }

    private void setForm(DetectionSettings values) {
        switchesText = Integer.toString(values.switches());
        windowText = Long.toString(values.window().toSeconds());
        stabilityText = Long.toString(values.stability().toSeconds());
        idleText = Long.toString(values.idle().toSeconds());
    }

    private static Integer parse(String text) {
        if (text == null || text.isBlank()) return null;
        try {
            return Integer.valueOf(text.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public static String summary(DetectionSettings values) {
        return values.switches() + " cambios · " + seconds(values.window()) + " de ventana · "
                + seconds(values.stability()) + " de estabilidad · "
                + seconds(values.idle()) + " de inactividad";
    }

    public static String seconds(Duration duration) {
        long seconds = duration.toSeconds();
        if (seconds % 60 == 0) return seconds + " s (" + seconds / 60 + " min)";
        return seconds + " s (" + seconds / 60 + " min " + (seconds % 60) + " s)";
    }

    private record Result(DetectionSettings values, String error) {}
}
