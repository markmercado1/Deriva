package com.deriva.app.usecase;

import com.deriva.analysis.DayWindow;
import com.deriva.goals.GoalProgressCalculator;
import com.deriva.goals.OpenSegmentProvider;
import com.deriva.goals.PassiveGoalProvider;
import com.deriva.goals.PassiveGoalSnapshot;
import com.deriva.goals.model.Goal;
import com.deriva.goals.model.GoalAlert;
import com.deriva.goals.model.GoalJustification;
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
 * Application use case for daily goals. It publishes an immutable active-goal
 * snapshot so the capture thread can reload passive goals without touching SQL
 * or the UI, and it turns user justifications into plain calculator data.
 */
public final class GoalsService implements PassiveGoalProvider {
    private final GoalRepository goals;
    private final SegmentRepository segments;
    private final GoalAlertRepository alerts;
    private final Clock clock;
    private volatile PassiveGoalSnapshot passiveSnapshot;

    public GoalsService(GoalRepository goals, SegmentRepository segments,
                        GoalAlertRepository alerts, Clock clock) {
        this.goals = Objects.requireNonNull(goals, "goals");
        this.segments = Objects.requireNonNull(segments, "segments");
        this.alerts = Objects.requireNonNull(alerts, "alerts");
        this.clock = Objects.requireNonNull(clock, "clock");
        this.passiveSnapshot = new PassiveGoalSnapshot(1, goals.listActive());
    }

    @Override
    public PassiveGoalSnapshot snapshot() {
        return passiveSnapshot;
    }

    public List<Goal> listAll() {
        return goals.listAll();
    }

    public List<ContextKey> observedContexts() {
        return segments.observedContexts();
    }

    public synchronized Goal create(Goal goal) {
        Objects.requireNonNull(goal, "goal");
        if (goal.id() != 0) throw new IllegalArgumentException("new goal must not have an id");
        Goal created = goals.create(goal);
        publish();
        return created;
    }

    public synchronized void update(Goal goal) {
        Objects.requireNonNull(goal, "goal");
        goals.update(goal);
        publish();
    }

    public synchronized void setActive(long id, boolean active) {
        Goal goal = goals.find(id).orElseThrow(
                () -> new IllegalArgumentException("Unknown goal: " + id));
        if (goal.active() == active) return;
        goals.update(new Goal(goal.id(), goal.context(), goal.dailyMinutes(), goal.dailyEntries(),
                goal.passive(), active, goal.createdAt(), goal.updatedAt()));
        publish();
    }

    public synchronized boolean delete(long id) {
        boolean deleted = goals.delete(id);
        if (deleted) publish();
        return deleted;
    }

    public List<GoalProgress> progressForDay(LocalDate day, ZoneId zone) {
        return progressForDay(day, zone, now -> java.util.Optional.empty());
    }

    /**
     * Uses the same basis as the alarm: closed segments plus the confirmed open
     * segment bounded by the capture snapshot. The open segment is never closed or
     * persisted here.
     */
    public List<GoalProgress> progressForDay(LocalDate day, ZoneId zone,
                                             OpenSegmentProvider openSegments) {
        Objects.requireNonNull(day, "day");
        Objects.requireNonNull(zone, "zone");
        Objects.requireNonNull(openSegments, "openSegments");
        DayWindow window = DayWindow.of(day, zone);
        List<Segment> segmentsInDay = new ArrayList<>(segments.between(window.start(), window.end()));
        openSegments.openSegment(clock.instant()).ifPresent(open -> {
            if (open.kind() == ObservationKind.OBSERVED && window.overlaps(open))
                segmentsInDay.add(open);
        });
        List<GoalJustification> justifications = justificationsIn(window);
        return GoalProgressCalculator.calculate(day, zone, segmentsInDay,
                passiveSnapshot.activeGoals(), justifications);
    }

    /** Alerts triggered on the local day, used by the screen to show its three states. */
    public List<GoalAlert> alertsForDay(LocalDate day, ZoneId zone) {
        Objects.requireNonNull(day, "day");
        Objects.requireNonNull(zone, "zone");
        DayWindow window = DayWindow.of(day, zone);
        return alerts.listBetween(window.start(), window.end());
    }

