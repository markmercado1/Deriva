package com.deriva.app.usecase;

import com.deriva.analysis.DayWindow;
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
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Application service for the daily goal alarm. It evaluates active goals with
 * {@link GoalProgressCalculator}, records the thresholds that are new today in
 * {@code goal_alerts}, and notifies the tray once per limit and evaluation.
 *
 * <p>Deduplication lives in the database, not in memory, so a restart on the same
 * day does not repeat an already recorded threshold. The open segment is supplied
 * by {@link OpenSegmentProvider}; this service never knows about the capture loop.</p>
 */
public final class GoalAlertService {
    private final GoalRepository goals;
    private final SegmentRepository segments;
    private final GoalAlertRepository alerts;
    private final GoalAlertNotifier notifier;
    private final Clock clock;
    private final ZoneId zone;

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

    /** Evaluates using the injected clock. */
    public List<GoalAlert> evaluate(OpenSegmentProvider openSegments) {
        return evaluate(clock.instant(), openSegments);
    }

    /**
     * Evaluates one instant. It returns the alerts created by this call, oldest
     * threshold first. Storage failures propagate so the caller can log them and
     * keep the capture loop alive.
     */
    public List<GoalAlert> evaluate(Instant now, OpenSegmentProvider openSegments) {
        Objects.requireNonNull(now, "now");
        Objects.requireNonNull(openSegments, "openSegments");

        LocalDate day = now.atZone(zone).toLocalDate();
        DayWindow window = DayWindow.of(day, zone);
        List<Goal> active = goals.listActive();
        if (active.isEmpty()) return List.of();

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
            notify(plan);
        }
        return List.copyOf(created);
    }

    private void notify(GoalAlertPolicy.Plan plan) {
        try {
            notifier.notifyGoal(notice(plan));
        } catch (RuntimeException e) {
            System.err.println("No se pudo mostrar el aviso de objetivo ("
                    + e.getClass().getSimpleName() + ").");
        }
    }

    private static GoalAlertNotice notice(GoalAlertPolicy.Plan plan) {
        String context = contextLabel(plan.goal().context());
        String body = plan.metric() == GoalMetric.MINUTES
                ? context + ": " + plan.used() / 60_000L + " de " + plan.limit() / 60_000L
                + " min. Aviso al " + plan.notifyThreshold() + " % del límite diario."
                : context + ": " + plan.used() + " de " + plan.limit()
                + " entradas. Aviso al " + plan.notifyThreshold() + " % del límite diario.";
        return new GoalAlertNotice("Objetivo diario", body);
    }

    private static String contextLabel(ContextKey context) {
        return context.siteHost() == null
                ? context.processName()
                : context.processName() + " · " + context.siteHost();
    }
}
