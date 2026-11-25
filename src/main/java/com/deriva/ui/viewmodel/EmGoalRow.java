package com.deriva.ui.viewmodel;

import com.deriva.goals.model.EmGoalState;

import java.util.List;
import java.util.Objects;

/** Presentation row for one persisted goal on a local day or a period; archived rows are read-only. */
public record EmGoalRow(
        long id,
        String contextLabel,
        String limitsText,
        String progressText,
        EmGoalState state,
        String stateLabel,
        String stateIcon,
        String stateKey,
        double ratio,
        boolean active,
        boolean passive,
        boolean unmeasurable,
        EmGoalJustificationView justification,
        String alertOutcomeText,
        String balanceText,
        List<String> alertLines,
        String configurationNote,
        boolean archived) {

    public EmGoalRow {
        if (id <= 0) throw new IllegalArgumentException("id");
        if (ratio < 0 || ratio > 1) throw new IllegalArgumentException("ratio");
        alertLines = List.copyOf(Objects.requireNonNull(alertLines, "alertLines"));
    }
}
