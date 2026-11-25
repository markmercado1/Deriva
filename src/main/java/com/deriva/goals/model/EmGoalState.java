package com.deriva.goals.model;

/** Progress state of a goal. The UI maps these values to Spanish labels. */
public enum EmGoalState {
    /** Below the near threshold of every configured limit. */
    IN_PROGRESS,
    /** At least one limit reached 80 % but none reached 100 %. */
    NEAR,
    /** The current excess over 100 % is covered by a justification for the day. */
    JUSTIFIED,
    /** At least one configured limit reached or passed 100 % without full coverage. */
    EXCEEDED
}
