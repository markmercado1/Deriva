package com.deriva.app.usecase;

import com.deriva.coach.model.DetectionSettings;

/** UI-facing port for starting, stopping and observing the real capture. */
public interface CaptureControl {
    CaptureStatus status();

    CaptureSnapshot snapshot();

    /** Values used by the current session, or the saved values when stopped. */
    DetectionSettings effectiveSettings();

    void start();

    void stop();

    /**
     * Stops sampling and closes the open segment/episode with reason PAUSE.
     * {@link #start()} resumes a new sampling loop from that point.
     */
    void pause();

    /** Stops capture, waits for it, and deletes activity records only. */
    void deleteAllRecords();

    void shutdown();
}
