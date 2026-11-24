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

    /** Physically deletes the goal and cascades to its alerts; false when it did not exist. */
    boolean delete(long id);

    List<Goal> listActive();

    /** All goals, active and inactive, in stable context order. */
    List<Goal> listAll();
}
