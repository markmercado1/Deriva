package com.deriva.goals;

import com.deriva.goals.model.Goal;
import com.deriva.goals.model.GoalJustification;
import com.deriva.goals.model.GoalMetric;
import com.deriva.goals.model.GoalProgress;
import com.deriva.goals.model.GoalState;
import com.deriva.privacy.ObservationKind;
import com.deriva.tracking.model.ContextKey;
import com.deriva.tracking.model.Segment;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GoalProgressCalculatorTest {
    private static final ZoneId ZONE = ZoneOffset.UTC;
    private static final LocalDate DAY = LocalDate.of(2026, 11, 24);

    @Test
    void clipsCrossMidnightAndCountsFirstPresenceAsOneEntry() {
        Goal goal = appGoal("chrome.exe", 60, null);

        GoalProgress progress = calculate(List.of(
                observed("chrome.exe", "youtube.com",
                        "2026-11-23T23:50:00Z", "2026-11-24T00:20:00Z")), goal).get(0);

        assertEquals(Duration.ofMinutes(20).toMillis(), progress.usedMillis());
        assertEquals(20, progress.usedMinutes());
        assertEquals(1, progress.entries());
        assertEquals(GoalState.IN_PROGRESS, progress.state());
    }

    @Test
    void siteHolesCountOnlyObservedTimeAndShorterHolesKeepTheEntry() {
        Goal goal = siteGoal("chrome.exe", "chess.com", 30, null);

        GoalProgress progress = calculate(List.of(
                observed("chrome.exe", "chess.com",
                        "2026-11-24T10:00:00Z", "2026-11-24T10:05:00Z"),
                observed("chrome.exe", "chess.com",
                        "2026-11-24T10:05:02Z", "2026-11-24T10:10:00Z"),
                observed("chrome.exe", "chess.com",
                        "2026-11-24T10:10:04Z", "2026-11-24T10:11:00Z")), goal).get(0);

        assertEquals(Duration.ofSeconds(654).toMillis(), progress.usedMillis());
        assertEquals(2, progress.entries());
    }

    @Test
    void outsideForAtLeastThreeSecondsStartsANewEntry() {
        Goal goal = appGoal("code.exe", 60, null);

        GoalProgress progress = calculate(List.of(
                observed("code.exe", null,
                        "2026-11-24T10:00:00Z", "2026-11-24T10:05:00Z"),
                observed("code.exe", null,
                        "2026-11-24T10:05:03Z", "2026-11-24T10:10:00Z"),
                observed("code.exe", null,
                        "2026-11-24T10:10:04Z", "2026-11-24T10:12:00Z")), goal).get(0);

        assertEquals(3, progress.entries());
        assertEquals(Duration.ofSeconds(713).toMillis(), progress.usedMillis());
    }

    @Test
    void idleExcludedAndUnavailableDoNotCountAsUsage() {
        Goal goal = appGoal("notepad.exe", 30, null);

        GoalProgress progress = calculate(List.of(
                observed("notepad.exe", null,
                        "2026-11-24T10:00:00Z", "2026-11-24T10:05:00Z"),
                boundary(ObservationKind.IDLE,
                        "2026-11-24T10:05:00Z", "2026-11-24T10:10:00Z"),
                observed("notepad.exe", null,
                        "2026-11-24T10:10:00Z", "2026-11-24T10:15:00Z"),
                boundary(ObservationKind.EXCLUDED,
                        "2026-11-24T10:15:00Z", "2026-11-24T10:20:00Z"),
                boundary(ObservationKind.UNAVAILABLE,
                        "2026-11-24T10:20:00Z", "2026-11-24T10:25:00Z"),
                observed("notepad.exe", null,
                        "2026-11-24T10:25:00Z", "2026-11-24T10:30:00Z")), goal).get(0);

        assertEquals(Duration.ofMinutes(15).toMillis(), progress.usedMillis());
        assertEquals(3, progress.entries());
        assertEquals(GoalState.IN_PROGRESS, progress.state());
        assertFalse(progress.unmeasurable());
    }

    @Test
    void minutesLimitUsesExactSeventyNineEightyAndHundredPercent() {
        Goal goal = appGoal("code.exe", 10, null);

        assertEquals(GoalState.IN_PROGRESS, progress(goal, Duration.ofSeconds(474)).state());
        assertEquals(GoalState.NEAR, progress(goal, Duration.ofMinutes(8)).state());
        assertEquals(GoalState.EXCEEDED, progress(goal, Duration.ofMinutes(10)).state());
    }

    @Test
    void twoLimitsUseTheWorstStateOfEitherMetric() {
        Goal goal = appGoal("code.exe", 10, 3);

        assertEquals(GoalState.NEAR, progress(goal, Duration.ofMinutes(8)).state());

        GoalProgress entries = calculate(List.of(
                observed("code.exe", null,
                        "2026-11-24T10:00:00Z", "2026-11-24T10:00:10Z"),
                observed("code.exe", null,
                        "2026-11-24T10:00:20Z", "2026-11-24T10:00:30Z"),
                observed("code.exe", null,
                        "2026-11-24T10:00:40Z", "2026-11-24T10:00:50Z")), goal).get(0);

        assertEquals(3, entries.entries());
        assertEquals(Duration.ofSeconds(30).toMillis(), entries.usedMillis());
        assertEquals(GoalState.EXCEEDED, entries.state());
    }

    @Test
    void applicationAndSiteGoalsOfSameProcessAreIndependentLimits() {
        Goal app = appGoal("chrome.exe", 60, null);
        Goal site = siteGoal("chrome.exe", "chess.com", 20, null);

        List<GoalProgress> progress = calculate(List.of(
                observed("chrome.exe", "chess.com",
                        "2026-11-24T10:00:00Z", "2026-11-24T10:20:00Z"),
                observed("chrome.exe", "youtube.com",
                        "2026-11-24T10:20:00Z", "2026-11-24T10:30:00Z"),
                observed("chrome.exe", null,
                        "2026-11-24T10:30:00Z", "2026-11-24T10:35:00Z")), app, site);

        assertEquals(Duration.ofMinutes(35).toMillis(), progress.get(0).usedMillis());
        assertEquals(1, progress.get(0).entries());
        assertEquals(GoalState.IN_PROGRESS, progress.get(0).state());
        assertEquals(Duration.ofMinutes(20).toMillis(), progress.get(1).usedMillis());
        assertEquals(1, progress.get(1).entries());
        assertEquals(GoalState.EXCEEDED, progress.get(1).state());
    }

    @Test
    void excludedTimeMarksBrowserAndSiteGoalsButNotOtherApplications() {
        Goal chromeApp = appGoal("chrome.exe", 60, null);
        Goal edgeApp = appGoal("msedge.exe", 60, null);
        Goal site = siteGoal("chrome.exe", "chess.com", 60, null);
        Goal otherApp = appGoal("code.exe", 60, null);

        List<GoalProgress> progress = calculate(List.of(
                observed("chrome.exe", "chess.com",
                        "2026-11-24T10:00:00Z", "2026-11-24T10:10:00Z"),
                boundary(ObservationKind.EXCLUDED,
                        "2026-11-24T10:10:00Z", "2026-11-24T10:15:00Z"),
                observed("code.exe", null,
                        "2026-11-24T10:15:00Z", "2026-11-24T10:20:00Z")),
                chromeApp, edgeApp, site, otherApp);

        assertTrue(progress.get(0).unmeasurable());
        assertTrue(progress.get(1).unmeasurable());
        assertTrue(progress.get(2).unmeasurable());
        assertFalse(progress.get(3).unmeasurable());
    }

    @Test
    void withoutExcludedTimeBrowserGoalsRemainMeasurable() {
        Goal chromeApp = appGoal("chrome.exe", 60, null);
        Goal site = siteGoal("chrome.exe", "chess.com", 60, null);

        List<GoalProgress> progress = calculate(List.of(
                observed("chrome.exe", "chess.com",
                        "2026-11-24T10:00:00Z", "2026-11-24T10:10:00Z")), chromeApp, site);

        assertFalse(progress.get(0).unmeasurable());
        assertFalse(progress.get(1).unmeasurable());
    }

    @Test
    void overlappingAndUnorderedSegmentsCountEachInstantOnce() {
        Goal goal = appGoal("code.exe", 60, null);

        GoalProgress progress = calculate(List.of(
                observed("code.exe", null,
                        "2026-11-24T10:05:00Z", "2026-11-24T10:15:00Z"),
                observed("code.exe", null,
                        "2026-11-24T10:00:00Z", "2026-11-24T10:10:00Z")), goal).get(0);

        assertEquals(Duration.ofMinutes(15).toMillis(), progress.usedMillis());
        assertEquals(1, progress.entries());
    }

    @Test
    void noSegmentsMeansZeroProgress() {
        Goal goal = appGoal("code.exe", 10, 2);

        GoalProgress progress = calculate(List.of(), goal).get(0);

        assertEquals(0, progress.usedMillis());
        assertEquals(0, progress.entries());
        assertEquals(GoalState.IN_PROGRESS, progress.state());
        assertFalse(progress.unmeasurable());
    }

    @Test
    void segmentsOutsideTheDayAreIgnored() {
        Goal goal = appGoal("code.exe", 60, null);

        GoalProgress progress = calculate(List.of(
                observed("code.exe", null,
                        "2026-11-23T10:00:00Z", "2026-11-23T11:00:00Z"),
                observed("code.exe", null,
                        "2026-11-25T10:00:00Z", "2026-11-25T11:00:00Z")), goal).get(0);

        assertEquals(0, progress.usedMillis());
        assertEquals(0, progress.entries());
    }

    @Test
    void justificationCoversTheExcessAtMarkTimeAndLaterUseCountsAgain() {
        Goal goal = storedAppGoal("code.exe", 10, null);
        GoalJustification justification = new GoalJustification(goal.id(), GoalMetric.MINUTES,
                Duration.ofMinutes(2).toMillis(), Instant.parse("2026-11-24T10:00:00Z"));

        GoalProgress covered = calculate(List.of(observed("code.exe", null,
                "2026-11-24T09:00:00Z", "2026-11-24T09:12:00Z")),
                List.of(justification), goal).get(0);

        assertEquals(Duration.ofMinutes(12).toMillis(), covered.usedMillis());
        assertEquals(GoalState.JUSTIFIED, covered.state());
        assertEquals(Duration.ofMinutes(2).toMillis(), covered.justifiedMillis());

        GoalProgress later = calculate(List.of(observed("code.exe", null,
                "2026-11-24T09:00:00Z", "2026-11-24T09:15:00Z")),
                List.of(justification), goal).get(0);

        assertEquals(Duration.ofMinutes(15).toMillis(), later.usedMillis());
        assertEquals(GoalState.EXCEEDED, later.state());
    }

    @Test
    void justificationFromAnotherDayIsIgnored() {
        Goal goal = storedAppGoal("code.exe", 10, null);
        GoalJustification yesterday = new GoalJustification(goal.id(), GoalMetric.MINUTES,
                Duration.ofMinutes(2).toMillis(), Instant.parse("2026-11-23T12:00:00Z"));

        GoalProgress progress = calculate(List.of(observed("code.exe", null,
                "2026-11-24T09:00:00Z", "2026-11-24T09:12:00Z")),
                List.of(yesterday), goal).get(0);

        assertEquals(GoalState.EXCEEDED, progress.state());
        assertTrue(progress.justifications().isEmpty());
    }

    @Test
    void latestJustificationReplacesEarlierCoverageInsteadOfAccumulating() {
        Goal goal = storedAppGoal("code.exe", 10, null);
        GoalJustification first = new GoalJustification(goal.id(), GoalMetric.MINUTES,
                Duration.ofMinutes(2).toMillis(), Instant.parse("2026-11-24T10:00:00Z"));
        GoalJustification second = new GoalJustification(goal.id(), GoalMetric.MINUTES,
                Duration.ofMinutes(5).toMillis(), Instant.parse("2026-11-24T11:00:00Z"));

        GoalProgress progress = calculate(List.of(observed("code.exe", null,
                "2026-11-24T09:00:00Z", "2026-11-24T09:15:00Z")),
                List.of(first, second), goal).get(0);

        assertEquals(GoalState.JUSTIFIED, progress.state());
        assertEquals(1, progress.justifications().size());
        assertEquals(Duration.ofMinutes(5).toMillis(), progress.justifiedMillis());
    }

    @Test
    void entryJustificationCoversExtraEntries() {
        Goal goal = storedAppGoal("code.exe", null, 2);
        GoalJustification justification = new GoalJustification(goal.id(), GoalMetric.ENTRIES,
                1, Instant.parse("2026-11-24T10:00:00Z"));

        GoalProgress progress = calculate(List.of(
                observed("code.exe", null, "2026-11-24T09:00:00Z", "2026-11-24T09:05:00Z"),
                observed("code.exe", null, "2026-11-24T09:05:04Z", "2026-11-24T09:10:00Z"),
                observed("code.exe", null, "2026-11-24T09:10:04Z", "2026-11-24T09:15:00Z")),
                List.of(justification), goal).get(0);

        assertEquals(3, progress.entries());
        assertEquals(1, progress.justifiedEntries());
        assertEquals(GoalState.JUSTIFIED, progress.state());
    }

    private static Goal storedAppGoal(String process, Integer minutes, Integer entries) {
        Instant at = Instant.parse("2026-11-24T00:00:00Z");
        return new Goal(7, new ContextKey(process, null), minutes, entries, false, true, at, at);
    }

    private static List<GoalProgress> calculate(List<Segment> segments,
                                                List<GoalJustification> justifications,
                                                Goal... goals) {
        return GoalProgressCalculator.calculate(DAY, ZONE, segments, List.of(goals), justifications);
    }

    private static GoalProgress progress(Goal goal, Duration used) {
        Instant start = Instant.parse("2026-11-24T10:00:00Z");
        return calculate(List.of(new Segment(0, ObservationKind.OBSERVED, goal.context(),
                null, null, start, start.plus(used))), goal).get(0);
    }

    private static List<GoalProgress> calculate(List<Segment> segments, Goal... goals) {
        return GoalProgressCalculator.calculate(DAY, ZONE, segments, List.of(goals));
    }

    private static Goal appGoal(String process, Integer minutes, Integer entries) {
        return Goal.draft(new ContextKey(process, null), minutes, entries, false);
    }

    private static Goal siteGoal(String process, String host, Integer minutes, Integer entries) {
        return Goal.draft(new ContextKey(process, host), minutes, entries, false);
    }

    private static Segment observed(String process, String host, String start, String end) {
        return new Segment(0, ObservationKind.OBSERVED, new ContextKey(process, host),
                null, null, Instant.parse(start), Instant.parse(end));
    }

    private static Segment boundary(ObservationKind kind, String start, String end) {
        return new Segment(0, kind, null, null, null, Instant.parse(start), Instant.parse(end));
    }
}
