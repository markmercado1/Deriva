package com.deriva.ui.viewmodel;

import com.deriva.coach.model.CloseReason;
import com.deriva.coach.model.DriftStatus;
import com.deriva.tracking.model.ContextKey;

import java.time.Instant;
import java.util.List;

public record InterruptionRow(
        long id,
        Instant startTs,
        String startText,
        DriftStatus status,
        String statusText,
        String firstChange,
        int switchCount,
        String stabilityText,
        String reasonCode,
        String reasonText,
        boolean justified,
        List<ContextKey> contexts,
        CloseReason closeReason) {
}
