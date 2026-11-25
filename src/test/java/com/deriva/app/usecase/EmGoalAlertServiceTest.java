package com.deriva.app.usecase;

import com.deriva.goals.EmDriftSignal;
import com.deriva.goals.EmDriftStateProvider;
import com.deriva.goals.model.EmGoal;
import com.deriva.goals.model.EmGoalAlert;
import com.deriva.goals.model.EmGoalMetric;
import com.deriva.privacy.EmObservationKind;
import com.deriva.storage.sqlite.EmSQLiteGoalAlertRepository;
import com.deriva.storage.sqlite.EmSQLiteGoalRepository;
import com.deriva.storage.sqlite.EmSQLiteSegmentRepository;
import com.deriva.tracking.model.EmContextKey;
import com.deriva.tracking.model.EmSegment;
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

class EmGoalAlertServiceTest {
    private static final ZoneOffset ZONE = ZoneOffset.UTC;
    private static final EmContextKey CODE = new EmContextKey("code.exe", null);

    @TempDir Path tempDirectory;

    private MutableClock clock;
    private EmSQLiteGoalRepository goals;
    private EmSQLiteSegmentRepository segments;
    private EmSQLiteGoalAlertRepository alerts;
    private List<EmGoalAlertNotice> notices;
    private EmGoalAlertService service;
    private EmDriftSignal driftSignal = EmDriftSignal.inactive();

    @BeforeEach
    void setUp() {
        clock = new MutableClock(Instant.parse("2026-11-24T10:30:00Z"), ZONE);
        goals = new EmSQLiteGoalRepository(tempDirectory.resolve("alerts.db"), clock);
        segments = new EmSQLiteSegmentRepository(tempDirectory.resolve("alerts.db"));
        alerts = new EmSQLiteGoalAlertRepository(tempDirectory.resolve("alerts.db"));
        notices = new ArrayList<>();
        service = newService(notices::add);
    }

    @Test
    void notifiesAtEightyThenAtHundredAndDeduplicatesAfterRestartAndOnTheNextDay() {
        EmGoal goal = goals.create(EmGoal.draft(CODE, 10, null, false));
        save("2026-11-24T10:00:00Z", "2026-11-24T10:08:00Z");

        List<EmGoalAlert> atEighty = service.evaluate(instant("2026-11-24T10:08:00Z"),
                now -> Optional.empty());
        assertEquals(1, atEighty.size());
        assertEquals(80, atEighty.getFirst().thresholdPercent());
        assertEquals(EmGoalMetric.MINUTES, atEighty.getFirst().metric());
        assertNull(atEighty.getFirst().outcome());
        assertNull(atEighty.getFirst().justification());
        assertEquals(1, notices.size());
        assertTrue(notices.getFirst().body().contains("8 de 10 min"));
        assertTrue(notices.getFirst().body().contains("80 %"));

        assertTrue(service.evaluate(instant("2026-11-24T10:08:30Z"), now -> Optional.empty()).isEmpty());
        assertEquals(1, notices.size());

        save("2026-11-24T10:08:00Z", "2026-11-24T10:10:00Z");
        List<EmGoalAlert> atHundred = service.evaluate(instant("2026-11-24T10:10:00Z"),
                now -> Optional.empty());
        assertEquals(1, atHundred.size());
        assertEquals(100, atHundred.getFirst().thresholdPercent());
        assertEquals(2, notices.size());
        assertTrue(notices.get(1).body().contains("10 de 10 min"));
        assertTrue(notices.get(1).body().contains("100 %"));

        EmGoalAlertService restarted = newService(notices::add);
        assertTrue(restarted.evaluate(instant("2026-11-24T10:11:00Z"), now -> Optional.empty()).isEmpty());
        assertEquals(2, alerts.listByGoal(goal.id()).size());
        assertEquals(2, notices.size());

        clock.set(instant("2026-11-25T10:30:00Z"));
        save("2026-11-25T10:00:00Z", "2026-11-25T10:08:00Z");
        List<EmGoalAlert> nextDay = restarted.evaluate(instant("2026-11-25T10:08:00Z"),
                now -> Optional.empty());
        assertEquals(1, nextDay.size());
        assertEquals(80, nextDay.getFirst().thresholdPercent());
        assertEquals(3, alerts.listByGoal(goal.id()).size());
    }

