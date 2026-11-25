package com.deriva.app.usecase;

import com.deriva.analysis.DayWindow;
import com.deriva.goals.DriftSignal;
import com.deriva.goals.DriftStateProvider;
import com.deriva.goals.GoalAlertPolicy;
import com.deriva.goals.GoalProgressCalculator;
import com.deriva.goals.OpenSegmentProvider;
import com.deriva.goals.model.Goal;
import com.deriva.goals.model.GoalAlert;
import com.deriva.goals.model.GoalMetric;
import com.deriva.goals.model.GoalProgress;
import com.deriva.privacy.ObservationKind;
import com.deriva.storage.port.GoalAlertRepository;
import com.deriva.storage.port.GoalRepository;
import com.deriva.storage.port.SegmentRepository;
import com.deriva.tracking.model.ContextKey;
import com.deriva.tracking.model.Segment;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Application service for the daily goal alarm. It evaluates active goals with
 * {@link GoalProgressCalculator}, records the thresholds that are new today in
 * {@code goal_alerts}, and notifies the tray once per goal and evaluation.
 *
 * <p>Deduplication lives in the database, not in memory, so a restart on the same
 * day does not repeat an already recorded threshold. The open segment is supplied
 * by {@link OpenSegmentProvider}; this service never knows about the capture loop.</p>
 *
 * <p>A notice is deferred while {@link DriftStateProvider} reports an active drift
 * episode, up to a configured maximum. The later outcome is an observation, not
 * evidence that the notice caused anything.</p>
 */
public final class GoalAlertService {
    /** Initial maximum wait while a drift episode is active. */
    public static final Duration DEFAULT_DEFERRAL_MAX = Duration.ofMinutes(5);
    /** Fixed window used to observe what happened after a notice. */
    public static final Duration OBSERVATION_WINDOW = Duration.ofMinutes(2);

    private final GoalRepository goals;
    private final SegmentRepository segments;
    private final GoalAlertRepository alerts;
    private final GoalAlertNotifier notifier;
    private final Clock clock;
    private final ZoneId zone;
    private final Map<Long, ObservationStart> observations = new HashMap<>();

    public GoalAlertService(GoalRepository goals, SegmentRepository segments,
                            GoalAlertRepository alerts, GoalAlertNotifier notifier,
                            Clock clock, ZoneId zone) {
        this.goals = Objects.requireNonNull(goals, "goals");
        this.segments = Objects.requireNonNull(segments, "segments");
        this.alerts = Objects.requireNonNull(alerts, "alerts");
        this.notifier = Objects.requireNonNull(notifier, "notifier");
        this.clock = Objects.requireNonNull(clock, "clock");
        this.zone = Objects.requireNonNull(zone, "zone");
    }

    /** Evaluates using the injected clock and no drift, as the pre-F6B-5 API did. */
    public List<GoalAlert> evaluate(OpenSegmentProvider openSegments) {
        return evaluate(clock.instant(), openSegments, DriftStateProvider.inactive(),
                DEFAULT_DEFERRAL_MAX);
    }

    /** Compatibility overload used before the deferred alarm existed. */
    public List<GoalAlert> evaluate(Instant now, OpenSegmentProvider openSegments) {
        return evaluate(now, openSegments, DriftStateProvider.inactive(), DEFAULT_DEFERRAL_MAX);
    }

