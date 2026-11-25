package com.deriva.app.usecase;

import com.deriva.tracking.model.EmSegment;

import java.time.Instant;

/**
 * Coherent live view published by the capture thread. Open is already confirmed
 * but not persisted; pending is not counted until confirmed. Neither is closed
 * merely to refresh the UI.
 */
public record EmCaptureSnapshot(Instant capturedAt, EmSegment open, EmSegment pending,
                              boolean captureRunning) {
    public static EmCaptureSnapshot stopped() {
        return new EmCaptureSnapshot(null, null, null, false);
    }

    public boolean hasLiveActivity() {
        return captureRunning && (open != null || pending != null);
    }
}
