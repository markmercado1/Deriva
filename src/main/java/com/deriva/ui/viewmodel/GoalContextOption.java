package com.deriva.ui.viewmodel;

import com.deriva.tracking.model.ContextKey;

import java.util.Objects;

/** One context already seen in the record, selectable when creating a goal. */
public record GoalContextOption(ContextKey context, String label) {
    public GoalContextOption {
        context = Objects.requireNonNull(context, "context");
        label = Objects.requireNonNull(label, "label");
    }

    @Override
    public String toString() {
        return label;
    }
}
