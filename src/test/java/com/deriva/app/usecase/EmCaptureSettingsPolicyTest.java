package com.deriva.app.usecase;

import com.deriva.coach.model.EmDetectionSettings;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

class EmCaptureSettingsPolicyTest {
    private static final EmDetectionSettings SESSION = new EmDetectionSettings(10,
            Duration.ofSeconds(120), Duration.ofSeconds(180), Duration.ofSeconds(120));
    private static final EmDetectionSettings SAVED = new EmDetectionSettings(8,
            Duration.ofSeconds(90), Duration.ofSeconds(240), Duration.ofSeconds(300));

    @Test
    void pauseResumeKeepsSessionSettings() {
        assertEquals(SESSION, EmCaptureSettingsPolicy.onStart(EmCaptureState.PAUSED, SESSION, SAVED));
    }

    @Test
    void stoppedOrErrorLoadsSavedSettings() {
        assertEquals(SAVED, EmCaptureSettingsPolicy.onStart(EmCaptureState.STOPPED, SESSION, SAVED));
        assertEquals(SAVED, EmCaptureSettingsPolicy.onStart(EmCaptureState.ERROR, SESSION, SAVED));
    }

    @Test
    void resumeWithoutSessionFallsBackToSaved() {
        assertEquals(SAVED, EmCaptureSettingsPolicy.onStart(EmCaptureState.PAUSED, null, SAVED));
    }
}
