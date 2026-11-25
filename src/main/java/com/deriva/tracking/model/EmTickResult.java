package com.deriva.tracking.model;

import java.util.List;
import java.util.Objects;

/** Result of one segmentation tick: the stable view and any segments closed by it. */
public record EmTickResult(EmStableTick tick, List<EmSegment> completedSegments) {
    public EmTickResult {
        Objects.requireNonNull(tick, "tick");
        completedSegments = List.copyOf(Objects.requireNonNull(completedSegments, "completedSegments"));
    }
}
