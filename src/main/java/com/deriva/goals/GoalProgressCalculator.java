package com.deriva.goals;

import com.deriva.analysis.DayWindow;
import com.deriva.goals.model.Goal;
import com.deriva.goals.model.GoalJustification;
import com.deriva.goals.model.GoalMetric;
import com.deriva.goals.model.GoalProgress;
import com.deriva.goals.model.GoalState;
import com.deriva.privacy.ObservationKind;
import com.deriva.privacy.PrivacyFilter;
import com.deriva.tracking.model.ContextKey;
import com.deriva.tracking.model.Segment;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Pure calculation of daily goal progress from closed segments. It knows nothing about
 * SQLite, JavaFX, threads or the wall clock.
 *
 * <p>Only observed segments count. An application goal matches every observed segment
 * of its process, with or without a site host; a site goal matches only its exact
 * process and host. Goals are independent limits, so the same sample can feed an
 * application goal and a site goal of the same process, but it is counted at most once
 * inside each goal. Time outside the local day is clipped. A visit is a new entry when
 * at least three seconds passed outside the context; shorter holes continue the same
 * visit.</p>
 */
public final class GoalProgressCalculator {
    /** Percentage at which a goal is considered near its limit. */
    public static final int NEAR_PERCENT = 80;
    private static final Duration ENTRY_GAP = Duration.ofSeconds(3);

    private GoalProgressCalculator() {
    }

    /**
     * Returns one progress row per supplied goal, preserving the input order.
     * Callers should pass the active goals; the calculator does not mutate them.
     */
    public static List<GoalProgress> calculate(LocalDate day, ZoneId zone,
                                               List<Segment> segments, List<Goal> goals) {
        return calculate(day, zone, segments, goals, List.of());
    }

    /**
     * Calculates progress applying user justifications as plain data. A justification
     * covers the excess accumulated when it was marked, never later use, and only
     * affects the local day of its {@code justifiedAt}. The raw used time and entries
     * are returned unchanged so callers can show what was measured.
     */
    public static List<GoalProgress> calculate(LocalDate day, ZoneId zone,
                                               List<Segment> segments, List<Goal> goals,
                                               List<GoalJustification> justifications) {
        Objects.requireNonNull(day, "day");
        Objects.requireNonNull(zone, "zone");
        Objects.requireNonNull(segments, "segments");
        Objects.requireNonNull(goals, "goals");
        Objects.requireNonNull(justifications, "justifications");
        DayWindow window = DayWindow.of(day, zone);
        boolean excludedInDay = segments.stream()
                .anyMatch(segment -> segment.kind() == ObservationKind.EXCLUDED && window.overlaps(segment));

        List<GoalProgress> progress = new ArrayList<>(goals.size());
        for (Goal goal : goals) {
            Objects.requireNonNull(goal, "goal");
            List<Interval> intervals = intervalsFor(goal, segments, window);
            long usedMillis = intervals.stream()
                    .mapToLong(interval -> Duration.between(interval.start(), interval.end()).toMillis())
                    .sum();
            int entries = countEntries(intervals);
            boolean unmeasurable = excludedInDay && mayHaveExcludedTime(goal.context());
            List<GoalJustification> applied = applicableJustifications(goal, justifications, window);
            progress.add(new GoalProgress(goal, usedMillis, entries,
                    state(goal, usedMillis, entries, applied), unmeasurable, applied));
        }
        return List.copyOf(progress);
    }

    /**
     * Keeps at most one justification per metric: the latest one replaces earlier
     * coverage instead of accumulating it. Justifications from another local day are
     * ignored even if a caller passes them.
     */
    private static List<GoalJustification> applicableJustifications(
            Goal goal, List<GoalJustification> justifications, DayWindow window) {
        Map<GoalMetric, GoalJustification> byMetric = new EnumMap<>(GoalMetric.class);
        for (GoalJustification justification : justifications) {
            Objects.requireNonNull(justification, "justification");
            if (justification.goalId() != goal.id()) continue;
            Instant at = justification.justifiedAt();
            if (at.isBefore(window.start()) || !at.isBefore(window.end())) continue;
            GoalJustification previous = byMetric.get(justification.metric());
            if (previous == null || !at.isBefore(previous.justifiedAt()))
                byMetric.put(justification.metric(), justification);
        }
        return List.copyOf(byMetric.values());
    }

