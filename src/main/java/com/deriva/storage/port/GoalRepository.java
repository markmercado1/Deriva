package com.deriva.storage.port;

import com.deriva.goals.model.Goal;

import java.util.List;
import java.util.Optional;

/** Persistence port for user-defined goals; implementations must not leak JDBC. */
public interface GoalRepository {
    /** Stores a new goal (id 0) and returns it with its id and timestamps. */
    Goal create(Goal goal);

    Optional<Goal> find(long id);

    /** Updates an existing goal and refreshes its modification time. */
    void update(Goal goal);

    /** Archives the goal, preserving configuration history and alerts. */
    boolean delete(long id);

    List<Goal> listActive();

    /** All goals, active and inactive, in stable context order. */
    List<Goal> listAll();

    /** Configuration events strictly before the instant, ordered by time and revision. */
    default List<com.deriva.goals.model.GoalRevision> revisionsBefore(java.time.Instant end) {
        return List.of();
    }
}
