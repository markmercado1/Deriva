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
 * Reads, validates and saves the four detection thresholds. Invalid persisted
 * values fall back to defaults for the affected field, are reported visibly and
 * are not written back automatically.
 */
public final class SettingsService {
    public static final String KEY_SWITCHES = "drift_switches";
    public static final String KEY_WINDOW = "drift_window_s";
    public static final String KEY_STABILITY = "stable_s";
    public static final String KEY_IDLE = "idle_threshold_s";

    private final SettingsRepository repository;
    private SettingsSnapshot snapshot;

    public SettingsService(SettingsRepository repository) {
        this.repository = Objects.requireNonNull(repository, "repository");
        this.snapshot = readSnapshot();
    }

    public synchronized SettingsSnapshot snapshot() {
        return snapshot;
    }

    public synchronized DetectionSettings current() {
        return snapshot.values();
    }

    public synchronized SettingsSnapshot save(DetectionSettings values) {
        Objects.requireNonNull(values, "values");
        repository.saveAll(toMap(values));
        snapshot = new SettingsSnapshot(values, List.of());
        return snapshot;
    }

    private SettingsSnapshot readSnapshot() {
        Map<String, String> stored = repository.loadAll();
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
