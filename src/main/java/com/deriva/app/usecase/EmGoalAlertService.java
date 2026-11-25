package com.deriva.app.usecase;

import com.deriva.analysis.EmDayWindow;
import com.deriva.goals.EmDriftSignal;
import com.deriva.goals.EmDriftStateProvider;
import com.deriva.goals.EmGoalAlertPolicy;
import com.deriva.goals.EmGoalProgressCalculator;
import com.deriva.goals.EmOpenSegmentProvider;
import com.deriva.goals.model.EmGoal;
import com.deriva.goals.model.EmGoalAlert;
import com.deriva.goals.model.EmGoalMetric;
import com.deriva.goals.model.EmGoalProgress;
import com.deriva.privacy.EmObservationKind;
import com.deriva.storage.port.EmGoalAlertRepository;
import com.deriva.storage.port.EmGoalRepository;
import com.deriva.storage.port.EmSegmentRepository;
import com.deriva.tracking.model.EmContextKey;
import com.deriva.tracking.model.EmSegment;

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
 * {@link EmGoalProgressCalculator}, records the thresholds that are new today in
 * {@code goal_alerts}, and notifies the tray once per goal and evaluation.
 *
 * <p>Deduplication lives in the database, not in memory, so a restart on the same
 * day does not repeat an already recorded threshold. The open segment is supplied
 * by {@link EmOpenSegmentProvider}; this service never knows about the capture loop.</p>
 *
 * <p>A notice is deferred while {@link EmDriftStateProvider} reports an active drift
 * episode, up to a configured maximum. The later outcome is an observation, not
 * evidence that the notice caused anything.</p>
 */
public final class EmGoalAlertService {
    /** Initial maximum wait while a drift episode is active. */
    public static final Duration DEFAULT_DEFERRAL_MAX = Duration.ofMinutes(5);
    /** Fixed window used to observe what happened after a notice. */
    public static final Duration OBSERVATION_WINDOW = Duration.ofMinutes(2);

    private final EmGoalRepository goals;
    private final EmSegmentRepository segments;
    private final EmGoalAlertRepository alerts;
    private final EmGoalAlertNotifier notifier;
    private final Clock clock;
    private final ZoneId zone;
    private final Map<Long, ObservationStart> observations = new HashMap<>();

    public EmGoalAlertService(EmGoalRepository goals, EmSegmentRepository segments,
                            EmGoalAlertRepository alerts, EmGoalAlertNotifier notifier,
                            Clock clock, ZoneId zone) {
        this.goals = Objects.requireNonNull(goals, "goals");
        this.segments = Objects.requireNonNull(segments, "segments");
        this.alerts = Objects.requireNonNull(alerts, "alerts");
        this.notifier = Objects.requireNonNull(notifier, "notifier");
        this.clock = Objects.requireNonNull(clock, "clock");
        this.zone = Objects.requireNonNull(zone, "zone");
    }

    /** Evaluates using the injected clock and no drift, as the pre-F6B-5 API did. */
    public List<EmGoalAlert> evaluate(EmOpenSegmentProvider openSegments) {
        return evaluate(clock.instant(), openSegments, EmDriftStateProvider.inactive(),
                DEFAULT_DEFERRAL_MAX);
    }

    /** Compatibility overload used before the deferred alarm existed. */
    public List<EmGoalAlert> evaluate(Instant now, EmOpenSegmentProvider openSegments) {
        return evaluate(now, openSegments, EmDriftStateProvider.inactive(), DEFAULT_DEFERRAL_MAX);
    }

