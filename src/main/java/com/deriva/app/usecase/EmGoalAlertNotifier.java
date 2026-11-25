package com.deriva.app.usecase;

/** Outbound port for goal notifications; the app adapts it to the tray. */
@FunctionalInterface
public interface EmGoalAlertNotifier {
    void notifyGoal(EmGoalAlertNotice notice);
}
