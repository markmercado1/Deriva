package com.deriva.app.usecase;

import java.time.Duration;
import java.util.List;
import java.util.Objects;

/**
 * Persisted maximum wait for a deferred goal notice, with warnings read alongside it.
 * The effective value of a running capture session is kept separately, following the
 * same rule as the detection settings and the passive-activity maximum.
 */
public record AlertDeferralSnapshot(Duration value, List<String> warnings) {
    public AlertDeferralSnapshot {
        value = Objects.requireNonNull(value, "value");
        warnings = List.copyOf(Objects.requireNonNull(warnings, "warnings"));
    }

    public boolean hasWarnings() {
        return !warnings.isEmpty();
    }
}
