package com.deriva.goals;

import com.deriva.goals.model.EmGoal;
import com.deriva.goals.model.EmGoalJustification;
import com.deriva.goals.model.EmGoalMetric;
import com.deriva.goals.model.EmGoalProgress;
import com.deriva.goals.model.EmGoalState;
import com.deriva.privacy.EmObservationKind;
import com.deriva.tracking.model.EmContextKey;
import com.deriva.tracking.model.EmSegment;
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

class EmGoalProgressCalculatorTest {
    private static final ZoneId ZONE = ZoneOffset.UTC;
    private static final LocalDate DAY = LocalDate.of(2026, 11, 24);

    @Test
    void clipsCrossMidnightAndCountsFirstPresenceAsOneEntry() {
        EmGoal goal = appGoal("chrome.exe", 60, null);

        EmGoalProgress progress = calculate(List.of(
                observed("chrome.exe", "youtube.com",
                        "2026-11-23T23:50:00Z", "2026-11-24T00:20:00Z")), goal).get(0);

        assertEquals(Duration.ofMinutes(20).toMillis(), progress.usedMillis());
        assertEquals(20, progress.usedMinutes());
        assertEquals(1, progress.entries());
        assertEquals(EmGoalState.IN_PROGRESS, progress.state());
    }

    @Test
    void siteHolesCountOnlyObservedTimeAndShorterHolesKeepTheEntry() {
        EmGoal goal = siteGoal("chrome.exe", "chess.com", 30, null);

        EmGoalProgress progress = calculate(List.of(
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
        EmGoal goal = appGoal("code.exe", 60, null);

        EmGoalProgress progress = calculate(List.of(
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
        EmGoal goal = appGoal("notepad.exe", 30, null);

        EmGoalProgress progress = calculate(List.of(
                observed("notepad.exe", null,
                        "2026-11-24T10:00:00Z", "2026-11-24T10:05:00Z"),
                boundary(EmObservationKind.IDLE,
                        "2026-11-24T10:05:00Z", "2026-11-24T10:10:00Z"),
                observed("notepad.exe", null,
                        "2026-11-24T10:10:00Z", "2026-11-24T10:15:00Z"),
                boundary(EmObservationKind.EXCLUDED,
                        "2026-11-24T10:15:00Z", "2026-11-24T10:20:00Z"),
                boundary(EmObservationKind.UNAVAILABLE,
                        "2026-11-24T10:20:00Z", "2026-11-24T10:25:00Z"),
                observed("notepad.exe", null,
                        "2026-11-24T10:25:00Z", "2026-11-24T10:30:00Z")), goal).get(0);

        assertEquals(Duration.ofMinutes(15).toMillis(), progress.usedMillis());
        assertEquals(3, progress.entries());
        assertEquals(EmGoalState.IN_PROGRESS, progress.state());
        assertFalse(progress.unmeasurable());
    }

    @Test
    void minutesLimitUsesExactSeventyNineEightyAndHundredPercent() {
        EmGoal goal = appGoal("code.exe", 10, null);

        assertEquals(EmGoalState.IN_PROGRESS, progress(goal, Duration.ofSeconds(474)).state());
        assertEquals(EmGoalState.NEAR, progress(goal, Duration.ofMinutes(8)).state());
        assertEquals(EmGoalState.EXCEEDED, progress(goal, Duration.ofMinutes(10)).state());
    }

    @Test
    void twoLimitsUseTheWorstStateOfEitherMetric() {
        EmGoal goal = appGoal("code.exe", 10, 3);

        assertEquals(EmGoalState.NEAR, progress(goal, Duration.ofMinutes(8)).state());

        EmGoalProgress entries = calculate(List.of(
                observed("code.exe", null,
                        "2026-11-24T10:00:00Z", "2026-11-24T10:00:10Z"),
                observed("code.exe", null,
                        "2026-11-24T10:00:20Z", "2026-11-24T10:00:30Z"),
                observed("code.exe", null,
                        "2026-11-24T10:00:40Z", "2026-11-24T10:00:50Z")), goal).get(0);

        assertEquals(3, entries.entries());
        assertEquals(Duration.ofSeconds(30).toMillis(), entries.usedMillis());
        assertEquals(EmGoalState.EXCEEDED, entries.state());
    }

    @Test
    void applicationAndSiteGoalsOfSameProcessAreIndependentLimits() {
        EmGoal app = appGoal("chrome.exe", 60, null);
        EmGoal site = siteGoal("chrome.exe", "chess.com", 20, null);

        List<EmGoalProgress> progress = calculate(List.of(
                observed("chrome.exe", "chess.com",
                        "2026-11-24T10:00:00Z", "2026-11-24T10:20:00Z"),
                observed("chrome.exe", "youtube.com",
                        "2026-11-24T10:20:00Z", "2026-11-24T10:30:00Z"),
                observed("chrome.exe", null,
                        "2026-11-24T10:30:00Z", "2026-11-24T10:35:00Z")), app, site);

        assertEquals(Duration.ofMinutes(35).toMillis(), progress.get(0).usedMillis());
        assertEquals(1, progress.get(0).entries());
        assertEquals(EmGoalState.IN_PROGRESS, progress.get(0).state());
        assertEquals(Duration.ofMinutes(20).toMillis(), progress.get(1).usedMillis());
        assertEquals(1, progress.get(1).entries());
        assertEquals(EmGoalState.EXCEEDED, progress.get(1).state());
    }

    @Test
    void excludedTimeMarksBrowserAndSiteGoalsButNotOtherApplications() {
        EmGoal chromeApp = appGoal("chrome.exe", 60, null);
        EmGoal edgeApp = appGoal("msedge.exe", 60, null);
        EmGoal site = siteGoal("chrome.exe", "chess.com", 60, null);
        EmGoal otherApp = appGoal("code.exe", 60, null);

        List<EmGoalProgress> progress = calculate(List.of(
                observed("chrome.exe", "chess.com",
                        "2026-11-24T10:00:00Z", "2026-11-24T10:10:00Z"),
                boundary(EmObservationKind.EXCLUDED,
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
        EmGoal chromeApp = appGoal("chrome.exe", 60, null);
        EmGoal site = siteGoal("chrome.exe", "chess.com", 60, null);

        List<EmGoalProgress> progress = calculate(List.of(
                observed("chrome.exe", "chess.com",
                        "2026-11-24T10:00:00Z", "2026-11-24T10:10:00Z")), chromeApp, site);

        assertFalse(progress.get(0).unmeasurable());
        assertFalse(progress.get(1).unmeasurable());
    }

    @Test
    void overlappingAndUnorderedSegmentsCountEachInstantOnce() {
        EmGoal goal = appGoal("code.exe", 60, null);

        EmGoalProgress progress = calculate(List.of(
                observed("code.exe", null,
                        "2026-11-24T10:05:00Z", "2026-11-24T10:15:00Z"),
                observed("code.exe", null,
                        "2026-11-24T10:00:00Z", "2026-11-24T10:10:00Z")), goal).get(0);

        assertEquals(Duration.ofMinutes(15).toMillis(), progress.usedMillis());
        assertEquals(1, progress.entries());
    }

    @Test
    void noSegmentsMeansZeroProgress() {
        EmGoal goal = appGoal("code.exe", 10, 2);

        EmGoalProgress progress = calculate(List.of(), goal).get(0);

        assertEquals(0, progress.usedMillis());
        assertEquals(0, progress.entries());
        assertEquals(EmGoalState.IN_PROGRESS, progress.state());
        assertFalse(progress.unmeasurable());
    }

    @Test
    void segmentsOutsideTheDayAreIgnored() {
        EmGoal goal = appGoal("code.exe", 60, null);

        EmGoalProgress progress = calculate(List.of(
                observed("code.exe", null,
                        "2026-11-23T10:00:00Z", "2026-11-23T11:00:00Z"),
                observed("code.exe", null,
                        "2026-11-25T10:00:00Z", "2026-11-25T11:00:00Z")), goal).get(0);

        assertEquals(0, progress.usedMillis());
        assertEquals(0, progress.entries());
    }

    @Test
    void justificationCoversTheExcessAtMarkTimeAndLaterUseCountsAgain() {
        EmGoal goal = storedAppGoal("code.exe", 10, null);
        EmGoalJustification justification = new EmGoalJustification(goal.id(), EmGoalMetric.MINUTES,
                Duration.ofMinutes(2).toMillis(), Instant.parse("2026-11-24T10:00:00Z"));

        EmGoalProgress covered = calculate(List.of(observed("code.exe", null,
                "2026-11-24T09:00:00Z", "2026-11-24T09:12:00Z")),
                List.of(justification), goal).get(0);

        assertEquals(Duration.ofMinutes(12).toMillis(), covered.usedMillis());
        assertEquals(EmGoalState.JUSTIFIED, covered.state());
        assertEquals(Duration.ofMinutes(2).toMillis(), covered.justifiedMillis());

        EmGoalProgress later = calculate(List.of(observed("code.exe", null,
                "2026-11-24T09:00:00Z", "2026-11-24T09:15:00Z")),
                List.of(justification), goal).get(0);

        assertEquals(Duration.ofMinutes(15).toMillis(), later.usedMillis());
        assertEquals(EmGoalState.EXCEEDED, later.state());
    }

    @Test
    void justificationFromAnotherDayIsIgnored() {
        EmGoal goal = storedAppGoal("code.exe", 10, null);
        EmGoalJustification yesterday = new EmGoalJustification(goal.id(), EmGoalMetric.MINUTES,
                Duration.ofMinutes(2).toMillis(), Instant.parse("2026-11-23T12:00:00Z"));

        EmGoalProgress progress = calculate(List.of(observed("code.exe", null,
                "2026-11-24T09:00:00Z", "2026-11-24T09:12:00Z")),
                List.of(yesterday), goal).get(0);

        assertEquals(EmGoalState.EXCEEDED, progress.state());
        assertTrue(progress.justifications().isEmpty());
    }

    @Test
    void latestJustificationReplacesEarlierCoverageInsteadOfAccumulating() {
        EmGoal goal = storedAppGoal("code.exe", 10, null);
        EmGoalJustification first = new EmGoalJustification(goal.id(), EmGoalMetric.MINUTES,
                Duration.ofMinutes(2).toMillis(), Instant.parse("2026-11-24T10:00:00Z"));
        EmGoalJustification second = new EmGoalJustification(goal.id(), EmGoalMetric.MINUTES,
                Duration.ofMinutes(5).toMillis(), Instant.parse("2026-11-24T11:00:00Z"));

        EmGoalProgress progress = calculate(List.of(observed("code.exe", null,
                "2026-11-24T09:00:00Z", "2026-11-24T09:15:00Z")),
                List.of(first, second), goal).get(0);

        assertEquals(EmGoalState.JUSTIFIED, progress.state());
        assertEquals(1, progress.justifications().size());
        assertEquals(Duration.ofMinutes(5).toMillis(), progress.justifiedMillis());
    }

    @Test
    void entryJustificationCoversExtraEntries() {
        EmGoal goal = storedAppGoal("code.exe", null, 2);
        EmGoalJustification justification = new EmGoalJustification(goal.id(), EmGoalMetric.ENTRIES,
                1, Instant.parse("2026-11-24T10:00:00Z"));

        EmGoalProgress progress = calculate(List.of(
                observed("code.exe", null, "2026-11-24T09:00:00Z", "2026-11-24T09:05:00Z"),
                observed("code.exe", null, "2026-11-24T09:05:04Z", "2026-11-24T09:10:00Z"),
                observed("code.exe", null, "2026-11-24T09:10:04Z", "2026-11-24T09:15:00Z")),
                List.of(justification), goal).get(0);

        assertEquals(3, progress.entries());
        assertEquals(1, progress.justifiedEntries());
        assertEquals(EmGoalState.JUSTIFIED, progress.state());
    }

    private static EmGoal storedAppGoal(String process, Integer minutes, Integer entries) {
        Instant at = Instant.parse("2026-11-24T00:00:00Z");
        return new EmGoal(7, new EmContextKey(process, null), minutes, entries, false, true, at, at);
    }

    private static List<EmGoalProgress> calculate(List<EmSegment> segments,
                                                List<EmGoalJustification> justifications,
                                                EmGoal... goals) {
        return EmGoalProgressCalculator.calculate(DAY, ZONE, segments, List.of(goals), justifications);
    }

    private static EmGoalProgress progress(EmGoal goal, Duration used) {
        Instant start = Instant.parse("2026-11-24T10:00:00Z");
        return calculate(List.of(new EmSegment(0, EmObservationKind.OBSERVED, goal.context(),
                null, null, start, start.plus(used))), goal).get(0);
    }

    private static List<EmGoalProgress> calculate(List<EmSegment> segments, EmGoal... goals) {
        return EmGoalProgressCalculator.calculate(DAY, ZONE, segments, List.of(goals));
    }

    private static EmGoal appGoal(String process, Integer minutes, Integer entries) {
        return EmGoal.draft(new EmContextKey(process, null), minutes, entries, false);
    }

    private static EmGoal siteGoal(String process, String host, Integer minutes, Integer entries) {
        return EmGoal.draft(new EmContextKey(process, host), minutes, entries, false);
    }

    private static EmSegment observed(String process, String host, String start, String end) {
        return new EmSegment(0, EmObservationKind.OBSERVED, new EmContextKey(process, host),
                null, null, Instant.parse(start), Instant.parse(end));
    }

    private static EmSegment boundary(EmObservationKind kind, String start, String end) {
        return new EmSegment(0, kind, null, null, null, Instant.parse(start), Instant.parse(end));
    }
}
