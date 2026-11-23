package com.deriva.coach;

import com.deriva.coach.model.DriftUpdate;
import com.deriva.tracking.model.StableTick;

import java.util.List;

/** Pure per-tick drift detector; no threads, storage, UI or side effects. */
@FunctionalInterface
public interface DriftDetector {
    List<DriftUpdate> onTick(StableTick tick);
}
