package com.deriva.ui.viewmodel;

import com.deriva.coach.model.CloseReason;
import com.deriva.coach.model.DriftStatus;
import com.deriva.tracking.model.ContextKey;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

public record InterruptionRow(
        long id,
        Instant startTs,
        String startText,
        DriftStatus status,
        String statusText,
        String firstChange,
        int switchCount,
        String stabilityText,
        String annotationText,
        Instant answeredAt,
        String reasonCode,
        String reasonText,
        boolean justified,
        List<ContextKey> contexts,
        CloseReason closeReason) {
    public boolean hasAnnotationChanges(String reason, boolean isJustified) {
        return reason != null && (answeredAt == null
                || !Objects.equals(reasonCode, reason) || justified != isJustified);
    }
}
