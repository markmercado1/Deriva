package com.deriva.app.usecase;

import com.deriva.tracking.model.Segment;

import java.time.Instant;

/**
 * Coherent live view published by the capture thread. Open is already confirmed
 * but not persisted; pending is not counted until confirmed. Neither is closed
 * merely to refresh the UI.
 */
public record CaptureSnapshot(Instant capturedAt, Segment open, Segment pending,
                              boolean captureRunning) {
    public static CaptureSnapshot stopped() {
        return new CaptureSnapshot(null, null, null, false);
    }

    public boolean hasLiveActivity() {
        return captureRunning && (open != null || pending != null);
    }
}
