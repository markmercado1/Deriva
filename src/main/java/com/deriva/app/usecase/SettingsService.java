package com.deriva.app.usecase;

import com.deriva.coach.model.DetectionSettings;
import com.deriva.storage.port.SettingsRepository;

import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Reads, validates and saves the four detection thresholds plus the passive-activity
 * maximum and the maximum wait for a deferred goal notice. Invalid persisted values
 * fall back to defaults for the affected field, are reported visibly and are not
 * written back automatically.
 */
public final class SettingsService {
    public static final String KEY_SWITCHES = "drift_switches";
    public static final String KEY_WINDOW = "drift_window_s";
    public static final String KEY_STABILITY = "stable_s";
    public static final String KEY_IDLE = "idle_threshold_s";
    public static final String KEY_PASSIVE_MAX = "passive_max_s";
    public static final String KEY_ALERT_DEFERRAL = "alert_deferral_max_s";
    public static final int MIN_PASSIVE_SECONDS = 60;
    public static final int MAX_PASSIVE_SECONDS = 3600;
    public static final int DEFAULT_PASSIVE_SECONDS = 600;
    public static final int MIN_ALERT_DEFERRAL_SECONDS = 60;
    public static final int MAX_ALERT_DEFERRAL_SECONDS = 1800;
    public static final int DEFAULT_ALERT_DEFERRAL_SECONDS = 300;

    private final SettingsRepository repository;
    private SettingsSnapshot snapshot;
    private PassiveMaxSnapshot passiveMax;
    private AlertDeferralSnapshot alertDeferral;

    public SettingsService(SettingsRepository repository) {
        this.repository = Objects.requireNonNull(repository, "repository");
        Map<String, String> stored = repository.loadAll();
        this.snapshot = readSnapshot(stored);
        this.passiveMax = readPassiveMax(stored);
        this.alertDeferral = readAlertDeferral(stored);
    }

    public synchronized SettingsSnapshot snapshot() {
        return snapshot;
    }

    public synchronized DetectionSettings current() {
        return snapshot.values();
    }

    public synchronized PassiveMaxSnapshot passiveMaxSnapshot() {
        return passiveMax;
    }

    public synchronized Duration passiveMax() {
        return passiveMax.value();
    }

    public synchronized AlertDeferralSnapshot alertDeferralSnapshot() {
        return alertDeferral;
    }

    public synchronized Duration alertDeferralMax() {
        return alertDeferral.value();
    }

    public synchronized SettingsSnapshot save(DetectionSettings values) {
        Objects.requireNonNull(values, "values");
        repository.saveAll(toMap(values));
        snapshot = new SettingsSnapshot(values, List.of());
        return snapshot;
    }

    public synchronized PassiveMaxSnapshot savePassiveMax(Duration value) {
        Objects.requireNonNull(value, "value");
        long seconds = value.getSeconds();
        if (value.getNano() != 0 || seconds < MIN_PASSIVE_SECONDS || seconds > MAX_PASSIVE_SECONDS)
            throw new IllegalArgumentException("passive max");
        repository.saveAll(Map.of(KEY_PASSIVE_MAX, Long.toString(seconds)));
        passiveMax = new PassiveMaxSnapshot(value, List.of());
        return passiveMax;
    }

    public synchronized AlertDeferralSnapshot saveAlertDeferralMax(Duration value) {
        Objects.requireNonNull(value, "value");
        long seconds = value.getSeconds();
        if (value.getNano() != 0 || seconds < MIN_ALERT_DEFERRAL_SECONDS
                || seconds > MAX_ALERT_DEFERRAL_SECONDS)
            throw new IllegalArgumentException("alert deferral");
        repository.saveAll(Map.of(KEY_ALERT_DEFERRAL, Long.toString(seconds)));
        alertDeferral = new AlertDeferralSnapshot(value, List.of());
        return alertDeferral;
    }

    private AlertDeferralSnapshot readAlertDeferral(Map<String, String> stored) {
        List<String> warnings = new ArrayList<>();
        int seconds = readInt(stored, KEY_ALERT_DEFERRAL, DEFAULT_ALERT_DEFERRAL_SECONDS,
                MIN_ALERT_DEFERRAL_SECONDS, MAX_ALERT_DEFERRAL_SECONDS,
                "Máximo de espera de un aviso", "s", warnings);
        return new AlertDeferralSnapshot(Duration.ofSeconds(seconds), warnings);
    }

    private PassiveMaxSnapshot readPassiveMax(Map<String, String> stored) {
        List<String> warnings = new ArrayList<>();
        int seconds = readInt(stored, KEY_PASSIVE_MAX, DEFAULT_PASSIVE_SECONDS,
                MIN_PASSIVE_SECONDS, MAX_PASSIVE_SECONDS,
                "Máximo de actividad pasiva", "s", warnings);
        return new PassiveMaxSnapshot(Duration.ofSeconds(seconds), warnings);
    }

    private SettingsSnapshot readSnapshot(Map<String, String> stored) {
        List<String> warnings = new ArrayList<>();
        DetectionSettings defaults = DetectionSettings.defaults();

        int switches = readInt(stored, KEY_SWITCHES, defaults.switches(),
                DetectionSettings.MIN_SWITCHES, DetectionSettings.MAX_SWITCHES,
                "Cambios para abrir un episodio", "cambios", warnings);
        int window = readInt(stored, KEY_WINDOW, (int) defaults.window().toSeconds(),
                DetectionSettings.MIN_WINDOW_SECONDS, DetectionSettings.MAX_WINDOW_SECONDS,
                "Ventana de tiempo", "s", warnings);
        int stability = readInt(stored, KEY_STABILITY, (int) defaults.stability().toSeconds(),
                DetectionSettings.MIN_STABILITY_SECONDS, DetectionSettings.MAX_STABILITY_SECONDS,
                "Tiempo de estabilidad", "s", warnings);
        int idle = readInt(stored, KEY_IDLE, (int) defaults.idle().toSeconds(),
                DetectionSettings.MIN_IDLE_SECONDS, DetectionSettings.MAX_IDLE_SECONDS,
                "Tiempo de inactividad", "s", warnings);

        DetectionSettings values = new DetectionSettings(switches,
                Duration.ofSeconds(window),
                Duration.ofSeconds(stability),
                Duration.ofSeconds(idle));
        return new SettingsSnapshot(values, warnings);
    }

    private static int readInt(Map<String, String> stored, String key, int fallback,
                               int min, int max, String label, String unit,
                               List<String> warnings) {
        String raw = stored.get(key);
        if (raw == null || raw.isBlank()) return fallback;
        try {
            int value = Integer.parseInt(raw.trim());
            if (value >= min && value <= max) return value;
        } catch (NumberFormatException ignored) {
            // Reported below as one visible warning.
        }
        warnings.add(label + ": el valor guardado no es válido; se usa " + fallback + " " + unit + ".");
        return fallback;
    }

    private static Map<String, String> toMap(DetectionSettings values) {
        Map<String, String> stored = new LinkedHashMap<>();
        stored.put(KEY_SWITCHES, Integer.toString(values.switches()));
        stored.put(KEY_WINDOW, Long.toString(values.window().toSeconds()));
        stored.put(KEY_STABILITY, Long.toString(values.stability().toSeconds()));
        stored.put(KEY_IDLE, Long.toString(values.idle().toSeconds()));
        return stored;
    }
}
