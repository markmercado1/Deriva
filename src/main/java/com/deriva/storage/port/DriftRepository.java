package com.deriva.storage.port;

import com.deriva.coach.model.DriftEvent;
import com.deriva.coach.model.DriftUpdate;

import java.time.Instant;
import java.util.List;

/** Persistence port for drift episodes, contexts and annotations. */
public interface DriftRepository {
    void upsertDetection(DriftUpdate update);

    void annotate(long id, String reason, boolean justified, Instant answeredAt);

    List<DriftEvent> between(Instant from, Instant to);

    /** Converts persisted OPEN episodes to UNCONFIRMED/RESTART; returns how many changed. */
    int recoverOpenEpisodes();
}