    @Test
    void entriesLimitOfFourRecordsBothThresholdsAndNotifiesOnlyTheHighest() {
        goals.create(EmGoal.draft(CODE, null, 4, false));
        save("2026-11-24T10:00:00Z", "2026-11-24T10:00:05Z");
        save("2026-11-24T10:00:10Z", "2026-11-24T10:00:15Z");
        save("2026-11-24T10:00:20Z", "2026-11-24T10:00:25Z");
        save("2026-11-24T10:00:30Z", "2026-11-24T10:00:35Z");

        List<EmGoalAlert> created = service.evaluate(instant("2026-11-24T10:00:35Z"),
                now -> Optional.empty());

        assertEquals(2, created.size());
        assertEquals(List.of(80, 100), created.stream().map(EmGoalAlert::thresholdPercent).toList());
        assertTrue(created.stream().allMatch(alert -> alert.metric() == EmGoalMetric.ENTRIES));
        assertEquals(1, notices.size());
        assertTrue(notices.getFirst().body().contains("4 de 4 entradas"));
        assertTrue(notices.getFirst().body().contains("100 %"));
    }

    @Test
    void startupAboveHundredRecordsBothThresholdsAndNotifiesOnlyTheHighest() {
        EmGoal goal = goals.create(EmGoal.draft(CODE, 10, null, false));
        save("2026-11-24T10:00:00Z", "2026-11-24T10:11:00Z");

        List<EmGoalAlert> created = service.evaluate(instant("2026-11-24T10:11:00Z"),
                now -> Optional.empty());

        assertEquals(List.of(80, 100), created.stream().map(EmGoalAlert::thresholdPercent).toList());
        assertEquals(1, notices.size());
        assertTrue(notices.getFirst().body().contains("11 de 10 min"));
        assertTrue(notices.getFirst().body().contains("100 %"));
        assertEquals(2, alerts.listByGoal(goal.id()).size());
    }

    @Test
    void minuteJumpBetweenEvaluationsRecordsBothThresholdsAndNotifiesOnlyTheHighest() {
        goals.create(EmGoal.draft(CODE, 10, null, false));
        save("2026-11-24T10:00:00Z", "2026-11-24T10:07:00Z");

        assertTrue(service.evaluate(instant("2026-11-24T10:07:00Z"), now -> Optional.empty()).isEmpty());

        save("2026-11-24T10:07:00Z", "2026-11-24T10:11:00Z");
        List<EmGoalAlert> created = service.evaluate(instant("2026-11-24T10:11:00Z"),
                now -> Optional.empty());

        assertEquals(List.of(80, 100), created.stream().map(EmGoalAlert::thresholdPercent).toList());
        assertEquals(1, notices.size());
        assertTrue(notices.getFirst().body().contains("11 de 10 min"));
        assertTrue(notices.getFirst().body().contains("100 %"));
    }

    @Test
    void openSegmentCutAtNowIsAddedToTheDay() {
        goals.create(EmGoal.draft(CODE, 10, null, false));
        save("2026-11-24T10:00:00Z", "2026-11-24T10:07:00Z");
        EmSegment open = new EmSegment(0, EmObservationKind.OBSERVED, CODE, null, null,
                instant("2026-11-24T10:07:00Z"), instant("2026-11-24T10:08:30Z"));

        List<EmGoalAlert> created = service.evaluate(instant("2026-11-24T10:08:30Z"),
                now -> Optional.of(open));

        assertEquals(1, created.size());
        assertEquals(80, created.getFirst().thresholdPercent());
        assertTrue(notices.getFirst().body().contains("8 de 10 min"));
    }

