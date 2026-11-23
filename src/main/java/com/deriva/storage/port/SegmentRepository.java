package com.deriva.storage.port;

import com.deriva.tracking.model.Segment;

import java.time.Instant;
import java.util.List;

/** Persistence port for segments; implementations must not leak JDBC into the core. */
public interface SegmentRepository {
    void saveAll(List<Segment> segments);

    List<Segment> between(Instant from, Instant to);
}
