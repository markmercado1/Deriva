package com.deriva.ui.viewmodel;

import com.deriva.goals.model.GoalState;

/** Presentation row for one persisted goal on the current local day. */
public record GoalRow(
        long id,
        String contextLabel,
        String limitsText,
        String progressText,
        GoalState state,
        String stateLabel,
        String stateIcon,
        String stateKey,
        double ratio,
        boolean active,
        boolean passive,
        boolean unmeasurable) {

    public GoalRow {
        if (id <= 0) throw new IllegalArgumentException("id");
        if (ratio < 0 || ratio > 1) throw new IllegalArgumentException("ratio");
    }
}