    @Test
    void raisingTheLimitTheSameDayDoesNotRepeatTheRecordedThreshold() {
        EmGoal goal = goals.create(EmGoal.draft(CODE, 10, null, false));
        save("2026-11-24T10:00:00Z", "2026-11-24T10:08:00Z");
        assertEquals(1, service.evaluate(instant("2026-11-24T10:08:00Z"),
                now -> Optional.empty()).size());

        goals.update(new EmGoal(goal.id(), goal.context(), 20, null, false, true,
                goal.createdAt(), goal.updatedAt()));
        save("2026-11-24T10:08:00Z", "2026-11-24T10:16:00Z");

        assertTrue(service.evaluate(instant("2026-11-24T10:16:00Z"), now -> Optional.empty()).isEmpty());
        assertEquals(1, notices.size());

        save("2026-11-24T10:16:00Z", "2026-11-24T10:20:00Z");
        List<EmGoalAlert> atHundred = service.evaluate(instant("2026-11-24T10:20:00Z"),
                now -> Optional.empty());
        assertEquals(1, atHundred.size());
        assertEquals(100, atHundred.getFirst().thresholdPercent());
        assertEquals(2, notices.size());
    }

    @Test
    void notifierFailureStillRecordsTheAlert() {
        goals.create(EmGoal.draft(CODE, 10, null, false));
        save("2026-11-24T10:00:00Z", "2026-11-24T10:08:00Z");
        EmGoalAlertService failing = new EmGoalAlertService(goals, segments, alerts,
                notice -> {
                    throw new IllegalStateException("tray unavailable");
                }, clock, ZONE);

        List<EmGoalAlert> created = assertDoesNotThrow(() ->
                failing.evaluate(instant("2026-11-24T10:08:00Z"), now -> Optional.empty()));

        assertEquals(1, created.size());
        assertEquals(1, alerts.listByGoal(created.getFirst().goalId()).size());
    }

    @Test
    void pauseAndResumeInTheSameSessionDeliversThePendingNotice() {
        goals.create(EmGoal.draft(CODE, 10, null, false));
        save("2026-11-24T10:00:00Z", "2026-11-24T10:08:00Z");
        driftSignal = new EmDriftSignal(true, 1, instant("2026-11-24T10:08:00Z"));

        List<EmGoalAlert> created = evaluateWithDrift("2026-11-24T10:08:00Z");
        assertEquals(1, created.size());
        assertTrue(notices.isEmpty());
        EmGoalAlert pending = alerts.find(created.getFirst().id()).orElseThrow();
        assertTrue(pending.isPendingNotice());
        assertNull(pending.notifiedAt());

        driftSignal = new EmDriftSignal(false, 1, instant("2026-11-24T10:09:00Z"));
        assertTrue(evaluateWithDrift("2026-11-24T10:09:30Z").isEmpty());

        assertEquals(1, notices.size());
        EmGoalAlert delivered = alerts.find(pending.id()).orElseThrow();
        assertEquals(instant("2026-11-24T10:09:30Z"), delivered.notifiedAt());
        assertEquals(instant("2026-11-24T10:11:30Z"), delivered.observedUntil());
        assertTrue(delivered.isObserving());
    }

    @Test
    void capDeliversTheNoticeWhileDriftIsStillActive() {
        goals.create(EmGoal.draft(CODE, 10, null, false));
        save("2026-11-24T10:00:00Z", "2026-11-24T10:08:00Z");
        driftSignal = new EmDriftSignal(true, 1, instant("2026-11-24T10:08:00Z"));

        evaluateWithDrift("2026-11-24T10:08:00Z");
        assertTrue(notices.isEmpty());
        driftSignal = new EmDriftSignal(true, 1, instant("2026-11-24T10:08:00Z"));

        evaluateWithDrift("2026-11-24T10:13:00Z");
        assertEquals(1, notices.size());
    }

