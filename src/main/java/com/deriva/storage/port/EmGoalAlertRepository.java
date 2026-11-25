package com.deriva.storage.port;

import com.deriva.goals.model.EmGoalAlert;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/** Persistence port for goal alerts; implementations must not leak JDBC. */
public interface EmGoalAlertRepository {
    /** Stores a new alert (id 0) and returns it with its id. */
    EmGoalAlert create(EmGoalAlert alert);

    Optional<EmGoalAlert> find(long id);

    void update(EmGoalAlert alert);

    /** Updates all supplied alerts in one transaction. */
    void updateAll(List<EmGoalAlert> alerts);

    boolean delete(long id);

    List<EmGoalAlert> listByGoal(long goalId);

    /** Alerts of non-archived goals whose outcome has not been recorded yet, oldest first. */
    List<EmGoalAlert> listPending();

    /** Alerts triggered in the semi-open interval {@code [from, to)}. */
    List<EmGoalAlert> listBetween(Instant from, Instant to);

    /** Alerts justified in the semi-open interval {@code [from, to)}, by moment. */
    List<EmGoalAlert> listJustifiedBetween(Instant from, Instant to);
}
