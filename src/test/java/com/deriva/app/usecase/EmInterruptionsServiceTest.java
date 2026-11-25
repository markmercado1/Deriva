package com.deriva.app.usecase;

import com.deriva.coach.model.EmDriftEpisode;
import com.deriva.coach.model.EmDriftStatus;
import com.deriva.coach.model.EmDriftUpdate;
import com.deriva.storage.sqlite.EmSQLiteDriftRepository;
import com.deriva.tracking.model.EmContextKey;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EmInterruptionsServiceTest {
    private static final ZoneId LIMA = ZoneId.of("America/Lima");
    private static final EmContextKey CONTEXT = new EmContextKey("chrome.exe", "youtube.com");

    @TempDir Path tempDirectory;

    @Test
    void crossMidnightEpisodeAppearsInBothDaysAndOnlyOnceInWideRange() {
        EmSQLiteDriftRepository repository = new EmSQLiteDriftRepository(
                tempDirectory.resolve("range.db"));
        Instant start = ZonedDateTime.of(2026, 11, 21, 23, 50, 0, 0, LIMA).toInstant();
        Instant detected = start.plusSeconds(60);
        Instant stable = ZonedDateTime.of(2026, 11, 22, 0, 5, 0, 0, LIMA).toInstant();

        EmDriftEpisode open = new EmDriftEpisode(1, start, detected, detected, null, null,
                EmDriftStatus.OPEN, null, 10, CONTEXT, null, Set.of(CONTEXT));
        EmDriftEpisode closed = new EmDriftEpisode(1, start, detected, stable, stable, stable,
                EmDriftStatus.STABLE, null, 10, CONTEXT,
                Duration.between(start, stable).toMillis(), Set.of(CONTEXT));
        repository.upsertDetection(new EmDriftUpdate(EmDriftUpdate.Type.OPENED, open));
        repository.upsertDetection(new EmDriftUpdate(EmDriftUpdate.Type.CLOSED, closed));

        EmInterruptionsService service = new EmInterruptionsService(repository,
                Clock.fixed(stable, LIMA));

        assertEquals(1, service.forRange(LocalDate.of(2026, 11, 21),
                LocalDate.of(2026, 11, 21), LIMA).size());
        assertEquals(1, service.forRange(LocalDate.of(2026, 11, 22),
                LocalDate.of(2026, 11, 22), LIMA).size());
        assertEquals(1, service.forRange(LocalDate.of(2026, 11, 20),
                LocalDate.of(2026, 11, 23), LIMA).size());
        assertEquals(0, service.forRange(LocalDate.of(2026, 11, 23),
                LocalDate.of(2026, 11, 23), LIMA).size());
    }
}