    @Test
    void noDriftAtNoticeAndNoNewDriftProducesNoDriftOutcome() {
        EmGoal goal = goals.create(EmGoal.draft(CODE, 10, null, false));
        save("2026-11-24T10:00:00Z", "2026-11-24T10:08:00Z");

        List<EmGoalAlert> created = evaluateWithDrift("2026-11-24T10:08:00Z");
        assertEquals(1, notices.size());
        evaluateWithDrift("2026-11-24T10:10:00Z");

        EmGoalAlert stored = alerts.find(created.getFirst().id()).orElseThrow();
        assertEquals(EmGoalAlert.OUTCOME_NO_DRIFT, stored.outcome());
        assertEquals(instant("2026-11-24T10:10:00Z"), stored.outcomeAt());
        assertNull(stored.outcomeAfterMs());
        assertEquals(goal.id(), stored.goalId());
    }

    @Test
    void driftThatEndsAfterTheNoticeProducesStableWithItsTime() {
        goals.create(EmGoal.draft(CODE, 10, null, false));
        save("2026-11-24T10:00:00Z", "2026-11-24T10:08:00Z");
        driftSignal = new EmDriftSignal(true, 1, instant("2026-11-24T10:08:00Z"));
        evaluateWithDrift("2026-11-24T10:08:00Z");
        evaluateWithDrift("2026-11-24T10:13:00Z");
        assertEquals(1, notices.size());

        driftSignal = new EmDriftSignal(false, 1, instant("2026-11-24T10:13:20Z"));
        evaluateWithDrift("2026-11-24T10:15:00Z");

        EmGoalAlert stored = alerts.listByGoal(1).getFirst();
        assertEquals(EmGoalAlert.OUTCOME_STABLE, stored.outcome());
        assertEquals(20_000L, stored.outcomeAfterMs());
    }

    @Test
    void driftThatRemainsAfterTheCapProducesStillDrifting() {
        goals.create(EmGoal.draft(CODE, 10, null, false));
        save("2026-11-24T10:00:00Z", "2026-11-24T10:08:00Z");
        driftSignal = new EmDriftSignal(true, 1, instant("2026-11-24T10:08:00Z"));
        evaluateWithDrift("2026-11-24T10:08:00Z");
        evaluateWithDrift("2026-11-24T10:13:00Z");
        assertEquals(1, notices.size());

        driftSignal = new EmDriftSignal(true, 1, instant("2026-11-24T10:08:00Z"));
        evaluateWithDrift("2026-11-24T10:15:00Z");

        assertEquals(EmGoalAlert.OUTCOME_STILL_DRIFTING,
                alerts.listByGoal(1).getFirst().outcome());
    }

    @Test
    void driftThatAppearsDuringTheObservationProducesNewDrift() {
        goals.create(EmGoal.draft(CODE, 10, null, false));
        save("2026-11-24T10:00:00Z", "2026-11-24T10:08:00Z");
        evaluateWithDrift("2026-11-24T10:08:00Z");
        assertEquals(1, notices.size());

        driftSignal = new EmDriftSignal(true, 1, instant("2026-11-24T10:08:30Z"));
        evaluateWithDrift("2026-11-24T10:10:00Z");

        assertEquals(EmGoalAlert.OUTCOME_NEW_DRIFT, alerts.listByGoal(1).getFirst().outcome());
    }

