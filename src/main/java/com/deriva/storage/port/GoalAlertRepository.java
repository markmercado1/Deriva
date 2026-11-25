package com.deriva.storage.port;

import com.deriva.goals.model.GoalAlert;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/** Persistence port for goal alerts; implementations must not leak JDBC. */
public interface GoalAlertRepository {
    /** Stores a new alert (id 0) and returns it with its id. */
    GoalAlert create(GoalAlert alert);

    Optional<GoalAlert> find(long id);

    void update(GoalAlert alert);

    /** Updates all supplied alerts in one transaction. */
    void updateAll(List<GoalAlert> alerts);

    boolean delete(long id);

    List<GoalAlert> listByGoal(long goalId);

    /** Alerts whose outcome has not been recorded yet, oldest first. */
    List<GoalAlert> listPending();

    /** Alerts triggered in the semi-open interval {@code [from, to)}. */
    List<GoalAlert> listBetween(Instant from, Instant to);

    /** Alerts justified in the semi-open interval {@code [from, to)}, by moment. */
    List<GoalAlert> listJustifiedBetween(Instant from, Instant to);
}
