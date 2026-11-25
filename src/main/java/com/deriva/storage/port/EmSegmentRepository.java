package com.deriva.storage.port;

import com.deriva.tracking.model.EmContextKey;
import com.deriva.tracking.model.EmSegment;

import java.time.Instant;
import java.util.List;

/** Persistence port for segments; implementations must not leak JDBC into the core. */
public interface EmSegmentRepository {
    void saveAll(List<EmSegment> segments);

    List<EmSegment> between(Instant from, Instant to);

    /** Distinct observed contexts already present in the record, without counts or times. */
    List<EmContextKey> observedContexts();
}
