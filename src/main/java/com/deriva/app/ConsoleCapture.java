package com.deriva.app;

import com.deriva.bridge.BrowserStateStore;
import com.deriva.bridge.http.LocalBridgeServer;
import com.deriva.coach.DriftStateMachine;
import com.deriva.coach.model.CloseReason;
import com.deriva.coach.model.DriftEpisode;
import com.deriva.coach.model.DriftUpdate;
import com.deriva.privacy.Observation;
import com.deriva.privacy.PrivacyFilter;
import com.deriva.sensor.ActivitySample;
import com.deriva.sensor.windows.WindowsIdleDetector;
import com.deriva.sensor.windows.WindowsWindowSensor;
import com.deriva.storage.sqlite.SQLiteDriftRepository;
import com.deriva.storage.sqlite.SQLitePrivacyCheck;
import com.deriva.storage.sqlite.SQLiteSegmentRepository;
import com.deriva.tracking.Segmenter;
import com.deriva.tracking.model.Segment;
import com.deriva.tracking.model.StableTick;
import com.deriva.tracking.model.TickResult;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * F3.1b temporary vertical slice: real sensor + bridge/privacy + segmenter + SQLite,
 * with console output. It is not the final UI, daily-use calculator or production
 * writer/queue. The bridge is started in this process; the extension must be configured
 * for the same port and token.
 */
public final class ConsoleCapture {
    private static final Duration IDLE_THRESHOLD = Duration.ofSeconds(120);
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm:ss");

    private ConsoleCapture() {}

    public static void main(String[] args) throws Exception {
        if (args.length < 1 || args.length > 2) {
            System.err.println("Uso: ConsoleCapture <extension-id> [puerto]. Token: DERIVA_BRIDGE_TOKEN.");
            return;
        }
        String extensionId = args[0];
        if (!extensionId.matches("[a-p]{32}")) throw new IllegalArgumentException("extension-id invalido");
        int port = args.length == 2 ? Integer.parseInt(args[1]) : 47821;
        if (port < 1024 || port > 65535) throw new IllegalArgumentException("puerto invalido");

        String token = System.getenv("DERIVA_BRIDGE_TOKEN");
        if (token == null || !token.matches("[0-9a-f]{64}"))
            throw new IllegalArgumentException("Configurar DERIVA_BRIDGE_TOKEN (64 hex)");

        Path databasePath = Path.of(environment("DERIVA_CONSOLE_DB", "work/console-day/deriva.db"))
                .toAbsolutePath();
        Path stopFile = Path.of(environment("DERIVA_CONSOLE_STOP_FILE", "work/console-day/STOP"))
                .toAbsolutePath();
        if (databasePath.getParent() != null) Files.createDirectories(databasePath.getParent());
        if (stopFile.getParent() != null) Files.createDirectories(stopFile.getParent());
        Files.deleteIfExists(stopFile);

        SQLiteSegmentRepository repository = new SQLiteSegmentRepository(databasePath);
        SQLiteDriftRepository driftRepository = new SQLiteDriftRepository(databasePath);
        int recovered = driftRepository.recoverOpenEpisodes();
        SQLitePrivacyCheck.verify(databasePath);

        BrowserStateStore browserState = new BrowserStateStore(Clock.systemUTC(), System::nanoTime);
        LocalBridgeServer bridge = new LocalBridgeServer(port, token, extensionId, browserState, System::nanoTime);
        WindowsWindowSensor sensor = new WindowsWindowSensor();
        WindowsIdleDetector idleDetector = new WindowsIdleDetector();
        PrivacyFilter filter = new PrivacyFilter(ProcessHandle.current().pid(), IDLE_THRESHOLD,
                browserState::freshNormalHost);
        Segmenter segmenter = new Segmenter();
        DriftStateMachine drift = new DriftStateMachine();

        AtomicBoolean running = new AtomicBoolean(true);
        CountDownLatch finished = new CountDownLatch(1);
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            running.set(false);
            try {
                finished.await(10, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, "deriva-console-shutdown"));

        bridge.start();
        System.out.println("ConsoleCapture activo en 127.0.0.1:" + port + ". Captura de sitios: Chrome/Brave + un perfil.");
        System.out.println("Base: " + databasePath);
        System.out.println("Detener: Ctrl+C o crear el archivo " + stopFile);
        System.out.println("Privacidad: sin URL/titulo; hosts validados y ventana privada excluida.");
        if (recovered > 0)
            System.out.println("Drift: " + recovered + " episodio(s) OPEN recuperados como UNCONFIRMED/RESTART.");

        ZoneId zone = ZoneId.systemDefault();
        String lastObservation = null;
        try {
            while (running.get()) {
                if (Files.exists(stopFile)) {
                    System.out.println("STOP detectado; cerrando captura.");
                    break;
                }

                Instant tickStart = Instant.now();
                ActivitySample sample = sensor.sample();
                Duration idleTime;
                try {
                    idleTime = idleDetector.idleTime();
                } catch (RuntimeException e) {
                    sample = unavailable(sample.timestamp());
                    idleTime = Duration.ZERO;
                    System.out.println("Aviso: detector de inactividad no disponible; muestra UNAVAILABLE.");
                }

                Observation observation = filter.filter(sample, idleTime);
                TickResult result = segmenter.onObservation(observation);
                saveSegments(repository, result.completedSegments(), zone);

                String key = observationKey(observation);
                if (!key.equals(lastObservation)) {
                    System.out.println(formatObservation(observation, zone));
                    lastObservation = key;
                }
                List<DriftUpdate> driftUpdates = drift.onTick(result.tick());
                for (DriftUpdate update : driftUpdates) driftRepository.upsertDetection(update);
                printDrift(driftUpdates);

                long elapsed = Duration.between(tickStart, Instant.now()).toMillis();
                if (elapsed < 1000) Thread.sleep(1000 - elapsed);
            }
        } finally {
            try {
                Instant closingAt = Instant.now();
                saveSegments(repository, segmenter.close(closingAt), zone);
                List<DriftUpdate> exitUpdates = drift.close(closingAt, CloseReason.EXIT);
                for (DriftUpdate update : exitUpdates) driftRepository.upsertDetection(update);
                printDrift(exitUpdates);
            } catch (RuntimeException e) {
                System.err.println("No se pudo guardar el cierre de captura: " + e.getMessage());
            }
            bridge.close();

            try {
                LocalDate today = LocalDate.now(zone);
                Instant dayStart = today.atStartOfDay(zone).toInstant();
                Instant summaryEnd = Instant.now();
                List<Segment> todaySegments = repository.between(dayStart, summaryEnd);
                ConsoleSummary.Summary summary = ConsoleSummary.of(todaySegments, dayStart, summaryEnd);
                System.out.println(ConsoleSummary.format(summary));
            } catch (RuntimeException e) {
                System.err.println("No se pudo generar el resumen: " + e.getMessage());
            }
            Files.deleteIfExists(stopFile);
            finished.countDown();
        }
    }