    /**
     * Marks the 100 % alerts of today for one goal. Coverage is the excess accumulated
     * up to this instant on the same basis as the alarm. Re-justifying replaces the
     * coverage; it never accumulates. The reason is stored exactly as supplied.
     */
    public synchronized void justify(long goalId, String reason, ZoneId zone,
                                     OpenSegmentProvider openSegments) {
        Objects.requireNonNull(zone, "zone");
        Objects.requireNonNull(openSegments, "openSegments");
        if (goalId <= 0) throw new IllegalArgumentException("goalId");
        String cleanReason = normalizeReason(reason);
        Instant now = clock.instant();
        LocalDate day = now.atZone(zone).toLocalDate();
        DayWindow window = DayWindow.of(day, zone);
        Goal goal = goals.find(goalId).orElseThrow(
                () -> new IllegalArgumentException("Unknown goal: " + goalId));
        List<GoalAlert> fullAlerts = alertsForGoalOnDay(goalId, window);
        if (fullAlerts.isEmpty())
            throw new IllegalStateException("No 100 % alert today for goal " + goalId);
        GoalProgress progress = progressForDay(day, zone, openSegments).stream()
                .filter(item -> item.goal().id() == goalId)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Goal is not active: " + goalId));
        List<GoalAlert> updated = new ArrayList<>(fullAlerts.size());
        for (GoalAlert alert : fullAlerts)
            updated.add(alert.justified(coveredExcess(goal, progress, alert.metric()), now, cleanReason));
        alerts.updateAll(updated);
    }

    /** Removes today's user justifications for one goal; the 100 % alerts remain. */
    public synchronized void removeJustification(long goalId, ZoneId zone) {
        Objects.requireNonNull(zone, "zone");
        if (goalId <= 0) throw new IllegalArgumentException("goalId");
        DayWindow window = DayWindow.of(clock.instant().atZone(zone).toLocalDate(), zone);
        List<GoalAlert> justified = alerts.listJustifiedBetween(window.start(), window.end()).stream()
                .filter(alert -> alert.goalId() == goalId && alert.thresholdPercent() == 100)
                .toList();
        if (!justified.isEmpty())
            alerts.updateAll(justified.stream().map(GoalAlert::unjustified).toList());
    }

    public LocalDate today(ZoneId zone) {
        return clock.instant().atZone(zone).toLocalDate();
    }

    public record DayHistory(List<Goal> goals, java.util.Map<Long, List<com.deriva.goals.model.GoalRevision>> revisions,
                             java.util.Set<Long> archived, List<GoalProgress> progress) {}

    /** Longest inclusive period the history screen may request. */
    public static final int MAX_RANGE_DAYS = 366;

    /**
     * One goal on one local day: the last configuration recorded before the day ended,
     * whether it was deleted during that day, and its progress frozen at deletion.
     */
    public record GoalDay(LocalDate day, Goal goal, boolean archivedThatDay, Instant archivedAt,
                          GoalProgress progress) {}

    /** Read-only history of an inclusive local period, grouped by goal id. */
    public record RangeHistory(LocalDate from, LocalDate to,
                               java.util.Map<Long, List<GoalDay>> days,
                               java.util.Map<Long, List<com.deriva.goals.model.GoalRevision>> revisions,
                               List<GoalAlert> alerts) {}

    /** Last configuration known that day; all changes remain available separately. */
    public DayHistory history(LocalDate day, ZoneId zone) {
        RangeHistory range = history(day, day, zone);
        var selected = new ArrayList<Goal>();
        var archived = new java.util.HashSet<Long>();
        var progress = new ArrayList<GoalProgress>();
        for (List<GoalDay> goalDays : range.days().values()) {
            GoalDay entry = goalDays.getFirst();
            selected.add(entry.goal());
            if (entry.archivedThatDay()) archived.add(entry.goal().id());
            progress.add(entry.progress());
        }
        return new DayHistory(List.copyOf(selected), range.revisions(),
                java.util.Set.copyOf(archived), List.copyOf(progress));
    }

