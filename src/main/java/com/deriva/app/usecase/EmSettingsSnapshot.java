package com.deriva.app.usecase;

import com.deriva.coach.model.EmDetectionSettings;

import java.util.List;
import java.util.Objects;

/**
 * Values actually in use and warnings produced while reading persisted settings.
 * Warnings never cause a silent overwrite of the stored values.
 */
public record EmSettingsSnapshot(EmDetectionSettings values, List<String> warnings) {
    public EmSettingsSnapshot {
        values = Objects.requireNonNull(values, "values");
        warnings = List.copyOf(Objects.requireNonNull(warnings, "warnings"));
    }

    public boolean hasWarnings() {
        return !warnings.isEmpty();
    }
}
