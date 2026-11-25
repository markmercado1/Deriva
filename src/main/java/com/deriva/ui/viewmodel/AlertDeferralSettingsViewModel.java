package com.deriva.ui.viewmodel;

import com.deriva.app.usecase.AlertDeferralSnapshot;
import com.deriva.app.usecase.SettingsService;

import java.time.Duration;
import java.util.List;
import java.util.Objects;

/**
 * Pure form model for the maximum wait of a deferred goal notice. It follows the
 * same effective-versus-pending rule as the detection settings.
 */
public final class AlertDeferralSettingsViewModel {
    public record Page(
            String minutesText,
            String effectiveText,
            String savedText,
            String validationError,
            boolean unsavedChanges,
            boolean pending,
            boolean canSave,
            List<String> loadWarnings) {

        public Page {
            loadWarnings = List.copyOf(loadWarnings);
        }

        public boolean valid() {
            return validationError == null;
        }
    }

    private String minutesText;
    private Duration saved;
    private Duration effective;
    private List<String> loadWarnings;

    public AlertDeferralSettingsViewModel() {
        Duration initial = Duration.ofSeconds(SettingsService.DEFAULT_ALERT_DEFERRAL_SECONDS);
        saved = initial;
        effective = initial;
        loadWarnings = List.of();
        setForm(initial);
    }

    public void load(AlertDeferralSnapshot snapshot, Duration effectiveValue) {
        Objects.requireNonNull(snapshot, "snapshot");
        saved = snapshot.value();
        effective = Objects.requireNonNull(effectiveValue, "effectiveValue");
        loadWarnings = snapshot.warnings();
        setForm(saved);
    }

    public void applySaved(AlertDeferralSnapshot snapshot) {
        Objects.requireNonNull(snapshot, "snapshot");
        saved = snapshot.value();
        loadWarnings = snapshot.warnings();
    }

    public void setEffective(Duration effectiveValue) {
        effective = Objects.requireNonNull(effectiveValue, "effectiveValue");
    }

    public void update(String text) {
        minutesText = text == null ? "" : text;
    }

    public void restoreDefaults() {
        setForm(Duration.ofSeconds(SettingsService.DEFAULT_ALERT_DEFERRAL_SECONDS));
    }

    public Duration validatedValue() {
        Result result = result();
        if (result.error() != null) throw new IllegalStateException(result.error());
        return result.value();
    }

    public Page page() {
        Result result = result();
        boolean unsaved = result.value() != null && !result.value().equals(saved);
        return new Page(minutesText, summary(effective), summary(saved),
                result.error(), unsaved, !saved.equals(effective),
                result.error() == null && unsaved, loadWarnings);
    }

    private Result result() {
        if (minutesText == null || minutesText.isBlank())
            return new Result(null, "Escribe el máximo de espera en minutos.");
        try {
            long minutes = Long.parseLong(minutesText.trim());
            long min = SettingsService.MIN_ALERT_DEFERRAL_SECONDS / 60;
            long max = SettingsService.MAX_ALERT_DEFERRAL_SECONDS / 60;
            if (minutes < min || minutes > max)
                return new Result(null, "Máximo de espera de un aviso: entre "
                        + min + " y " + max + " minutos.");
            return new Result(Duration.ofMinutes(minutes), null);
        } catch (NumberFormatException e) {
            return new Result(null, "Usa un número entero de minutos.");
        }
    }

    private void setForm(Duration value) {
        minutesText = Long.toString(value.toSeconds() / 60);
    }

    public static String summary(Duration value) {
        long seconds = value.toSeconds();
        if (seconds % 60 == 0) return seconds + " s (" + seconds / 60 + " min)";
        return seconds + " s (" + seconds / 60 + " min " + (seconds % 60) + " s)";
    }

    private record Result(Duration value, String error) {}
}