    /**
     * Applies the daily semantics to every local day of an inclusive period with one
     * query per table. Goals appear only on the days they existed; deleted goals stay
     * visible up to their deletion day and stop accruing use at the moment of deletion.
     */
    public RangeHistory history(LocalDate from, LocalDate to, ZoneId zone) {
        Objects.requireNonNull(from, "from");
        Objects.requireNonNull(to, "to");
        Objects.requireNonNull(zone, "zone");
        if (from.isAfter(to)) throw new IllegalArgumentException("from is after to");
        if (java.time.temporal.ChronoUnit.DAYS.between(from, to) + 1 > MAX_RANGE_DAYS)
            throw new IllegalArgumentException("period longer than " + MAX_RANGE_DAYS + " days");
        Instant start = DayWindow.of(from, zone).start();
        Instant end = DayWindow.of(to, zone).end();

        var grouped = new java.util.LinkedHashMap<Long, List<com.deriva.goals.model.GoalRevision>>();
        for (var revision : goals.revisionsBefore(end))
            grouped.computeIfAbsent(revision.goal().id(), ignored -> new ArrayList<>()).add(revision);

        var segmentsByDay = new java.util.HashMap<LocalDate, List<Segment>>();
        for (Segment segment : segments.between(start, end)) {
            LocalDate first = segment.start().atZone(zone).toLocalDate();
            LocalDate last = segment.end().atZone(zone).toLocalDate();
            if (first.isBefore(from)) first = from;
            if (last.isAfter(to)) last = to;
            for (LocalDate day = first; !day.isAfter(last); day = day.plusDays(1))
                segmentsByDay.computeIfAbsent(day, ignored -> new ArrayList<>()).add(segment);
        }
        List<GoalJustification> justifications = justificationsIn(start, end);

        var days = new java.util.LinkedHashMap<Long, List<GoalDay>>();
        for (LocalDate day = from; !day.isAfter(to); day = day.plusDays(1)) {
            DayWindow window = DayWindow.of(day, zone);
            List<Segment> daySegments = segmentsByDay.getOrDefault(day, List.of());
            for (var entry : grouped.entrySet()) {
                com.deriva.goals.model.GoalRevision last = null;
                for (var revision : entry.getValue()) {
                    if (!revision.recordedAt().isBefore(window.end())) break;
                    last = revision;
                }
                if (last == null) continue;
                if (last.archived() && last.recordedAt().isBefore(window.start())) continue;
                Goal goal = last.goal();
                // An archived goal freezes at deletion rather than accruing later use.
                Instant cutoff = last.archived() ? last.recordedAt() : window.end();
                var clipped = new ArrayList<Segment>();
                for (Segment segment : daySegments) {
                    if (!segment.start().isBefore(cutoff)) continue;
                    Instant segmentEnd = segment.end().isAfter(cutoff) ? cutoff : segment.end();
                    clipped.add(new Segment(segment.id(), segment.kind(), segment.context(),
                            segment.siteLabel(), segment.siteSource(), segment.start(), segmentEnd));
                }
                GoalProgress progress = GoalProgressCalculator.calculate(day, zone, clipped,
                        List.of(goal), justifications).getFirst();
                days.computeIfAbsent(entry.getKey(), ignored -> new ArrayList<>())
                        .add(new GoalDay(day, goal, last.archived(),
                                last.archived() ? last.recordedAt() : null, progress));
            }
        }
        var frozen = new java.util.LinkedHashMap<Long, List<GoalDay>>();
        days.forEach((id, list) -> frozen.put(id, List.copyOf(list)));
        return new RangeHistory(from, to, java.util.Collections.unmodifiableMap(frozen),
                java.util.Map.copyOf(grouped), alerts.listBetween(start, end));
    }

    private List<GoalJustification> justificationsIn(DayWindow window) {
        return justificationsIn(window.start(), window.end());
    }

    private List<GoalJustification> justificationsIn(Instant start, Instant end) {
        return alerts.listJustifiedBetween(start, end).stream()
                .filter(alert -> alert.thresholdPercent() == 100 && alert.isJustified())
                .map(alert -> new GoalJustification(alert.goalId(), alert.metric(),
                        alert.justifiedExcess(), alert.justifiedAt()))
                .toList();
    }

    private List<GoalAlert> alertsForGoalOnDay(long goalId, DayWindow window) {
        return alerts.listBetween(window.start(), window.end()).stream()
                .filter(alert -> alert.goalId() == goalId && alert.thresholdPercent() == 100)
                .toList();
    }

    private static long coveredExcess(Goal goal, GoalProgress progress, GoalMetric metric) {
        if (metric == GoalMetric.MINUTES) {
            if (goal.dailyMinutes() == null) return 0;
            long limit = goal.dailyMinutes() * 60_000L;
            return Math.max(0, progress.usedMillis() - limit);
        }
        if (goal.dailyEntries() == null) return 0;
        return Math.max(0, (long) progress.entries() - goal.dailyEntries());
    }

    private static String normalizeReason(String reason) {
        if (reason == null || reason.isBlank()) return null;
        if (reason.length() > GoalAlert.MAX_JUSTIFICATION_LENGTH)
            throw new IllegalArgumentException("justification is longer than 500 characters");
        return reason;
    }

    private void publish() {
        passiveSnapshot = new PassiveGoalSnapshot(passiveSnapshot.revision() + 1,
                goals.listActive());
    }
}
