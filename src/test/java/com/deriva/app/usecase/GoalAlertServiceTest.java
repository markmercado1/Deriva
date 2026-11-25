package com.deriva.app.usecase;

import com.deriva.goals.DriftSignal;
import com.deriva.goals.DriftStateProvider;
import com.deriva.goals.model.Goal;
import com.deriva.goals.model.GoalAlert;
import com.deriva.goals.model.GoalMetric;
import com.deriva.privacy.ObservationKind;
import com.deriva.storage.sqlite.SQLiteGoalAlertRepository;
import com.deriva.storage.sqlite.SQLiteGoalRepository;
import com.deriva.storage.sqlite.SQLiteSegmentRepository;
import com.deriva.tracking.model.ContextKey;
import com.deriva.tracking.model.Segment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GoalAlertServiceTest {
    private static final ZoneOffset ZONE = ZoneOffset.UTC;
    private static final ContextKey CODE = new ContextKey("code.exe", null);

    @TempDir Path tempDirectory;

    private MutableClock clock;
    private SQLiteGoalRepository goals;
    private SQLiteSegmentRepository segments;
    private SQLiteGoalAlertRepository alerts;
    private List<GoalAlertNotice> notices;
    private GoalAlertService service;
    private DriftSignal driftSignal = DriftSignal.inactive();

    @BeforeEach
    void setUp() {
        clock = new MutableClock(Instant.parse("2026-11-24T10:30:00Z"), ZONE);
        goals = new SQLiteGoalRepository(tempDirectory.resolve("alerts.db"), clock);
        segments = new SQLiteSegmentRepository(tempDirectory.resolve("alerts.db"));
        alerts = new SQLiteGoalAlertRepository(tempDirectory.resolve("alerts.db"));
        notices = new ArrayList<>();
        service = newService(notices::add);
    }

    @Test
    void notifiesAtEightyThenAtHundredAndDeduplicatesAfterRestartAndOnTheNextDay() {
        Goal goal = goals.create(Goal.draft(CODE, 10, null, false));
        save("2026-11-24T10:00:00Z", "2026-11-24T10:08:00Z");

        List<GoalAlert> atEighty = service.evaluate(instant("2026-11-24T10:08:00Z"),
                now -> Optional.empty());
        assertEquals(1, atEighty.size());
        assertEquals(80, atEighty.getFirst().thresholdPercent());
        assertEquals(GoalMetric.MINUTES, atEighty.getFirst().metric());
        assertNull(atEighty.getFirst().outcome());
        assertNull(atEighty.getFirst().justification());
        assertEquals(1, notices.size());
        assertTrue(notices.getFirst().body().contains("8 de 10 min"));
        assertTrue(notices.getFirst().body().contains("80 %"));

        assertTrue(service.evaluate(instant("2026-11-24T10:08:30Z"), now -> Optional.empty()).isEmpty());
        assertEquals(1, notices.size());

        save("2026-11-24T10:08:00Z", "2026-11-24T10:10:00Z");
        List<GoalAlert> atHundred = service.evaluate(instant("2026-11-24T10:10:00Z"),
                now -> Optional.empty());
        assertEquals(1, atHundred.size());
        assertEquals(100, atHundred.getFirst().thresholdPercent());
        assertEquals(2, notices.size());
        assertTrue(notices.get(1).body().contains("10 de 10 min"));
        assertTrue(notices.get(1).body().contains("100 %"));

        GoalAlertService restarted = newService(notices::add);
        assertTrue(restarted.evaluate(instant("2026-11-24T10:11:00Z"), now -> Optional.empty()).isEmpty());
        assertEquals(2, alerts.listByGoal(goal.id()).size());
        assertEquals(2, notices.size());

        clock.set(instant("2026-11-25T10:30:00Z"));
        save("2026-11-25T10:00:00Z", "2026-11-25T10:08:00Z");
        List<GoalAlert> nextDay = restarted.evaluate(instant("2026-11-25T10:08:00Z"),
                now -> Optional.empty());
        assertEquals(1, nextDay.size());
        assertEquals(80, nextDay.getFirst().thresholdPercent());
        assertEquals(3, alerts.listByGoal(goal.id()).size());
    }

    @Test
    void entriesLimitOfFourRecordsBothThresholdsAndNotifiesOnlyTheHighest() {
        goals.create(Goal.draft(CODE, null, 4, false));
        save("2026-11-24T10:00:00Z", "2026-11-24T10:00:05Z");
        save("2026-11-24T10:00:10Z", "2026-11-24T10:00:15Z");
        save("2026-11-24T10:00:20Z", "2026-11-24T10:00:25Z");
        save("2026-11-24T10:00:30Z", "2026-11-24T10:00:35Z");

        List<GoalAlert> created = service.evaluate(instant("2026-11-24T10:00:35Z"),
                now -> Optional.empty());

        assertEquals(2, created.size());
        assertEquals(List.of(80, 100), created.stream().map(GoalAlert::thresholdPercent).toList());
        assertTrue(created.stream().allMatch(alert -> alert.metric() == GoalMetric.ENTRIES));
        assertEquals(1, notices.size());
        assertTrue(notices.getFirst().body().contains("4 de 4 entradas"));
        assertTrue(notices.getFirst().body().contains("100 %"));
    }

    @Test
    void startupAboveHundredRecordsBothThresholdsAndNotifiesOnlyTheHighest() {
        Goal goal = goals.create(Goal.draft(CODE, 10, null, false));
        save("2026-11-24T10:00:00Z", "2026-11-24T10:11:00Z");

        List<GoalAlert> created = service.evaluate(instant("2026-11-24T10:11:00Z"),
                now -> Optional.empty());

        assertEquals(List.of(80, 100), created.stream().map(GoalAlert::thresholdPercent).toList());
        assertEquals(1, notices.size());
        assertTrue(notices.getFirst().body().contains("11 de 10 min"));
        assertTrue(notices.getFirst().body().contains("100 %"));
        assertEquals(2, alerts.listByGoal(goal.id()).size());
    }

    @Test
    void minuteJumpBetweenEvaluationsRecordsBothThresholdsAndNotifiesOnlyTheHighest() {
        goals.create(Goal.draft(CODE, 10, null, false));
        save("2026-11-24T10:00:00Z", "2026-11-24T10:07:00Z");

        assertTrue(service.evaluate(instant("2026-11-24T10:07:00Z"), now -> Optional.empty()).isEmpty());

        save("2026-11-24T10:07:00Z", "2026-11-24T10:11:00Z");
        List<GoalAlert> created = service.evaluate(instant("2026-11-24T10:11:00Z"),
                now -> Optional.empty());

        assertEquals(List.of(80, 100), created.stream().map(GoalAlert::thresholdPercent).toList());
        assertEquals(1, notices.size());
        assertTrue(notices.getFirst().body().contains("11 de 10 min"));
        assertTrue(notices.getFirst().body().contains("100 %"));
    }

    @Test
    void openSegmentCutAtNowIsAddedToTheDay() {
        goals.create(Goal.draft(CODE, 10, null, false));
        save("2026-11-24T10:00:00Z", "2026-11-24T10:07:00Z");
        Segment open = new Segment(0, ObservationKind.OBSERVED, CODE, null, null,
                instant("2026-11-24T10:07:00Z"), instant("2026-11-24T10:08:30Z"));

        List<GoalAlert> created = service.evaluate(instant("2026-11-24T10:08:30Z"),
                now -> Optional.of(open));

        assertEquals(1, created.size());
        assertEquals(80, created.getFirst().thresholdPercent());
        assertTrue(notices.getFirst().body().contains("8 de 10 min"));
    }

    @Test
    void raisingTheLimitTheSameDayDoesNotRepeatTheRecordedThreshold() {
        Goal goal = goals.create(Goal.draft(CODE, 10, null, false));
        save("2026-11-24T10:00:00Z", "2026-11-24T10:08:00Z");
        assertEquals(1, service.evaluate(instant("2026-11-24T10:08:00Z"),
                now -> Optional.empty()).size());

        goals.update(new Goal(goal.id(), goal.context(), 20, null, false, true,
                goal.createdAt(), goal.updatedAt()));
        save("2026-11-24T10:08:00Z", "2026-11-24T10:16:00Z");

        assertTrue(service.evaluate(instant("2026-11-24T10:16:00Z"), now -> Optional.empty()).isEmpty());
        assertEquals(1, notices.size());

        save("2026-11-24T10:16:00Z", "2026-11-24T10:20:00Z");
        List<GoalAlert> atHundred = service.evaluate(instant("2026-11-24T10:20:00Z"),
                now -> Optional.empty());
        assertEquals(1, atHundred.size());
        assertEquals(100, atHundred.getFirst().thresholdPercent());
        assertEquals(2, notices.size());
    }

    @Test
    void notifierFailureStillRecordsTheAlert() {
        goals.create(Goal.draft(CODE, 10, null, false));
        save("2026-11-24T10:00:00Z", "2026-11-24T10:08:00Z");
        GoalAlertService failing = new GoalAlertService(goals, segments, alerts,
                notice -> {
                    throw new IllegalStateException("tray unavailable");
                }, clock, ZONE);

        List<GoalAlert> created = assertDoesNotThrow(() ->
                failing.evaluate(instant("2026-11-24T10:08:00Z"), now -> Optional.empty()));

        assertEquals(1, created.size());
        assertEquals(1, alerts.listByGoal(created.getFirst().goalId()).size());
    }

    @Test
    void pauseAndResumeInTheSameSessionDeliversThePendingNotice() {
        goals.create(Goal.draft(CODE, 10, null, false));
        save("2026-11-24T10:00:00Z", "2026-11-24T10:08:00Z");
        driftSignal = new DriftSignal(true, 1, instant("2026-11-24T10:08:00Z"));

        List<GoalAlert> created = evaluateWithDrift("2026-11-24T10:08:00Z");
        assertEquals(1, created.size());
        assertTrue(notices.isEmpty());
        GoalAlert pending = alerts.find(created.getFirst().id()).orElseThrow();
        assertTrue(pending.isPendingNotice());
        assertNull(pending.notifiedAt());

        driftSignal = new DriftSignal(false, 1, instant("2026-11-24T10:09:00Z"));
        assertTrue(evaluateWithDrift("2026-11-24T10:09:30Z").isEmpty());

        assertEquals(1, notices.size());
        GoalAlert delivered = alerts.find(pending.id()).orElseThrow();
        assertEquals(instant("2026-11-24T10:09:30Z"), delivered.notifiedAt());
        assertEquals(instant("2026-11-24T10:11:30Z"), delivered.observedUntil());
        assertTrue(delivered.isObserving());
    }

    @Test
    void capDeliversTheNoticeWhileDriftIsStillActive() {
        goals.create(Goal.draft(CODE, 10, null, false));
        save("2026-11-24T10:00:00Z", "2026-11-24T10:08:00Z");
        driftSignal = new DriftSignal(true, 1, instant("2026-11-24T10:08:00Z"));

        evaluateWithDrift("2026-11-24T10:08:00Z");
        assertTrue(notices.isEmpty());
        driftSignal = new DriftSignal(true, 1, instant("2026-11-24T10:08:00Z"));

        evaluateWithDrift("2026-11-24T10:13:00Z");
        assertEquals(1, notices.size());
    }

    @Test
    void noDriftAtNoticeAndNoNewDriftProducesNoDriftOutcome() {
        Goal goal = goals.create(Goal.draft(CODE, 10, null, false));
        save("2026-11-24T10:00:00Z", "2026-11-24T10:08:00Z");

        List<GoalAlert> created = evaluateWithDrift("2026-11-24T10:08:00Z");
        assertEquals(1, notices.size());
        evaluateWithDrift("2026-11-24T10:10:00Z");

        GoalAlert stored = alerts.find(created.getFirst().id()).orElseThrow();
        assertEquals(GoalAlert.OUTCOME_NO_DRIFT, stored.outcome());
        assertEquals(instant("2026-11-24T10:10:00Z"), stored.outcomeAt());
        assertNull(stored.outcomeAfterMs());
        assertEquals(goal.id(), stored.goalId());
    }

    @Test
    void driftThatEndsAfterTheNoticeProducesStableWithItsTime() {
        goals.create(Goal.draft(CODE, 10, null, false));
        save("2026-11-24T10:00:00Z", "2026-11-24T10:08:00Z");
        driftSignal = new DriftSignal(true, 1, instant("2026-11-24T10:08:00Z"));
        evaluateWithDrift("2026-11-24T10:08:00Z");
        evaluateWithDrift("2026-11-24T10:13:00Z");
        assertEquals(1, notices.size());

        driftSignal = new DriftSignal(false, 1, instant("2026-11-24T10:13:20Z"));
        evaluateWithDrift("2026-11-24T10:15:00Z");

        GoalAlert stored = alerts.listByGoal(1).getFirst();
        assertEquals(GoalAlert.OUTCOME_STABLE, stored.outcome());
        assertEquals(20_000L, stored.outcomeAfterMs());
    }

    @Test
    void driftThatRemainsAfterTheCapProducesStillDrifting() {
        goals.create(Goal.draft(CODE, 10, null, false));
        save("2026-11-24T10:00:00Z", "2026-11-24T10:08:00Z");
        driftSignal = new DriftSignal(true, 1, instant("2026-11-24T10:08:00Z"));
        evaluateWithDrift("2026-11-24T10:08:00Z");
        evaluateWithDrift("2026-11-24T10:13:00Z");
        assertEquals(1, notices.size());

        driftSignal = new DriftSignal(true, 1, instant("2026-11-24T10:08:00Z"));
        evaluateWithDrift("2026-11-24T10:15:00Z");

        assertEquals(GoalAlert.OUTCOME_STILL_DRIFTING,
                alerts.listByGoal(1).getFirst().outcome());
    }

    @Test
    void driftThatAppearsDuringTheObservationProducesNewDrift() {
        goals.create(Goal.draft(CODE, 10, null, false));
        save("2026-11-24T10:00:00Z", "2026-11-24T10:08:00Z");
        evaluateWithDrift("2026-11-24T10:08:00Z");
        assertEquals(1, notices.size());

        driftSignal = new DriftSignal(true, 1, instant("2026-11-24T10:08:30Z"));
        evaluateWithDrift("2026-11-24T10:10:00Z");

        assertEquals(GoalAlert.OUTCOME_NEW_DRIFT, alerts.listByGoal(1).getFirst().outcome());
    }

    @Test
    void restartDiscardsAnUndeliveredNoticeInsteadOfShowingIt() {
        goals.create(Goal.draft(CODE, 10, null, false));
        save("2026-11-24T10:00:00Z", "2026-11-24T10:08:00Z");
        driftSignal = new DriftSignal(true, 1, instant("2026-11-24T10:08:00Z"));
        List<GoalAlert> created = evaluateWithDrift("2026-11-24T10:08:00Z");
        assertTrue(notices.isEmpty());

        GoalAlertService restarted = newService(notices::add);
        assertEquals(1, restarted.recoverAfterRestart(instant("2026-11-24T10:12:00Z")));

        GoalAlert stored = alerts.find(created.getFirst().id()).orElseThrow();
        assertEquals(GoalAlert.OUTCOME_DISCARDED, stored.outcome());
        assertEquals(instant("2026-11-24T10:12:00Z"), stored.outcomeAt());
        assertTrue(notices.isEmpty());
    }

    @Test
    void interruptingTheObservationRecordsNotObservedWithoutClaimingACause() {
        goals.create(Goal.draft(CODE, 10, null, false));
        save("2026-11-24T10:00:00Z", "2026-11-24T10:08:00Z");
        List<GoalAlert> created = evaluateWithDrift("2026-11-24T10:08:00Z");
        assertEquals(1, notices.size());

        assertEquals(1, service.interruptObservations(instant("2026-11-24T10:09:00Z")));

        GoalAlert stored = alerts.find(created.getFirst().id()).orElseThrow();
        assertEquals(GoalAlert.OUTCOME_NOT_OBSERVED, stored.outcome());
        assertNotNull(stored.outcomeAt());
        assertEquals(1, notices.size());
    }

    @Test
    void twoLimitsOfTheSameGoalProduceOneCombinedNoticeAndRecordEveryThreshold() {
        Goal goal = goals.create(Goal.draft(CODE, 10, 4, false));
        save("2026-11-24T10:00:00Z", "2026-11-24T10:02:00Z");
        save("2026-11-24T10:02:05Z", "2026-11-24T10:04:05Z");
        save("2026-11-24T10:04:10Z", "2026-11-24T10:06:10Z");
        save("2026-11-24T10:06:15Z", "2026-11-24T10:08:15Z");

        List<GoalAlert> created = evaluateWithDrift("2026-11-24T10:08:15Z");

        assertEquals(3, created.size());
        assertEquals(1, notices.size());
        String body = notices.getFirst().body();
        assertTrue(body.contains("100 % de entradas"), body);
        assertTrue(body.contains("4 de 4 entradas"), body);
        assertTrue(body.contains("80 % de minutos"), body);
        assertTrue(body.contains("8 de 10 min"), body);
        assertFalse(body.toLowerCase().contains("gracias"), body);
        assertEquals(3, alerts.listByGoal(goal.id()).size());
    }

    @Test
    void lockDuringTheDeferralWaitDoesNotLoseThePendingNotice() {
        goals.create(Goal.draft(CODE, 10, null, false));
        save("2026-11-24T10:00:00Z", "2026-11-24T10:08:00Z");
        driftSignal = new DriftSignal(true, 1, instant("2026-11-24T10:08:00Z"));
        GoalAlertMonitor monitor = new GoalAlertMonitor(service, Duration.ofSeconds(30));

        assertTrue(monitor.tick(CaptureState.RUNNING, ObservationKind.OBSERVED,
                instant("2026-11-24T10:08:00Z"), now -> Optional.empty(),
                () -> driftSignal, Duration.ofMinutes(5)));
        assertTrue(notices.isEmpty());

        assertFalse(monitor.tick(CaptureState.RUNNING, ObservationKind.UNAVAILABLE,
                instant("2026-11-24T10:08:10Z"), now -> Optional.empty(),
                () -> driftSignal, Duration.ofMinutes(5)));
        assertTrue(notices.isEmpty());

        driftSignal = new DriftSignal(false, 1, instant("2026-11-24T10:08:20Z"));
        assertTrue(monitor.tick(CaptureState.RUNNING, ObservationKind.OBSERVED,
                instant("2026-11-24T10:08:30Z"), now -> Optional.empty(),
                () -> driftSignal, Duration.ofMinutes(5)));
        assertEquals(1, notices.size());
    }

    private List<GoalAlert> evaluateWithDrift(String now) {
        return service.evaluate(instant(now), now2 -> Optional.empty(), () -> driftSignal,
                Duration.ofMinutes(5));
    }

    private GoalAlertService newService(GoalAlertNotifier notifier) {
        return new GoalAlertService(goals, segments, alerts, notifier, clock, ZONE);
    }

    private void save(String start, String end) {
        segments.saveAll(List.of(new Segment(0, ObservationKind.OBSERVED, CODE, null, null,
                instant(start), instant(end))));
    }

    private static Instant instant(String value) {
        return Instant.parse(value);
    }

    private static final class MutableClock extends Clock {
        private Instant instant;
        private final ZoneId zone;

        private MutableClock(Instant instant, ZoneId zone) {
            this.instant = instant;
            this.zone = zone;
        }

        void set(Instant instant) {
            this.instant = instant;
        }

        @Override
        public ZoneId getZone() {
            return zone;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return new MutableClock(instant, zone);
        }

        @Override
        public Instant instant() {
            return instant;
        }
    }
}
