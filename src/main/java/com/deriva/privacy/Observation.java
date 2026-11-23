package com.deriva.privacy;

import java.time.Instant;

/**
 * Sanitized observation produced by {@link PrivacyFilter}.
 *
 * <p>Only {@link ObservationKind#OBSERVED} may carry an identity. Excluded, idle and
 * unavailable observations keep both identity fields null so the previous context can
 * never be reused for a private or unknown situation.</p>
 */
public record Observation(Instant timestamp, ObservationKind kind, String processName, String siteHost) {
    public Observation {
        if (timestamp == null || kind == null) throw new IllegalArgumentException("timestamp/kind");
        if (kind == ObservationKind.OBSERVED) {
            if (processName == null || processName.isBlank()) throw new IllegalArgumentException("processName");
            processName = processName.trim();
            if (processName.indexOf('/') >= 0 || processName.indexOf('\\') >= 0)
                throw new IllegalArgumentException("processName must be a file name");
            if (processName.chars().anyMatch(Character::isISOControl))
                throw new IllegalArgumentException("processName");
            if (siteHost != null && siteHost.isBlank()) throw new IllegalArgumentException("siteHost");
        } else if (processName != null || siteHost != null) {
            throw new IllegalArgumentException("Non-observed observation cannot carry identity");
        }
    }
}
