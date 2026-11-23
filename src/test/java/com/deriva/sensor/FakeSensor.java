package com.deriva.sensor;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Objects;

/** Deterministic test double that returns queued samples in order. */
public final class FakeSensor implements WindowSensor {
    private final Deque<ActivitySample> samples;

    public FakeSensor(ActivitySample... samples) {
        this(List.of(samples));
    }

    public FakeSensor(List<ActivitySample> samples) {
        Objects.requireNonNull(samples, "samples");
        this.samples = new ArrayDeque<>(samples.size());
        for (ActivitySample sample : samples) this.samples.addLast(Objects.requireNonNull(sample, "sample"));
    }

    @Override
    public ActivitySample sample() {
        ActivitySample next = samples.pollFirst();
        if (next == null) throw new IllegalStateException("No more samples");
        return next;
    }

    public int remaining() {
        return samples.size();
    }
}
