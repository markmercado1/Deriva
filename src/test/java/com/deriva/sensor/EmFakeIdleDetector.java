package com.deriva.sensor;

import java.time.Duration;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Objects;

/** Returns queued idle durations, then {@link Duration#ZERO} for later calls. */
public final class EmFakeIdleDetector implements EmIdleDetector {
    private final Deque<Duration> durations;

    public EmFakeIdleDetector(Duration... durations) {
        this(List.of(durations));
    }

    public EmFakeIdleDetector(List<Duration> durations) {
        Objects.requireNonNull(durations, "durations");
        this.durations = new ArrayDeque<>(durations.size());
        for (Duration duration : durations) {
            Duration value = Objects.requireNonNull(duration, "duration");
            if (value.isNegative()) throw new IllegalArgumentException("duration");
            this.durations.addLast(value);
        }
    }

    @Override
    public Duration idleTime() {
        Duration next = durations.pollFirst();
        return next == null ? Duration.ZERO : next;
    }

    public int remaining() {
        return durations.size();
    }
}