    private static ActivitySample unavailable(Instant timestamp) {
        return new ActivitySample(timestamp, null, 0, 0, false, false);
    }

    private static void saveSegments(SQLiteSegmentRepository repository, List<Segment> segments, ZoneId zone) {
        if (segments.isEmpty()) return;
        repository.saveAll(segments);
        for (Segment segment : segments) System.out.println(formatSegment(segment, zone));
    }

    private static String formatObservation(Observation observation, ZoneId zone) {
        String time = TIME.format(observation.timestamp().atZone(zone));
        if (observation.kind() == com.deriva.privacy.ObservationKind.OBSERVED) {
            String site = observation.siteHost() == null ? "" : " site=" + observation.siteHost();
            return time + " OBSERVED " + observation.processName() + site;
        }
        return time + " " + observation.kind();
    }

    private static String observationKey(Observation observation) {
        return observation.kind() + "|" + Objects.toString(observation.processName(), "")
                + "|" + Objects.toString(observation.siteHost(), "");
    }

    private static String formatSegment(Segment segment, ZoneId zone) {
        String from = TIME.format(segment.start().atZone(zone));
        String to = TIME.format(segment.end().atZone(zone));
        String identity = segment.context() == null ? ""
                : " " + segment.context().processName()
                + (segment.context().siteHost() == null ? "" : "@" + segment.context().siteHost());
        return "segment " + segment.kind() + identity + " " + from + " -> " + to
                + " (" + ConsoleSummary.duration(Duration.between(segment.start(), segment.end())) + ")";
    }

    private static void printDrift(List<DriftUpdate> updates) {
        for (DriftUpdate update : updates) {
            DriftEpisode episode = update.episode();
            if (update.type() == DriftUpdate.Type.CLOSED) {
                System.out.println("drift CLOSE id=" + episode.id() + " status=" + episode.status()
                        + " switches=" + episode.switchCount()
                        + (episode.closeReason() == null ? "" : " reason=" + episode.closeReason())
                        + (episode.timeToStabilityMs() == null ? "" : " timeToStabilityMs=" + episode.timeToStabilityMs()));
            } else {
                System.out.println("drift " + update.type() + " id=" + episode.id()
                        + " switches=" + episode.switchCount()
                        + " first=" + context(episode.firstChangeContext())
                        + " contexts=" + episode.contexts().stream().map(ConsoleCapture::context).toList());
            }
        }
    }

    private static String context(com.deriva.tracking.model.ContextKey context) {
        return context.siteHost() == null ? context.processName()
                : context.processName() + "@" + context.siteHost();
    }

    private static String environment(String name, String fallback) {
        String value = System.getenv(name);
        return value == null || value.isBlank() ? fallback : value;
    }
}
