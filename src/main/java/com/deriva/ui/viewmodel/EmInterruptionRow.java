package com.deriva.ui.viewmodel;

import com.deriva.coach.model.EmCloseReason;
import com.deriva.coach.model.EmDriftStatus;
import com.deriva.tracking.model.EmContextKey;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

public record EmInterruptionRow(
        long id,
        Instant startTs,
        String startText,
        EmDriftStatus status,
        String statusText,
        String firstChange,
        int switchCount,
        String stabilityText,
        String annotationText,
        Instant answeredAt,
        String reasonCode,
        String reasonText,
        boolean justified,
        List<EmContextKey> contexts,
        EmCloseReason closeReason) {
    public boolean hasAnnotationChanges(String reason, boolean isJustified) {
        return reason != null && (answeredAt == null
                || !Objects.equals(reasonCode, reason) || justified != isJustified);
    }
}
