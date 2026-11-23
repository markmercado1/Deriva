package com.deriva.app;

import com.deriva.coach.model.CloseReason;
import com.deriva.coach.model.DriftEpisode;
import com.deriva.coach.model.DriftEvent;
import com.deriva.coach.model.DriftStatus;
import com.deriva.coach.model.DriftUpdate;
import com.deriva.privacy.ObservationKind;
import com.deriva.storage.sqlite.SQLiteDriftRepository;
import com.deriva.storage.sqlite.SQLiteSegmentRepository;
import com.deriva.tracking.model.ContextKey;
import com.deriva.tracking.model.Segment;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/** Synthetic data for the F4.1 UI preview; never used by the real capture path. */
public final class UiDemoData {
    private static final ContextKey WHATSAPP = new ContextKey("chrome.exe", "web.whatsapp.com");
    private static final ContextKey INSTAGRAM = new ContextKey("chrome.exe", "instagram.com");
    private static final ContextKey YOUTUBE = new ContextKey("chrome.exe", "youtube.com");
    private static final ContextKey DOCS = new ContextKey("chrome.exe", "docs.google.com");
    private static final ContextKey GMAIL = new ContextKey("chrome.exe", "mail.google.com");
    private static final ContextKey CHROME = new ContextKey("chrome.exe", null);
    private static final ContextKey VSCODE = new ContextKey("code.exe", null);
    private static final ContextKey WORD = new ContextKey("winword.exe", null);
    private static final ContextKey TERMINAL = new ContextKey("windowsterminal.exe", null);

    private UiDemoData() {}

    /** Seeds only a fresh database so a second launch can verify persisted annotations. */
    public static void seedIfMissing(Path databasePath) throws IOException {
        if (!Files.exists(databasePath)) {
            seed(databasePath);
            return;
        }
        ZoneId zone = ZoneId.systemDefault();
        Instant now = Instant.now();
        SQLiteSegmentRepository segments = new SQLiteSegmentRepository(databasePath);
        if (segments.between(now.minus(2, ChronoUnit.DAYS), now.plus(1, ChronoUnit.DAYS)).isEmpty())
            seedSegments(databasePath, LocalDate.now(zone), zone);
    }

    public static void seed(Path databasePath) throws IOException {
        Files.deleteIfExists(databasePath);
        Files.deleteIfExists(Path.of(databasePath + "-wal"));
        Files.deleteIfExists(Path.of(databasePath + "-shm"));
        if (databasePath.getParent() != null) Files.createDirectories(databasePath.getParent());

        SQLiteDriftRepository repository = new SQLiteDriftRepository(databasePath);
        ZoneId zone = ZoneId.systemDefault();
        LocalDate day = LocalDate.now(zone);

        addStable(repository, 1, at(day, 11, 8, zone), 134_000L, 10, INSTAGRAM,
                Set.of(INSTAGRAM, VSCODE, DOCS));
        addStable(repository, 2, at(day, 9, 12, zone), 98_000L, 11, DOCS,
                Set.of(DOCS, CHROME));
        addUnconfirmed(repository, 3, at(day, 9, 41, zone), 14, CHROME,
                Set.of(CHROME, VSCODE));
        addStable(repository, 4, at(day, 8, 36, zone), 182_000L, 10, YOUTUBE,
                Set.of(YOUTUBE, GMAIL));
        addStable(repository, 5, at(day, 8, 14, zone), 72_000L, 13, VSCODE,
                Set.of(VSCODE, TERMINAL));
        addStable(repository, 6, at(day, 8, 2, zone), 126_000L, 10, GMAIL,
                Set.of(GMAIL, CHROME));
        addOpen(repository, 7, at(day, 14, 32, zone), 12, WHATSAPP,
                Set.of(WHATSAPP, INSTAGRAM, DOCS));
        addStable(repository, 8, at(day.minusDays(1), 23, 50, zone), 900_000L, 10, GMAIL,
                Set.of(GMAIL, CHROME));

        Instant dayStart = day.atStartOfDay(zone).toInstant();
        Instant dayEnd = day.plusDays(1).atStartOfDay(zone).toInstant();
        List<DriftEvent> events = repository.between(dayStart, dayEnd);
        annotate(repository, events, at(day, 11, 8, zone), "MESSAGES", true, zone);
        annotate(repository, events, at(day, 9, 12, zone), "SEARCHING_INFO", true, zone);
        annotate(repository, events, at(day, 8, 14, zone), "BREAK", false, zone);

        seedSegments(databasePath, day, zone);
    }

