package com.deriva.goals;

import com.deriva.tracking.model.EmSegment;

import java.time.Instant;
import java.util.Optional;

/**
 * Supplies the confirmed segment in progress, already bounded by {@code now}.
 *
 * <p>It is the application's adapter between the live segmenter and the goal
 * evaluation. The alert service never sees the capture session or the segmenter.</p>
 */
@FunctionalInterface
public interface EmOpenSegmentProvider {
    Optional<EmSegment> openSegment(Instant now);
}
