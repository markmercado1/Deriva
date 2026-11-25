package com.deriva.ui.viewmodel;

/** One application or site row in the daily usage view. */
public record EmUsageRow(String name, long millis, String durationText, double ratio) {
    public EmUsageRow {
        if (name == null || millis < 0 || ratio < 0 || ratio > 1)
            throw new IllegalArgumentException("Invalid usage row");
    }
}
