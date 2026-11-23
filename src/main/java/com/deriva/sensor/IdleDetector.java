package com.deriva.sensor;

import java.time.Duration;

/** Port for reading how long the user has been idle. */
@FunctionalInterface
public interface IdleDetector {
    Duration idleTime();
}