    private static void seedSegments(Path databasePath, LocalDate day, ZoneId zone) {
        List<Segment> segments = new ArrayList<>();
        addSegment(segments, ObservationKind.OBSERVED, VSCODE,
                at(day, 8, 0, zone), at(day, 8, 30, zone));
        addSegment(segments, ObservationKind.OBSERVED, INSTAGRAM,
                at(day, 8, 30, zone), at(day, 9, 0, zone));
        addSegment(segments, ObservationKind.OBSERVED, YOUTUBE,
                at(day, 9, 0, zone), at(day, 9, 45, zone));
        addSegment(segments, ObservationKind.OBSERVED, CHROME,
                at(day, 9, 45, zone), at(day, 10, 0, zone));
        addSegment(segments, ObservationKind.OBSERVED, WORD,
                at(day, 10, 0, zone), at(day, 10, 20, zone));
        addSegment(segments, ObservationKind.IDLE, null,
                at(day, 10, 20, zone), at(day, 10, 35, zone));
        addSegment(segments, ObservationKind.EXCLUDED, null,
                at(day, 10, 35, zone), at(day, 10, 45, zone));
        addSegment(segments, ObservationKind.UNAVAILABLE, null,
                at(day, 10, 45, zone), at(day, 11, 0, zone));
        addSegment(segments, ObservationKind.OBSERVED, INSTAGRAM,
                at(day, 11, 0, zone), at(day, 11, 20, zone));
        addSegment(segments, ObservationKind.OBSERVED, TERMINAL,
                at(day, 11, 20, zone), at(day, 11, 50, zone));
        addSegment(segments, ObservationKind.OBSERVED, YOUTUBE,
                at(day.minusDays(1), 23, 50, zone), at(day, 0, 20, zone));
        new SQLiteSegmentRepository(databasePath).saveAll(segments);
    }

    private static void addSegment(List<Segment> segments, ObservationKind kind,
                                   ContextKey context, Instant start, Instant end) {
        segments.add(new Segment(0, kind, context, null, null, start, end));
    }

    private static void addStable(SQLiteDriftRepository repository, long id, Instant start,
                                  long timeMs, int switches, ContextKey first,
                                  Set<ContextKey> contexts) {
        DriftEpisode open = new DriftEpisode(id, start, start.plusSeconds(60), start.plusSeconds(60),
                null, null, DriftStatus.OPEN, null, switches, first, null, contexts);
        repository.upsertDetection(new DriftUpdate(DriftUpdate.Type.OPENED, open));
        Instant stable = start.plusMillis(timeMs);
        DriftEpisode closed = new DriftEpisode(id, start, start.plusSeconds(60), stable,
                stable, stable, DriftStatus.STABLE, null, switches, first, timeMs, contexts);
        repository.upsertDetection(new DriftUpdate(DriftUpdate.Type.CLOSED, closed));
    }

    private static void addUnconfirmed(SQLiteDriftRepository repository, long id, Instant start,
                                       int switches, ContextKey first, Set<ContextKey> contexts) {
        DriftEpisode open = new DriftEpisode(id, start, start.plusSeconds(60), start.plusSeconds(60),
                null, null, DriftStatus.OPEN, null, switches, first, null, contexts);
        repository.upsertDetection(new DriftUpdate(DriftUpdate.Type.OPENED, open));
        Instant closed = start.plusSeconds(120);
        DriftEpisode unconfirmed = new DriftEpisode(id, start, start.plusSeconds(60), closed,
                null, closed, DriftStatus.UNCONFIRMED, CloseReason.IDLE, switches, first, null, contexts);
        repository.upsertDetection(new DriftUpdate(DriftUpdate.Type.CLOSED, unconfirmed));
    }

    private static void addOpen(SQLiteDriftRepository repository, long id, Instant start,
                                int switches, ContextKey first, Set<ContextKey> contexts) {
        DriftEpisode open = new DriftEpisode(id, start, start.plusSeconds(60), start.plusSeconds(180),
                null, null, DriftStatus.OPEN, null, switches, first, null, contexts);
        repository.upsertDetection(new DriftUpdate(DriftUpdate.Type.OPENED, open));
    }

    private static void annotate(SQLiteDriftRepository repository, List<DriftEvent> events,
                                 Instant start, String reason, boolean justified, ZoneId zone) {
        events.stream()
                .filter(event -> event.startTs().equals(start))
                .findFirst()
                .ifPresent(event -> repository.annotate(event.id(), reason, justified,
                        start.plusSeconds(300)));
    }

    private static Instant at(LocalDate day, int hour, int minute, ZoneId zone) {
        return day.atTime(hour, minute).atZone(zone).toInstant();
    }
}
