package com.deriva.app.usecase;

import com.deriva.coach.model.DetectionSettings;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

class CaptureSettingsPolicyTest {
    private static final DetectionSettings SESSION = new DetectionSettings(10,
            Duration.ofSeconds(120), Duration.ofSeconds(180), Duration.ofSeconds(120));
    private static final DetectionSettings SAVED = new DetectionSettings(8,
            Duration.ofSeconds(90), Duration.ofSeconds(240), Duration.ofSeconds(300));

    @Test
    void pauseResumeKeepsSessionSettings() {
        assertEquals(SESSION, CaptureSettingsPolicy.onStart(CaptureState.PAUSED, SESSION, SAVED));
    }

    @Test
    void stoppedOrErrorLoadsSavedSettings() {
        assertEquals(SAVED, CaptureSettingsPolicy.onStart(CaptureState.STOPPED, SESSION, SAVED));
        assertEquals(SAVED, CaptureSettingsPolicy.onStart(CaptureState.ERROR, SESSION, SAVED));
    }

    @Test
    void resumeWithoutSessionFallsBackToSaved() {
        assertEquals(SAVED, CaptureSettingsPolicy.onStart(CaptureState.PAUSED, null, SAVED));
    }
}
