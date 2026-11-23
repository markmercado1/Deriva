package com.deriva.app.usecase;

import java.util.Objects;

/** Visible state of the real capture session. */
public record CaptureStatus(CaptureState state, String message) {
    public CaptureStatus {
        state = Objects.requireNonNull(state, "state");
        message = message == null ? "" : message;
    }

    public static CaptureStatus stopped() {
        return new CaptureStatus(CaptureState.STOPPED, "Captura detenida.");
    }

    public static CaptureStatus paused() {
        return new CaptureStatus(CaptureState.PAUSED, "Captura en pausa.");
    }
}
