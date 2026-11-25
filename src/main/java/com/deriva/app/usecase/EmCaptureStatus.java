package com.deriva.app.usecase;

import java.util.Objects;

/** Visible state of the real capture session. */
public record EmCaptureStatus(EmCaptureState state, String message) {
    public EmCaptureStatus {
        state = Objects.requireNonNull(state, "state");
        message = message == null ? "" : message;
    }

    public static EmCaptureStatus stopped() {
        return new EmCaptureStatus(EmCaptureState.STOPPED, "Captura detenida.");
    }

    public static EmCaptureStatus paused() {
        return new EmCaptureStatus(EmCaptureState.PAUSED, "Captura en pausa.");
    }
}
