package com.deriva.app.usecase;

import com.deriva.coach.model.DetectionSettings;

/**
 * Effective settings belong to a capture session. Pausing and resuming keeps the
 * session values; only a new start from STOPPED or ERROR reloads the saved ones.
 */
public final class CaptureSettingsPolicy {
    private CaptureSettingsPolicy() {}

    public static DetectionSettings onStart(CaptureState state, DetectionSettings sessionSettings,
                                            DetectionSettings savedSettings) {
        if (state == CaptureState.PAUSED && sessionSettings != null) return sessionSettings;
        return savedSettings;
    }

    /** Same rule for the passive-activity maximum. */
    public static java.time.Duration onStart(CaptureState state, java.time.Duration sessionValue,
                                             java.time.Duration savedValue) {
        if (state == CaptureState.PAUSED && sessionValue != null) return sessionValue;
        return savedValue;
    }
}
