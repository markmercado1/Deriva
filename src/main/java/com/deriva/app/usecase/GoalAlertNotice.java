package com.deriva.app.usecase;

/**
 * Neutral text ready for the tray. It only describes what was used and the limit;
 * it never classifies the context or blames the user.
 */
public record GoalAlertNotice(String title, String body) {
    public GoalAlertNotice {
        if (title == null || title.isBlank()) throw new IllegalArgumentException("title");
        if (body == null || body.isBlank()) throw new IllegalArgumentException("body");
    }
}