    /**
     * Evaluates one instant. It returns the alerts created by this call, oldest
     * threshold first. Storage failures propagate so the caller can log them and
     * keep the capture loop alive.
     */
    public List<GoalAlert> evaluate(Instant now, OpenSegmentProvider openSegments,
                                    DriftStateProvider drift, Duration deferralMax) {
        Objects.requireNonNull(now, "now");
        Objects.requireNonNull(openSegments, "openSegments");
        Objects.requireNonNull(drift, "drift");
        Objects.requireNonNull(deferralMax, "deferralMax");
        if (deferralMax.isZero() || deferralMax.isNegative())
            throw new IllegalArgumentException("deferralMax");

        LocalDate day = now.atZone(zone).toLocalDate();
        DayWindow window = DayWindow.of(day, zone);
        List<Goal> active = goals.listActive();

        List<Segment> daySegments = new ArrayList<>(segments.between(window.start(), window.end()));
        openSegments.openSegment(now).ifPresent(open -> {
            if (open.kind() == ObservationKind.OBSERVED && window.overlaps(open))
                daySegments.add(open);
        });

        List<GoalProgress> progress =
                GoalProgressCalculator.calculate(day, zone, daySegments, active);
        List<GoalAlert> today = new ArrayList<>();
        for (Goal goal : active) {
            for (GoalAlert alert : alerts.listByGoal(goal.id())) {
                if (!alert.triggeredAt().isBefore(window.start())
                        && alert.triggeredAt().isBefore(window.end()))
                    today.add(alert);
            }
        }

        List<GoalAlert> created = new ArrayList<>();
        for (GoalAlertPolicy.Plan plan : GoalAlertPolicy.plan(progress, today)) {
            for (int threshold : plan.thresholds()) {
                created.add(alerts.create(
                        GoalAlert.draft(plan.goal().id(), now, threshold, plan.metric())));
            }
        }

        settleObservations(now, drift);
        deliverDueNotices(now, drift, deferralMax, progress);
        return List.copyOf(created);
    }

    /**
     * Finishes observations left open by a previous run and discards notices that
     * were never delivered. A notice from a former session is old and out of
     * context, so it is closed without showing it. Pausing and resuming inside the
     * same session does not call this method.
     */
    public int recoverAfterRestart(Instant now) {
        Objects.requireNonNull(now, "now");
        List<GoalAlert> pending = alerts.listPending();
        List<GoalAlert> updates = new ArrayList<>();
        for (GoalAlert alert : pending) {
            if (alert.isPendingNotice())
                updates.add(alert.observed(GoalAlert.OUTCOME_DISCARDED, now, null));
            else if (alert.isObserving())
                updates.add(alert.observed(GoalAlert.OUTCOME_NOT_OBSERVED, now, null));
        }
        if (!updates.isEmpty()) alerts.updateAll(updates);
        observations.clear();
        return updates.size();
    }

    /**
     * Marks open observations as not observed without touching notices that are
     * still waiting to be delivered. Used by pause, lock or suspension boundaries.
     */
    public int interruptObservations(Instant now) {
        Objects.requireNonNull(now, "now");
        List<GoalAlert> updates = alerts.listPending().stream()
                .filter(GoalAlert::isObserving)
                .map(alert -> alert.observed(GoalAlert.OUTCOME_NOT_OBSERVED, now, null))
                .toList();
        if (!updates.isEmpty()) {
            alerts.updateAll(updates);
            for (GoalAlert alert : updates) observations.remove(alert.id());
        }
        return updates.size();
    }

    private void settleObservations(Instant now, DriftStateProvider drift) {
        List<GoalAlert> updates = new ArrayList<>();
        for (GoalAlert alert : alerts.listPending()) {
            if (!alert.isObserving() || now.isBefore(alert.observedUntil())) continue;
            ObservationStart start = observations.remove(alert.id());
            updates.add(resolve(alert, start, drift.current(), now));
        }
        if (!updates.isEmpty()) alerts.updateAll(updates);
    }

    private static GoalAlert resolve(GoalAlert alert, ObservationStart start,
                                     DriftSignal signal, Instant now) {
        if (start == null)
            return alert.observed(GoalAlert.OUTCOME_NOT_OBSERVED, now, null);

        if (signal.active()) {
            String outcome = start.driftActive()
                    ? GoalAlert.OUTCOME_STILL_DRIFTING
                    : GoalAlert.OUTCOME_NEW_DRIFT;
            return alert.observed(outcome, now, null);
        }
        if (signal.generation() > start.generation())
            return alert.observed(GoalAlert.OUTCOME_NEW_DRIFT, now, null);
        if (!start.driftActive())
            return alert.observed(GoalAlert.OUTCOME_NO_DRIFT, now, null);

        return alert.observed(GoalAlert.OUTCOME_STABLE, now,
                millisFromNotice(alert.notifiedAt(), signal.changedAt()));
    }

    private static long millisFromNotice(Instant noticeAt, Instant changedAt) {
        if (noticeAt == null || changedAt == null || changedAt.isBefore(noticeAt)) return 0;
        return Duration.between(noticeAt, changedAt).toMillis();
    }