    /**
     * Evaluates one instant. It returns the alerts created by this call, oldest
     * threshold first. Storage failures propagate so the caller can log them and
     * keep the capture loop alive.
     */
    public List<EmGoalAlert> evaluate(Instant now, EmOpenSegmentProvider openSegments,
                                    EmDriftStateProvider drift, Duration deferralMax) {
        Objects.requireNonNull(now, "now");
        Objects.requireNonNull(openSegments, "openSegments");
        Objects.requireNonNull(drift, "drift");
        Objects.requireNonNull(deferralMax, "deferralMax");
        if (deferralMax.isZero() || deferralMax.isNegative())
            throw new IllegalArgumentException("deferralMax");

        LocalDate day = now.atZone(zone).toLocalDate();
        EmDayWindow window = EmDayWindow.of(day, zone);
        List<EmGoal> active = goals.listActive();

        List<EmSegment> daySegments = new ArrayList<>(segments.between(window.start(), window.end()));
        openSegments.openSegment(now).ifPresent(open -> {
            if (open.kind() == EmObservationKind.OBSERVED && window.overlaps(open))
                daySegments.add(open);
        });

        List<EmGoalProgress> progress =
                EmGoalProgressCalculator.calculate(day, zone, daySegments, active);
        List<EmGoalAlert> today = new ArrayList<>();
        for (EmGoal goal : active) {
            for (EmGoalAlert alert : alerts.listByGoal(goal.id())) {
                if (!alert.triggeredAt().isBefore(window.start())
                        && alert.triggeredAt().isBefore(window.end()))
                    today.add(alert);
            }
        }

        List<EmGoalAlert> created = new ArrayList<>();
        for (EmGoalAlertPolicy.Plan plan : EmGoalAlertPolicy.plan(progress, today)) {
            for (int threshold : plan.thresholds()) {
                created.add(alerts.create(
                        EmGoalAlert.draft(plan.goal().id(), now, threshold, plan.metric())));
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
        List<EmGoalAlert> pending = alerts.listPending();
        List<EmGoalAlert> updates = new ArrayList<>();
        for (EmGoalAlert alert : pending) {
            if (alert.isPendingNotice())
                updates.add(alert.observed(EmGoalAlert.OUTCOME_DISCARDED, now, null));
            else if (alert.isObserving())
                updates.add(alert.observed(EmGoalAlert.OUTCOME_NOT_OBSERVED, now, null));
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
        List<EmGoalAlert> updates = alerts.listPending().stream()
                .filter(EmGoalAlert::isObserving)
                .map(alert -> alert.observed(EmGoalAlert.OUTCOME_NOT_OBSERVED, now, null))
                .toList();
        if (!updates.isEmpty()) {
            alerts.updateAll(updates);
            for (EmGoalAlert alert : updates) observations.remove(alert.id());
        }
        return updates.size();
    }

    private void settleObservations(Instant now, EmDriftStateProvider drift) {
        List<EmGoalAlert> updates = new ArrayList<>();
        for (EmGoalAlert alert : alerts.listPending()) {
            if (!alert.isObserving() || now.isBefore(alert.observedUntil())) continue;
            ObservationStart start = observations.remove(alert.id());
            updates.add(resolve(alert, start, drift.current(), now));
        }
        if (!updates.isEmpty()) alerts.updateAll(updates);
    }

    private static EmGoalAlert resolve(EmGoalAlert alert, ObservationStart start,
                                     EmDriftSignal signal, Instant now) {
        if (start == null)
            return alert.observed(EmGoalAlert.OUTCOME_NOT_OBSERVED, now, null);

        if (signal.active()) {
            String outcome = start.driftActive()
                    ? EmGoalAlert.OUTCOME_STILL_DRIFTING
                    : EmGoalAlert.OUTCOME_NEW_DRIFT;
            return alert.observed(outcome, now, null);
        }
        if (signal.generation() > start.generation())
            return alert.observed(EmGoalAlert.OUTCOME_NEW_DRIFT, now, null);
        if (!start.driftActive())
            return alert.observed(EmGoalAlert.OUTCOME_NO_DRIFT, now, null);

        return alert.observed(EmGoalAlert.OUTCOME_STABLE, now,
                millisFromNotice(alert.notifiedAt(), signal.changedAt()));
    }

    private static long millisFromNotice(Instant noticeAt, Instant changedAt) {
        if (noticeAt == null || changedAt == null || changedAt.isBefore(noticeAt)) return 0;
        return Duration.between(noticeAt, changedAt).toMillis();
    }

    private void deliverDueNotices(Instant now, EmDriftStateProvider drift,
                                   Duration deferralMax, List<EmGoalProgress> progress) {
        EmDriftSignal signal = drift.current();
        List<EmGoalAlert> due = new ArrayList<>();
        for (EmGoalAlert alert : alerts.listPending()) {
            if (!alert.isPendingNotice()) continue;
            boolean capReached = !now.isBefore(alert.triggeredAt().plus(deferralMax));
            if (!signal.active() || capReached) due.add(alert);
        }
        if (due.isEmpty()) return;

        List<EmGoalAlert> notified = new ArrayList<>(due.size());
        for (EmGoalAlert alert : due) {
            EmGoalAlert delivered = alert.notified(now, now.plus(OBSERVATION_WINDOW));
            notified.add(delivered);
            observations.put(delivered.id(), new ObservationStart(
                    signal.active(), signal.generation()));
        }
        alerts.updateAll(notified);

        List<EmGoal> allGoals = goals.listAll();
        Map<Long, EmGoal> goalsById = new HashMap<>();
        for (EmGoal goal : allGoals) goalsById.put(goal.id(), goal);
        Map<Long, EmGoalProgress> progressById = new HashMap<>();
        for (EmGoalProgress item : progress) progressById.put(item.goal().id(), item);

        Map<Long, List<EmGoalAlert>> byGoal = new LinkedHashMap<>();
        for (EmGoalAlert alert : notified)
            byGoal.computeIfAbsent(alert.goalId(), ignored -> new ArrayList<>()).add(alert);
        for (Map.Entry<Long, List<EmGoalAlert>> entry : byGoal.entrySet()) {
            EmGoal goal = goalsById.get(entry.getKey());
            if (goal == null) continue;
            notify(goal, entry.getValue(), progressById.get(goal.id()));
        }
    }

    private void notify(EmGoal goal, List<EmGoalAlert> alertsOfGoal, EmGoalProgress progress) {
        try {
            notifier.notifyGoal(notice(goal, alertsOfGoal, progress));
        } catch (RuntimeException e) {
            System.err.println("No se pudo mostrar el aviso de objetivo ("
                    + e.getClass().getSimpleName() + ").");
        }
    }

    private static EmGoalAlertNotice notice(EmGoal goal, List<EmGoalAlert> alertsOfGoal,
                                          EmGoalProgress progress) {
        int minutesThreshold = highestThreshold(alertsOfGoal, EmGoalMetric.MINUTES);
        int entriesThreshold = highestThreshold(alertsOfGoal, EmGoalMetric.ENTRIES);
        List<String> parts = new ArrayList<>(2);
        if (entriesThreshold > 0)
            parts.add(part(EmGoalMetric.ENTRIES, entriesThreshold, goal, progress));
        if (minutesThreshold > 0)
            parts.add(part(EmGoalMetric.MINUTES, minutesThreshold, goal, progress));
        String body = contextLabel(goal.context()) + ": " + String.join(" y ", parts)
                + ". Aviso del límite diario.";
        return new EmGoalAlertNotice("Objetivo diario", body);
    }

    private static int highestThreshold(List<EmGoalAlert> alertsOfGoal, EmGoalMetric metric) {
        return alertsOfGoal.stream()
                .filter(alert -> alert.metric() == metric)
                .mapToInt(EmGoalAlert::thresholdPercent)
                .max()
                .orElse(0);
    }

    private static String part(EmGoalMetric metric, int threshold, EmGoal goal,
                               EmGoalProgress progress) {
        if (metric == EmGoalMetric.MINUTES) {
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

    private static String contextLabel(EmContextKey context) {
        return context.siteHost() == null
                ? context.processName()
                : context.processName() + " · " + context.siteHost();
    }

    private record ObservationStart(boolean driftActive, long generation) {
    }
}
