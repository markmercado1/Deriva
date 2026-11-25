package com.deriva.storage.sqlite;

import com.deriva.coach.EmDriftStateMachine;
import com.deriva.coach.model.EmCloseReason;
import com.deriva.coach.model.EmDriftEpisode;
import com.deriva.coach.model.EmDriftEvent;
import com.deriva.coach.model.EmDriftStatus;
import com.deriva.coach.model.EmDriftUpdate;
import com.deriva.privacy.EmObservationKind;
import com.deriva.storage.EmStorageException;
import com.deriva.tracking.model.EmContextKey;
import com.deriva.tracking.model.EmStableTick;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class EmSQLiteDriftRepositoryTest {
    private static final Instant BASE = Instant.parse("2026-11-22T10:00:00Z");
    private static final EmContextKey A = new EmContextKey("a.exe", null);
    private static final EmContextKey B = new EmContextKey("chrome.exe", "chess.com");

    @TempDir Path tempDirectory;

    private static Instant at(int second) {
        return BASE.plusSeconds(second);
    }

    private Path database() {
        return tempDirectory.resolve("drift.db");
    }

    private static EmDriftEpisode open(long id, int last, int switches, EmContextKey first,
                                     Set<EmContextKey> contexts) {
        return new EmDriftEpisode(id, at(0), at(10), at(last), null, null,
                EmDriftStatus.OPEN, null, switches, first, null, contexts);
    }

    @Test
    void persistsOpenUpdatesStableAndPreservesAnnotations() {
        EmSQLiteDriftRepository repository = new EmSQLiteDriftRepository(database());
        repository.upsertDetection(new EmDriftUpdate(EmDriftUpdate.Type.OPENED,
                open(1, 10, 10, A, Set.of(A, B))));

        List<EmDriftEvent> first = repository.between(at(0), at(200));
        assertEquals(1, first.size());
        long databaseId = first.getFirst().id();
        assertEquals(10, first.getFirst().switchCount());
        assertEquals(Set.of(A, B), first.getFirst().contexts());

        repository.annotate(databaseId, "BREAK", true, at(11));
        repository.upsertDetection(new EmDriftUpdate(EmDriftUpdate.Type.UPDATED,
                open(1, 11, 11, A, Set.of(A, B, new EmContextKey("word.exe", null)))));

        EmDriftEpisode stable = new EmDriftEpisode(1, at(0), at(10), at(190), at(190), at(190),
                EmDriftStatus.STABLE, null, 11, A, 190_000L,
                Set.of(A, B, new EmContextKey("word.exe", null)));
        repository.upsertDetection(new EmDriftUpdate(EmDriftUpdate.Type.CLOSED, stable));

        EmDriftEvent event = repository.between(at(0), at(200)).getFirst();
        assertEquals(EmDriftStatus.STABLE, event.status());
        assertEquals(11, event.switchCount());
        assertEquals(3, event.contexts().size());
        assertEquals("BREAK", event.reason());
        assertTrue(event.justified());
        assertEquals(at(11), event.answeredAt());
        assertEquals(190_000L, event.timeToStabilityMs());
    }

    @Test
    void pauseClosesOpenEpisodeWithPauseReason() {
        EmSQLiteDriftRepository repository = new EmSQLiteDriftRepository(database());
        EmDriftStateMachine detector = new EmDriftStateMachine();
        detector.onTick(observed("a.exe", 0, 0));
        List<EmDriftUpdate> updates = List.of();
        for (int i = 1; i <= 10; i++)
            updates = detector.onTick(observed(i % 2 == 1 ? "b.exe" : "a.exe", i, i));
        assertEquals(EmDriftUpdate.Type.OPENED, updates.getFirst().type());
        for (EmDriftUpdate update : updates) repository.upsertDetection(update);

        List<EmDriftUpdate> closed = detector.close(at(20), EmCloseReason.PAUSE);
        assertEquals(1, closed.size());
        repository.upsertDetection(closed.getFirst());

        EmDriftEvent event = new EmSQLiteDriftRepository(database()).between(at(0), at(200)).getFirst();
        assertEquals(EmDriftStatus.UNCONFIRMED, event.status());
        assertEquals(EmCloseReason.PAUSE, event.closeReason());
        assertEquals(at(20), event.closedAt());
    }

    private static EmStableTick observed(String process, int second, int since) {
        return new EmStableTick(at(second), EmObservationKind.OBSERVED,
                new EmContextKey(process, null), at(since), false);
    }

    @Test
    void recoveryConvertsOpenToUnconfirmedRestart() {
        EmSQLiteDriftRepository repository = new EmSQLiteDriftRepository(database());
        repository.upsertDetection(new EmDriftUpdate(EmDriftUpdate.Type.OPENED,
                open(1, 10, 10, A, Set.of(A))));
        long id = repository.between(at(0), at(20)).getFirst().id();
        repository.annotate(id, "OTHER", false, at(12));

        assertEquals(1, repository.recoverOpenEpisodes());
        EmDriftEvent event = repository.between(at(0), at(20)).getFirst();
        assertEquals(EmDriftStatus.UNCONFIRMED, event.status());
        assertEquals(EmCloseReason.RESTART, event.closeReason());
        assertEquals(at(10), event.closedAt());
        assertNull(event.stabilityStartTs());
        assertNull(event.timeToStabilityMs());
        assertEquals("OTHER", event.reason());
    }

    @Test
    void betweenReturnsOverlappingEventsAndSpecificContexts() {
        EmSQLiteDriftRepository repository = new EmSQLiteDriftRepository(database());
        repository.upsertDetection(new EmDriftUpdate(EmDriftUpdate.Type.OPENED,
                open(1, 100, 10, A, Set.of(A))));
        repository.upsertDetection(new EmDriftUpdate(EmDriftUpdate.Type.OPENED,
                open(2, 300, 10, B, Set.of(B))));

        List<EmDriftEvent> events = repository.between(at(50), at(250));
        assertEquals(2, events.size());
        assertEquals(at(0), events.get(0).startTs());
        assertEquals(at(0), events.get(1).startTs());
        assertEquals(Set.of(A), events.get(0).contexts());
        assertEquals(Set.of(B), events.get(1).contexts());
        assertTrue(repository.between(at(400), at(500)).isEmpty());
    }

    @Test
    void annotationSurvivesRepositoryReopen() {
        Path database = database();
        EmSQLiteDriftRepository first = new EmSQLiteDriftRepository(database);
        first.upsertDetection(new EmDriftUpdate(EmDriftUpdate.Type.OPENED,
                open(1, 10, 10, A, Set.of(A))));
        long id = first.between(at(0), at(20)).getFirst().id();
        first.annotate(id, "OTHER", true, at(12));

        EmDriftEvent event = new EmSQLiteDriftRepository(database)
                .between(at(0), at(20)).getFirst();
        assertEquals("OTHER", event.reason());
        assertTrue(event.justified());
        assertEquals(at(12), event.answeredAt());
        assertEquals(EmDriftStatus.OPEN, event.status());
        assertNull(event.closedAt());
        assertNull(event.closeReason());
    }

    @Test
    void annotationDoesNotReopenClosedEpisode() {
        EmSQLiteDriftRepository repository = new EmSQLiteDriftRepository(database());
        repository.upsertDetection(new EmDriftUpdate(EmDriftUpdate.Type.OPENED,
                open(1, 10, 10, A, Set.of(A))));
        long id = repository.between(at(0), at(20)).getFirst().id();
        EmDriftEpisode stable = new EmDriftEpisode(1, at(0), at(10), at(190), at(190), at(190),
                EmDriftStatus.STABLE, null, 10, A, 190_000L, Set.of(A));
        repository.upsertDetection(new EmDriftUpdate(EmDriftUpdate.Type.CLOSED, stable));

        repository.annotate(id, "BREAK", true, at(200));

        EmDriftEvent event = repository.between(at(0), at(300)).getFirst();
        assertEquals(EmDriftStatus.STABLE, event.status());
        assertEquals(at(190), event.closedAt());
        assertEquals(at(200), event.answeredAt());
    }

    @Test
    void annotateRejectsUnknownIdAndInvalidReason() {
        EmSQLiteDriftRepository repository = new EmSQLiteDriftRepository(database());
        repository.upsertDetection(new EmDriftUpdate(EmDriftUpdate.Type.OPENED,
                open(1, 10, 10, A, Set.of(A))));
        long id = repository.between(at(0), at(20)).getFirst().id();

        assertThrows(EmStorageException.class, () -> repository.annotate(999, "BREAK", false, at(11)));
        assertThrows(IllegalArgumentException.class, () -> repository.annotate(id, "NOT_A_REASON", false, at(11)));
    }

    @Test
    void longEpisodeCrossingMidnightIsNotTruncated() {
        Instant start = Instant.parse("2026-11-22T23:00:00Z");
        Instant detected = start.plusSeconds(600);
        Instant stable = start.plusSeconds(1_800);
        Instant closed = start.plusSeconds(4_200);
        EmDriftEpisode episode = new EmDriftEpisode(1, start, detected, closed, stable, closed,
                EmDriftStatus.STABLE, null, 10, A, 1_800_000L, Set.of(A));
        EmSQLiteDriftRepository repository = new EmSQLiteDriftRepository(database());
        repository.upsertDetection(new EmDriftUpdate(EmDriftUpdate.Type.OPENED,
                new EmDriftEpisode(1, start, detected, detected, null, null,
                        EmDriftStatus.OPEN, null, 10, A, null, Set.of(A))));
        repository.upsertDetection(new EmDriftUpdate(EmDriftUpdate.Type.CLOSED, episode));

        EmDriftEvent event = repository.between(start, closed.plusSeconds(1)).getFirst();
        assertEquals(closed, event.closedAt());
        assertEquals(1_800_000L, event.timeToStabilityMs());
        assertEquals(EmDriftStatus.STABLE, event.status());
    }
}
