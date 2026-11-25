package com.deriva.ui.viewmodel;

import com.deriva.tracking.model.EmContextKey;

import java.util.Objects;

/** One context already seen in the record, selectable when creating a goal. */
public record EmGoalContextOption(EmContextKey context, String label) {
    public EmGoalContextOption {
        context = Objects.requireNonNull(context, "context");
        label = Objects.requireNonNull(label, "label");
    }

    @Override
    public String toString() {
        return label;
    }
}
