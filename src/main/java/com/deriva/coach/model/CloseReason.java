package com.deriva.coach.model;

/** Explicit reason for an episode closed without confirmed stability. */
public enum CloseReason {
    IDLE,
    EXCLUSION,
    UNAVAILABLE,
    PAUSE,
    LOCK,
    EXIT,
    GAP,
    RESTART
}
