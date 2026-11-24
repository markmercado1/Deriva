package com.deriva.storage.port;

import com.deriva.goals.model.GoalAlert;

import java.util.List;
import java.util.Optional;

/** Persistence port for goal alerts; implementations must not leak JDBC. */
public interface GoalAlertRepository {
    /** Stores a new alert (id 0) and returns it with its id. */
    GoalAlert create(GoalAlert alert);

    Optional<GoalAlert> find(long id);

    void update(GoalAlert alert);

    boolean delete(long id);

    List<GoalAlert> listByGoal(long goalId);

    /** Alerts whose outcome has not been recorded yet, oldest first. */
    List<GoalAlert> listPending();
}