    private void deliverDueNotices(Instant now, DriftStateProvider drift,
                                   Duration deferralMax, List<GoalProgress> progress) {
        DriftSignal signal = drift.current();
        List<GoalAlert> due = new ArrayList<>();
        for (GoalAlert alert : alerts.listPending()) {
            if (!alert.isPendingNotice()) continue;
            boolean capReached = !now.isBefore(alert.triggeredAt().plus(deferralMax));
            if (!signal.active() || capReached) due.add(alert);
        }
        if (due.isEmpty()) return;

        List<GoalAlert> notified = new ArrayList<>(due.size());
        for (GoalAlert alert : due) {
            GoalAlert delivered = alert.notified(now, now.plus(OBSERVATION_WINDOW));
            notified.add(delivered);
            observations.put(delivered.id(), new ObservationStart(
                    signal.active(), signal.generation()));
        }
        alerts.updateAll(notified);

        List<Goal> allGoals = goals.listAll();
        Map<Long, Goal> goalsById = new HashMap<>();
        for (Goal goal : allGoals) goalsById.put(goal.id(), goal);
        Map<Long, GoalProgress> progressById = new HashMap<>();
        for (GoalProgress item : progress) progressById.put(item.goal().id(), item);

        Map<Long, List<GoalAlert>> byGoal = new LinkedHashMap<>();
        for (GoalAlert alert : notified)
            byGoal.computeIfAbsent(alert.goalId(), ignored -> new ArrayList<>()).add(alert);
        for (Map.Entry<Long, List<GoalAlert>> entry : byGoal.entrySet()) {
            Goal goal = goalsById.get(entry.getKey());
            if (goal == null) continue;
            notify(goal, entry.getValue(), progressById.get(goal.id()));
        }
    }

    private void notify(Goal goal, List<GoalAlert> alertsOfGoal, GoalProgress progress) {
        try {
            notifier.notifyGoal(notice(goal, alertsOfGoal, progress));
        } catch (RuntimeException e) {
            System.err.println("No se pudo mostrar el aviso de objetivo ("
                    + e.getClass().getSimpleName() + ").");
        }
    }

    private static GoalAlertNotice notice(Goal goal, List<GoalAlert> alertsOfGoal,
                                          GoalProgress progress) {
        int minutesThreshold = highestThreshold(alertsOfGoal, GoalMetric.MINUTES);
        int entriesThreshold = highestThreshold(alertsOfGoal, GoalMetric.ENTRIES);
        List<String> parts = new ArrayList<>(2);
        if (entriesThreshold > 0)
            parts.add(part(GoalMetric.ENTRIES, entriesThreshold, goal, progress));
        if (minutesThreshold > 0)
            parts.add(part(GoalMetric.MINUTES, minutesThreshold, goal, progress));
        String body = contextLabel(goal.context()) + ": " + String.join(" y ", parts)
                + ". Aviso del límite diario.";
        return new GoalAlertNotice("Objetivo diario", body);
    }

    private static int highestThreshold(List<GoalAlert> alertsOfGoal, GoalMetric metric) {
        return alertsOfGoal.stream()
                .filter(alert -> alert.metric() == metric)
                .mapToInt(GoalAlert::thresholdPercent)
                .max()
                .orElse(0);
    }

    private static String part(GoalMetric metric, int threshold, Goal goal,
                               GoalProgress progress) {
        if (metric == GoalMetric.MINUTES) {
            String used = progress == null || goal.dailyMinutes() == null
                    ? ""
                    : " (" + progress.usedMinutes() + " de " + goal.dailyMinutes() + " min)";
            return threshold + " % de minutos" + used;
        }
        String used = progress == null || goal.dailyEntries() == null
                ? ""
                : " (" + progress.entries() + " de " + goal.dailyEntries() + " entradas)";
        return threshold + " % de entradas" + used;
    }

    private static String contextLabel(ContextKey context) {
        return context.siteHost() == null
                ? context.processName()
                : context.processName() + " · " + context.siteHost();
    }

    private record ObservationStart(boolean driftActive, long generation) {
    }
}