    private static List<Interval> intervalsFor(Goal goal, List<Segment> segments, DayWindow window) {
        List<Interval> matching = new ArrayList<>();
        for (Segment segment : segments) {
            if (segment.kind() != ObservationKind.OBSERVED || !matches(goal.context(), segment.context()))
                continue;
            Instant start = window.clipStart(segment.start());
            Instant end = window.clipEnd(segment.end());
            if (start.isBefore(end)) matching.add(new Interval(start, end));
        }
        matching.sort(Comparator.comparing(Interval::start).thenComparing(Interval::end));
        return merge(matching);
    }

    /**
     * Application goals include every observed sample of the process. Site goals need
     * the exact process and host. This deliberately differs from the passive-activity
     * matching of A43, which must not promote an idle browser site under an app goal.
     */
    private static boolean matches(ContextKey target, ContextKey sample) {
        if (sample == null || !target.processName().equals(sample.processName())) return false;
        if (target.siteHost() == null) return true;
        return target.siteHost().equals(sample.siteHost());
    }

    /** Overlapping or touching intervals are unioned so no instant is counted twice. */
    private static List<Interval> merge(List<Interval> sorted) {
        List<Interval> merged = new ArrayList<>();
        for (Interval interval : sorted) {
            if (merged.isEmpty()) {
                merged.add(interval);
                continue;
            }
            Interval last = merged.get(merged.size() - 1);
            if (!interval.start().isAfter(last.end())) {
                if (interval.end().isAfter(last.end()))
                    merged.set(merged.size() - 1, new Interval(last.start(), interval.end()));
            } else {
                merged.add(interval);
            }
        }
        return merged;
    }

    private static int countEntries(List<Interval> intervals) {
        if (intervals.isEmpty()) return 0;
        int entries = 1;
        for (int i = 1; i < intervals.size(); i++) {
            Duration outside = Duration.between(intervals.get(i - 1).end(), intervals.get(i).start());
            if (outside.compareTo(ENTRY_GAP) >= 0) entries++;
        }
        return entries;
    }

    /**
     * A justification covers an over-limit metric only while the excess accumulated
     * after it was marked stays at zero. Any later use makes that metric exceeded
     * again, even though the stored justification remains for display.
     */
    private static GoalState state(Goal goal, long usedMillis, int entries,
                                   List<GoalJustification> applied) {
        boolean exceeded = false;
        boolean covered = false;
        if (goal.dailyMinutes() != null) {
            long limitMillis = goal.dailyMinutes() * 60_000L;
            if (usedMillis >= limitMillis) {
                GoalJustification justification = find(applied, GoalMetric.MINUTES);
                if (justification != null && usedMillis - limitMillis <= justification.covered())
                    covered = true;
                else
                    exceeded = true;
            }
        }
        if (goal.dailyEntries() != null) {
            if (entries >= goal.dailyEntries()) {
                GoalJustification justification = find(applied, GoalMetric.ENTRIES);
                if (justification != null && (long) entries - goal.dailyEntries() <= justification.covered())
                    covered = true;
                else
                    exceeded = true;
            }
        }
        if (exceeded) return GoalState.EXCEEDED;
        if (covered) return GoalState.JUSTIFIED;

        boolean near = false;
        if (goal.dailyMinutes() != null) {
            long limitMillis = goal.dailyMinutes() * 60_000L;
            near = usedMillis * 100L >= limitMillis * NEAR_PERCENT;
        }
        if (goal.dailyEntries() != null)
            near |= (long) entries * 100L >= (long) goal.dailyEntries() * NEAR_PERCENT;
        return near ? GoalState.NEAR : GoalState.IN_PROGRESS;
    }

    private static GoalJustification find(List<GoalJustification> applied, GoalMetric metric) {
        for (GoalJustification justification : applied)
            if (justification.metric() == metric) return justification;
        return null;
    }

    /**
     * Excluded time has no identity. A site goal may always be affected; an application
     * goal only when it targets a browser process. Other applications are not marked,
     * because browser exclusions cannot be part of their observed time.
     */
    private static boolean mayHaveExcludedTime(ContextKey context) {
        return context.siteHost() != null || PrivacyFilter.isBrowserProcess(context.processName());
    }

    private record Interval(Instant start, Instant end) {
        private Interval {
            Objects.requireNonNull(start, "start");
            Objects.requireNonNull(end, "end");
        }
    }
}
