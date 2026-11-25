package com.deriva.goals;

/**
 * Read-only port used by the goal alarm to know whether a drift episode is active.
 * The drift state machine never depends on the alarm or on this port.
 */
@FunctionalInterface
public interface EmDriftStateProvider {
    EmDriftSignal current();

    static EmDriftStateProvider inactive() {
        return EmDriftSignal::inactive;
    }
}
