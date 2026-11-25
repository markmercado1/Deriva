package com.deriva.coach;

import com.deriva.coach.model.EmDriftUpdate;
import com.deriva.tracking.model.EmStableTick;

import java.util.List;

/** Pure per-tick drift detector; no threads, storage, UI or side effects. */
@FunctionalInterface
public interface EmDriftDetector {
    List<EmDriftUpdate> onTick(EmStableTick tick);
}
