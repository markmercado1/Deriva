package com.deriva.goals;

import com.deriva.analysis.EmDayWindow;
import com.deriva.goals.model.EmGoal;
import com.deriva.goals.model.EmGoalJustification;
import com.deriva.goals.model.EmGoalMetric;
import com.deriva.goals.model.EmGoalProgress;
import com.deriva.goals.model.EmGoalState;
import com.deriva.privacy.EmObservationKind;
import com.deriva.privacy.EmPrivacyFilter;
import com.deriva.tracking.model.EmContextKey;
import com.deriva.tracking.model.EmSegment;

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
public final class EmGoalProgressCalculator {
    /** Percentage at which a goal is considered near its limit. */
    public static final int NEAR_PERCENT = 80;
    private static final Duration ENTRY_GAP = Duration.ofSeconds(3);

    private EmGoalProgressCalculator() {
    }

    /**
     * Returns one progress row per supplied goal, preserving the input order.
     * Callers should pass the active goals; the calculator does not mutate them.
     */
    public static List<EmGoalProgress> calculate(LocalDate day, ZoneId zone,
                                               List<EmSegment> segments, List<EmGoal> goals) {
        return calculate(day, zone, segments, goals, List.of());
    }

    /**
     * Calculates progress applying user justifications as plain data. A justification
     * covers the excess accumulated when it was marked, never later use, and only
     * affects the local day of its {@code justifiedAt}. The raw used time and entries
     * are returned unchanged so callers can show what was measured.
     */
    public static List<EmGoalProgress> calculate(LocalDate day, ZoneId zone,
                                               List<EmSegment> segments, List<EmGoal> goals,
                                               List<EmGoalJustification> justifications) {
        Objects.requireNonNull(day, "day");
        Objects.requireNonNull(zone, "zone");
        Objects.requireNonNull(segments, "segments");
        Objects.requireNonNull(goals, "goals");
        Objects.requireNonNull(justifications, "justifications");
        EmDayWindow window = EmDayWindow.of(day, zone);
        boolean excludedInDay = segments.stream()
                .anyMatch(segment -> segment.kind() == EmObservationKind.EXCLUDED && window.overlaps(segment));

        List<EmGoalProgress> progress = new ArrayList<>(goals.size());
        for (EmGoal goal : goals) {
            Objects.requireNonNull(goal, "goal");
            List<Interval> intervals = intervalsFor(goal, segments, window);
            long usedMillis = intervals.stream()
                    .mapToLong(interval -> Duration.between(interval.start(), interval.end()).toMillis())
                    .sum();
            int entries = countEntries(intervals);
            boolean unmeasurable = excludedInDay && mayHaveExcludedTime(goal.context());
            List<EmGoalJustification> applied = applicableJustifications(goal, justifications, window);
            progress.add(new EmGoalProgress(goal, usedMillis, entries,
                    state(goal, usedMillis, entries, applied), unmeasurable, applied));
        }
        return List.copyOf(progress);
    }

    /**
     * Keeps at most one justification per metric: the latest one replaces earlier
     * coverage instead of accumulating it. Justifications from another local day are
     * ignored even if a caller passes them.
     */
    private static List<EmGoalJustification> applicableJustifications(
            EmGoal goal, List<EmGoalJustification> justifications, EmDayWindow window) {
        Map<EmGoalMetric, EmGoalJustification> byMetric = new EnumMap<>(EmGoalMetric.class);
        for (EmGoalJustification justification : justifications) {
            Objects.requireNonNull(justification, "justification");
            if (justification.goalId() != goal.id()) continue;
            Instant at = justification.justifiedAt();
            if (at.isBefore(window.start()) || !at.isBefore(window.end())) continue;
            EmGoalJustification previous = byMetric.get(justification.metric());
            if (previous == null || !at.isBefore(previous.justifiedAt()))
                byMetric.put(justification.metric(), justification);
        }
        return List.copyOf(byMetric.values());
    }

    private static List<Interval> intervalsFor(EmGoal goal, List<EmSegment> segments, EmDayWindow window) {
        List<Interval> matching = new ArrayList<>();
        for (EmSegment segment : segments) {
            if (segment.kind() != EmObservationKind.OBSERVED || !matches(goal.context(), segment.context()))
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
    private static boolean matches(EmContextKey target, EmContextKey sample) {
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
    private static EmGoalState state(EmGoal goal, long usedMillis, int entries,
                                   List<EmGoalJustification> applied) {
        boolean exceeded = false;
        boolean covered = false;
        if (goal.dailyMinutes() != null) {
            long limitMillis = goal.dailyMinutes() * 60_000L;
            if (usedMillis >= limitMillis) {
                EmGoalJustification justification = find(applied, EmGoalMetric.MINUTES);
                if (justification != null && usedMillis - limitMillis <= justification.covered())
                    covered = true;
                else
                    exceeded = true;
            }
        }
        if (goal.dailyEntries() != null) {
            if (entries >= goal.dailyEntries()) {
                EmGoalJustification justification = find(applied, EmGoalMetric.ENTRIES);
                if (justification != null && (long) entries - goal.dailyEntries() <= justification.covered())
                    covered = true;
                else
                    exceeded = true;
            }
        }
        if (exceeded) return EmGoalState.EXCEEDED;
        if (covered) return EmGoalState.JUSTIFIED;

        boolean near = false;
        if (goal.dailyMinutes() != null) {
            long limitMillis = goal.dailyMinutes() * 60_000L;
            near = usedMillis * 100L >= limitMillis * NEAR_PERCENT;
        }
        if (goal.dailyEntries() != null)
            near |= (long) entries * 100L >= (long) goal.dailyEntries() * NEAR_PERCENT;
        return near ? EmGoalState.NEAR : EmGoalState.IN_PROGRESS;
    }

    private static EmGoalJustification find(List<EmGoalJustification> applied, EmGoalMetric metric) {
        for (EmGoalJustification justification : applied)
            if (justification.metric() == metric) return justification;
        return null;
    }

    /**
     * Excluded time has no identity. A site goal may always be affected; an application
     * goal only when it targets a browser process. Other applications are not marked,
     * because browser exclusions cannot be part of their observed time.
     */
    private static boolean mayHaveExcludedTime(EmContextKey context) {
        return context.siteHost() != null || EmPrivacyFilter.isBrowserProcess(context.processName());
    }

    private record Interval(Instant start, Instant end) {
        private Interval {
            Objects.requireNonNull(start, "start");
            Objects.requireNonNull(end, "end");
        }
    }
}
