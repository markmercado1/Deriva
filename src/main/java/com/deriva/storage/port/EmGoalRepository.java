package com.deriva.storage.port;

import com.deriva.goals.model.EmGoal;

import java.util.List;
import java.util.Optional;

/** Persistence port for user-defined goals; implementations must not leak JDBC. */
public interface EmGoalRepository {
    /** Stores a new goal (id 0) and returns it with its id and timestamps. */
    EmGoal create(EmGoal goal);

    Optional<EmGoal> find(long id);

    /** Updates an existing goal and refreshes its modification time. */
    void update(EmGoal goal);

    /** Archives the goal, preserving configuration history and alerts. */
    boolean delete(long id);

    List<EmGoal> listActive();

    /** All goals, active and inactive, in stable context order. */
    List<EmGoal> listAll();

    /** Configuration events strictly before the instant, ordered by time and revision. */
    default List<com.deriva.goals.model.EmGoalRevision> revisionsBefore(java.time.Instant end) {
        return List.of();
    }
}