    @Test
    void restartDiscardsAnUndeliveredNoticeInsteadOfShowingIt() {
        goals.create(EmGoal.draft(CODE, 10, null, false));
        save("2026-11-24T10:00:00Z", "2026-11-24T10:08:00Z");
        driftSignal = new EmDriftSignal(true, 1, instant("2026-11-24T10:08:00Z"));
        List<EmGoalAlert> created = evaluateWithDrift("2026-11-24T10:08:00Z");
        assertTrue(notices.isEmpty());

        EmGoalAlertService restarted = newService(notices::add);
        assertEquals(1, restarted.recoverAfterRestart(instant("2026-11-24T10:12:00Z")));

        EmGoalAlert stored = alerts.find(created.getFirst().id()).orElseThrow();
        assertEquals(EmGoalAlert.OUTCOME_DISCARDED, stored.outcome());
        assertEquals(instant("2026-11-24T10:12:00Z"), stored.outcomeAt());
        assertTrue(notices.isEmpty());
    }

    @Test
    void interruptingTheObservationRecordsNotObservedWithoutClaimingACause() {
        goals.create(EmGoal.draft(CODE, 10, null, false));
        save("2026-11-24T10:00:00Z", "2026-11-24T10:08:00Z");
        List<EmGoalAlert> created = evaluateWithDrift("2026-11-24T10:08:00Z");
        assertEquals(1, notices.size());

        assertEquals(1, service.interruptObservations(instant("2026-11-24T10:09:00Z")));

        EmGoalAlert stored = alerts.find(created.getFirst().id()).orElseThrow();
        assertEquals(EmGoalAlert.OUTCOME_NOT_OBSERVED, stored.outcome());
        assertNotNull(stored.outcomeAt());
        assertEquals(1, notices.size());
    }

    @Test
    void twoLimitsOfTheSameGoalProduceOneCombinedNoticeAndRecordEveryThreshold() {
        EmGoal goal = goals.create(EmGoal.draft(CODE, 10, 4, false));
        save("2026-11-24T10:00:00Z", "2026-11-24T10:02:00Z");
        save("2026-11-24T10:02:05Z", "2026-11-24T10:04:05Z");
        save("2026-11-24T10:04:10Z", "2026-11-24T10:06:10Z");
        save("2026-11-24T10:06:15Z", "2026-11-24T10:08:15Z");

        List<EmGoalAlert> created = evaluateWithDrift("2026-11-24T10:08:15Z");

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
        goals.create(EmGoal.draft(CODE, 10, null, false));
        save("2026-11-24T10:00:00Z", "2026-11-24T10:08:00Z");
        driftSignal = new EmDriftSignal(true, 1, instant("2026-11-24T10:08:00Z"));
        EmGoalAlertMonitor monitor = new EmGoalAlertMonitor(service, Duration.ofSeconds(30));

        assertTrue(monitor.tick(EmCaptureState.RUNNING, EmObservationKind.OBSERVED,
                instant("2026-11-24T10:08:00Z"), now -> Optional.empty(),
                () -> driftSignal, Duration.ofMinutes(5)));
        assertTrue(notices.isEmpty());

        assertFalse(monitor.tick(EmCaptureState.RUNNING, EmObservationKind.UNAVAILABLE,
                instant("2026-11-24T10:08:10Z"), now -> Optional.empty(),
                () -> driftSignal, Duration.ofMinutes(5)));
        assertTrue(notices.isEmpty());

        driftSignal = new EmDriftSignal(false, 1, instant("2026-11-24T10:08:20Z"));
        assertTrue(monitor.tick(EmCaptureState.RUNNING, EmObservationKind.OBSERVED,
                instant("2026-11-24T10:08:30Z"), now -> Optional.empty(),
                () -> driftSignal, Duration.ofMinutes(5)));
        assertEquals(1, notices.size());
    }

    private List<EmGoalAlert> evaluateWithDrift(String now) {
        return service.evaluate(instant(now), now2 -> Optional.empty(), () -> driftSignal,
                Duration.ofMinutes(5));
    }

    private EmGoalAlertService newService(EmGoalAlertNotifier notifier) {
        return new EmGoalAlertService(goals, segments, alerts, notifier, clock, ZONE);
    }

    private void save(String start, String end) {
        segments.saveAll(List.of(new EmSegment(0, EmObservationKind.OBSERVED, CODE, null, null,
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
