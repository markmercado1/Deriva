package com.deriva.goals.model;

/** Progress state of a goal. The UI maps these values to Spanish labels. */
public enum GoalState {
    /** Below the near threshold of every configured limit. */
    IN_PROGRESS,
    /** At least one limit reached 80 % but none reached 100 %. */
    NEAR,
    /** At least one configured limit reached or passed 100 %. */
    EXCEEDED
}
