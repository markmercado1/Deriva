package com.deriva.ui.viewmodel;

import java.time.LocalDate;
import java.util.List;

/** Display state for the goals screen and its create/edit form. */
public record GoalsPage(
        List<GoalRow> goals,
        List<GoalContextOption> contexts,
        String summary,
        boolean hasGoals,
        boolean editing,
        boolean contextLocked,
        String formTitle,
        String processText,
        String hostText,
        String minutesText,
        String entriesText,
        boolean passive,
        boolean active,
        String validationError,
        boolean canSave,
        LocalDate selectedDate,
        boolean historical,
        String historyNote,
        List<String> revisionLines) {

    public GoalsPage {
        goals = List.copyOf(goals);
        contexts = List.copyOf(contexts);
        revisionLines = List.copyOf(revisionLines);
    }
}
