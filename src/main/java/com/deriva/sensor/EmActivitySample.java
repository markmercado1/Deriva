package com.deriva.sensor;

import java.time.Instant;

/**
 * One foreground observation at a point in time.
 *
 * <p>An available sample identifies the foreground process and window. An unavailable
 * sample carries no identity at all: privacy filtering runs before persistence, so an
 * unknown observation must never inherit the previous context.</p>
 */
public record EmActivitySample(
        Instant timestamp,
        String processName,
        long pid,
        long foregroundWindowId,
        boolean idle,
        boolean available) {

    public EmActivitySample {
        if (timestamp == null) throw new IllegalArgumentException("timestamp");
        if (available) {
            if (processName == null || processName.isBlank()) throw new IllegalArgumentException("processName");
            processName = processName.trim();
            if (processName.indexOf('/') >= 0 || processName.indexOf('\\') >= 0)
                throw new IllegalArgumentException("processName must be a file name");
            if (processName.chars().anyMatch(Character::isISOControl))
                throw new IllegalArgumentException("processName");
            if (pid <= 0) throw new IllegalArgumentException("pid");
            if (foregroundWindowId == 0) throw new IllegalArgumentException("foregroundWindowId");
        } else if (processName != null || pid != 0 || foregroundWindowId != 0 || idle) {
            throw new IllegalArgumentException("Unavailable sample cannot carry identity or idle state");
        }
    }
}
