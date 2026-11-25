package com.deriva.ui.viewmodel;

/**
 * Presentation data for the user's justification of one goal excess.
 *
 * <p>{@code reason} is the stored text exactly as the user wrote it. {@code note}
 * explains the visible state without adding a cause or a verification claim.</p>
 */
public record GoalJustificationView(
        String reason,
        String coverage,
        String note,
        boolean canJustify,
        String actionLabel,
        boolean canRemove) {

    public GoalJustificationView {
        if (canJustify && (actionLabel == null || actionLabel.isBlank()))
            throw new IllegalArgumentException("actionLabel");
    }
}
