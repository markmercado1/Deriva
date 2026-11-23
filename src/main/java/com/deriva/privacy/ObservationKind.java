package com.deriva.privacy;

/** Result of applying privacy rules to one sample, before any tracking context exists. */
public enum ObservationKind {
    OBSERVED,
    IDLE,
    EXCLUDED,
    UNAVAILABLE
}
