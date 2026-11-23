package com.deriva.app.usecase;

/** UI-facing port for starting, stopping and observing the real capture. */
public interface CaptureControl {
    CaptureStatus status();

    CaptureSnapshot snapshot();

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
