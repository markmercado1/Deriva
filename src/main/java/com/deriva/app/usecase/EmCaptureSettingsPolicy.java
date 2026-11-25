package com.deriva.app.usecase;

import com.deriva.coach.model.EmDetectionSettings;

/**
 * Effective settings belong to a capture session. Pausing and resuming keeps the
 * session values; only a new start from STOPPED or ERROR reloads the saved ones.
 */
public final class EmCaptureSettingsPolicy {
    private EmCaptureSettingsPolicy() {}

    public static EmDetectionSettings onStart(EmCaptureState state, EmDetectionSettings sessionSettings,
                                            EmDetectionSettings savedSettings) {
        if (state == EmCaptureState.PAUSED && sessionSettings != null) return sessionSettings;
        return savedSettings;
    }

    /** Same rule for the passive-activity maximum. */
    public static java.time.Duration onStart(EmCaptureState state, java.time.Duration sessionValue,
                                             java.time.Duration savedValue) {
        if (state == EmCaptureState.PAUSED && sessionValue != null) return sessionValue;
        return savedValue;
    }
}
