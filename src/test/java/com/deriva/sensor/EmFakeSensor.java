package com.deriva.sensor;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Objects;

/** Deterministic test double that returns queued samples in order. */
public final class EmFakeSensor implements EmWindowSensor {
    private final Deque<EmActivitySample> samples;

    public EmFakeSensor(EmActivitySample... samples) {
        this(List.of(samples));
    }

    public EmFakeSensor(List<EmActivitySample> samples) {
        Objects.requireNonNull(samples, "samples");
        this.samples = new ArrayDeque<>(samples.size());
        for (EmActivitySample sample : samples) this.samples.addLast(Objects.requireNonNull(sample, "sample"));
    }

    @Override
    public EmActivitySample sample() {
        EmActivitySample next = samples.pollFirst();
        if (next == null) throw new IllegalStateException("No more samples");
        return next;
    }

    public int remaining() {
        return samples.size();
    }
}
