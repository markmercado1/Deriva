package com.deriva.privacy;

/** Result of applying privacy rules to one sample, before any tracking context exists. */
public enum EmObservationKind {
    OBSERVED,
    IDLE,
    EXCLUDED,
    UNAVAILABLE
}
