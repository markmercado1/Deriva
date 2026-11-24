package com.deriva.storage.sqlite;

import com.deriva.coach.DriftStateMachine;
import com.deriva.coach.model.CloseReason;
import com.deriva.coach.model.DriftEpisode;
import com.deriva.coach.model.DriftEvent;
import com.deriva.coach.model.DriftStatus;
import com.deriva.coach.model.DriftUpdate;
import com.deriva.privacy.ObservationKind;
import com.deriva.storage.StorageException;
import com.deriva.tracking.model.ContextKey;
import com.deriva.tracking.model.StableTick;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class SQLiteDriftRepositoryTest {
    private static final Instant BASE = Instant.parse("2026-11-22T10:00:00Z");
    private static final ContextKey A = new ContextKey("a.exe", null);
    private static final ContextKey B = new ContextKey("chrome.exe", "chess.com");

    @TempDir Path tempDirectory;

    private static Instant at(int second) {
        return BASE.plusSeconds(second);
    }

    private Path database() {
        return tempDirectory.resolve("drift.db");
    }

    private static DriftEpisode open(long id, int last, int switches, ContextKey first,
                                     Set<ContextKey> contexts) {
        return new DriftEpisode(id, at(0), at(10), at(last), null, null,
                DriftStatus.OPEN, null, switches, first, null, contexts);
    }

    @Test
    void persistsOpenUpdatesStableAndPreservesAnnotations() {
        SQLiteDriftRepository repository = new SQLiteDriftRepository(database());
        repository.upsertDetection(new DriftUpdate(DriftUpdate.Type.OPENED,
                open(1, 10, 10, A, Set.of(A, B))));

        List<DriftEvent> first = repository.between(at(0), at(200));
        assertEquals(1, first.size());
        long databaseId = first.getFirst().id();
        assertEquals(10, first.getFirst().switchCount());
        assertEquals(Set.of(A, B), first.getFirst().contexts());

        repository.annotate(databaseId, "BREAK", true, at(11));
        repository.upsertDetection(new DriftUpdate(DriftUpdate.Type.UPDATED,
                open(1, 11, 11, A, Set.of(A, B, new ContextKey("word.exe", null)))));

        DriftEpisode stable = new DriftEpisode(1, at(0), at(10), at(190), at(190), at(190),
                DriftStatus.STABLE, null, 11, A, 190_000L,
                Set.of(A, B, new ContextKey("word.exe", null)));
        repository.upsertDetection(new DriftUpdate(DriftUpdate.Type.CLOSED, stable));

        DriftEvent event = repository.between(at(0), at(200)).getFirst();
        assertEquals(DriftStatus.STABLE, event.status());
        assertEquals(11, event.switchCount());
        assertEquals(3, event.contexts().size());
        assertEquals("BREAK", event.reason());
        assertTrue(event.justified());
        assertEquals(at(11), event.answeredAt());
        assertEquals(190_000L, event.timeToStabilityMs());
    }

    @Test
    void pauseClosesOpenEpisodeWithPauseReason() {
        SQLiteDriftRepository repository = new SQLiteDriftRepository(database());
        DriftStateMachine detector = new DriftStateMachine();
        detector.onTick(observed("a.exe", 0, 0));
        List<DriftUpdate> updates = List.of();
        for (int i = 1; i <= 10; i++)
            updates = detector.onTick(observed(i % 2 == 1 ? "b.exe" : "a.exe", i, i));
        assertEquals(DriftUpdate.Type.OPENED, updates.getFirst().type());
        for (DriftUpdate update : updates) repository.upsertDetection(update);

        List<DriftUpdate> closed = detector.close(at(20), CloseReason.PAUSE);
        assertEquals(1, closed.size());
        repository.upsertDetection(closed.getFirst());

        DriftEvent event = new SQLiteDriftRepository(database()).between(at(0), at(200)).getFirst();
        assertEquals(DriftStatus.UNCONFIRMED, event.status());
        assertEquals(CloseReason.PAUSE, event.closeReason());
        assertEquals(at(20), event.closedAt());
    }

    private static StableTick observed(String process, int second, int since) {
        return new StableTick(at(second), ObservationKind.OBSERVED,
                new ContextKey(process, null), at(since), false);
    }

    @Test
    void recoveryConvertsOpenToUnconfirmedRestart() {
        SQLiteDriftRepository repository = new SQLiteDriftRepository(database());
        repository.upsertDetection(new DriftUpdate(DriftUpdate.Type.OPENED,
                open(1, 10, 10, A, Set.of(A))));
        long id = repository.between(at(0), at(20)).getFirst().id();
        repository.annotate(id, "OTHER", false, at(12));

        assertEquals(1, repository.recoverOpenEpisodes());
        DriftEvent event = repository.between(at(0), at(20)).getFirst();
        assertEquals(DriftStatus.UNCONFIRMED, event.status());
        assertEquals(CloseReason.RESTART, event.closeReason());
        assertEquals(at(10), event.closedAt());
        assertNull(event.stabilityStartTs());
        assertNull(event.timeToStabilityMs());
        assertEquals("OTHER", event.reason());
    }

    @Test
    void betweenReturnsOverlappingEventsAndSpecificContexts() {
        SQLiteDriftRepository repository = new SQLiteDriftRepository(database());
        repository.upsertDetection(new DriftUpdate(DriftUpdate.Type.OPENED,
                open(1, 100, 10, A, Set.of(A))));
        repository.upsertDetection(new DriftUpdate(DriftUpdate.Type.OPENED,
                open(2, 300, 10, B, Set.of(B))));

        List<DriftEvent> events = repository.between(at(50), at(250));
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
        SQLiteDriftRepository first = new SQLiteDriftRepository(database);
        first.upsertDetection(new DriftUpdate(DriftUpdate.Type.OPENED,
                open(1, 10, 10, A, Set.of(A))));
        long id = first.between(at(0), at(20)).getFirst().id();
        first.annotate(id, "OTHER", true, at(12));

        DriftEvent event = new SQLiteDriftRepository(database)
                .between(at(0), at(20)).getFirst();
        assertEquals("OTHER", event.reason());
        assertTrue(event.justified());
        assertEquals(at(12), event.answeredAt());
        assertEquals(DriftStatus.OPEN, event.status());
        assertNull(event.closedAt());
        assertNull(event.closeReason());
    }

    @Test
    void annotationDoesNotReopenClosedEpisode() {
        SQLiteDriftRepository repository = new SQLiteDriftRepository(database());
        repository.upsertDetection(new DriftUpdate(DriftUpdate.Type.OPENED,
                open(1, 10, 10, A, Set.of(A))));
        long id = repository.between(at(0), at(20)).getFirst().id();
        DriftEpisode stable = new DriftEpisode(1, at(0), at(10), at(190), at(190), at(190),
                DriftStatus.STABLE, null, 10, A, 190_000L, Set.of(A));
        repository.upsertDetection(new DriftUpdate(DriftUpdate.Type.CLOSED, stable));

        repository.annotate(id, "BREAK", true, at(200));

        DriftEvent event = repository.between(at(0), at(300)).getFirst();
        assertEquals(DriftStatus.STABLE, event.status());
        assertEquals(at(190), event.closedAt());
        assertEquals(at(200), event.answeredAt());
    }

    @Test
    void annotateRejectsUnknownIdAndInvalidReason() {
        SQLiteDriftRepository repository = new SQLiteDriftRepository(database());
        repository.upsertDetection(new DriftUpdate(DriftUpdate.Type.OPENED,
                open(1, 10, 10, A, Set.of(A))));
        long id = repository.between(at(0), at(20)).getFirst().id();

        assertThrows(StorageException.class, () -> repository.annotate(999, "BREAK", false, at(11)));
        assertThrows(IllegalArgumentException.class, () -> repository.annotate(id, "NOT_A_REASON", false, at(11)));
    }

    @Test
    void longEpisodeCrossingMidnightIsNotTruncated() {
        Instant start = Instant.parse("2026-11-22T23:00:00Z");
        Instant detected = start.plusSeconds(600);
        Instant stable = start.plusSeconds(1_800);
        Instant closed = start.plusSeconds(4_200);
        DriftEpisode episode = new DriftEpisode(1, start, detected, closed, stable, closed,
                DriftStatus.STABLE, null, 10, A, 1_800_000L, Set.of(A));
        SQLiteDriftRepository repository = new SQLiteDriftRepository(database());
        repository.upsertDetection(new DriftUpdate(DriftUpdate.Type.OPENED,
                new DriftEpisode(1, start, detected, detected, null, null,
                        DriftStatus.OPEN, null, 10, A, null, Set.of(A))));
        repository.upsertDetection(new DriftUpdate(DriftUpdate.Type.CLOSED, episode));

        DriftEvent event = repository.between(start, closed.plusSeconds(1)).getFirst();
        assertEquals(closed, event.closedAt());
        assertEquals(1_800_000L, event.timeToStabilityMs());
        assertEquals(DriftStatus.STABLE, event.status());
    }
}
