package com.deriva.storage.port;

import com.deriva.coach.model.EmDriftEvent;
import com.deriva.coach.model.EmDriftUpdate;

import java.time.Instant;
import java.util.List;

/** Persistence port for drift episodes, contexts and annotations. */
public interface EmDriftRepository {
    void upsertDetection(EmDriftUpdate update);

    void annotate(long id, String reason, boolean justified, Instant answeredAt);

    List<EmDriftEvent> between(Instant from, Instant to);

    /** Converts persisted OPEN episodes to UNCONFIRMED/RESTART; returns how many changed. */
    int recoverOpenEpisodes();
}
