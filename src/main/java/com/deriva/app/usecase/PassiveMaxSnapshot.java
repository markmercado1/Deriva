package com.deriva.app.usecase;

import java.time.Duration;
import java.util.List;
import java.util.Objects;

/**
 * Persisted maximum for passive activity and warnings read with it. The effective
 * value of a running capture session is kept separately, as with detection settings.
 */
public record PassiveMaxSnapshot(Duration value, List<String> warnings) {
    public PassiveMaxSnapshot {
        value = Objects.requireNonNull(value, "value");
        warnings = List.copyOf(Objects.requireNonNull(warnings, "warnings"));
    }

    public boolean hasWarnings() {
        return !warnings.isEmpty();
    }
}
