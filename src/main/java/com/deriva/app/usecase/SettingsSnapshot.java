package com.deriva.app.usecase;

import com.deriva.coach.model.DetectionSettings;

import java.util.List;
import java.util.Objects;

/**
 * Values actually in use and warnings produced while reading persisted settings.
 * Warnings never cause a silent overwrite of the stored values.
 */
public record SettingsSnapshot(DetectionSettings values, List<String> warnings) {
    public SettingsSnapshot {
        values = Objects.requireNonNull(values, "values");
        warnings = List.copyOf(Objects.requireNonNull(warnings, "warnings"));
    }

    public boolean hasWarnings() {
        return !warnings.isEmpty();
    }
}
