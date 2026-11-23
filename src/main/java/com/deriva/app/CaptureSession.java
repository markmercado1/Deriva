package com.deriva.app;

import com.deriva.app.usecase.CaptureControl;
import com.deriva.app.usecase.CaptureSnapshot;
import com.deriva.app.usecase.CaptureState;
import com.deriva.app.usecase.CaptureStatus;
import com.deriva.bridge.BrowserStateStore;
import com.deriva.bridge.http.LocalBridgeServer;
import com.deriva.coach.DriftStateMachine;
import com.deriva.coach.model.CloseReason;
import com.deriva.coach.model.DriftUpdate;
import com.deriva.privacy.Observation;
import com.deriva.privacy.PrivacyFilter;
import com.deriva.sensor.ActivitySample;
import com.deriva.sensor.windows.WindowsIdleDetector;
import com.deriva.sensor.windows.WindowsWindowSensor;
import com.deriva.storage.StorageException;
import com.deriva.storage.sqlite.SQLiteDataMaintenance;
import com.deriva.storage.sqlite.SQLiteDriftRepository;
import com.deriva.storage.sqlite.SQLitePrivacyCheck;
import com.deriva.storage.sqlite.SQLiteSegmentRepository;
import com.deriva.tracking.Segmenter;
import com.deriva.tracking.model.Segment;
import com.deriva.tracking.model.TickResult;

import java.net.BindException;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Real capture session owned by the app. Sampling and persistence run on a
 * dedicated thread; the UI only observes status and immutable snapshots.
 */
public final class CaptureSession implements CaptureControl {
    public record BridgeConfig(int port, String token, String extensionId) {
        public BridgeConfig {
            if (port < 1024 || port > 65535) throw new IllegalArgumentException("port");
            if (token == null || !token.matches("[0-9a-f]{64}")) throw new IllegalArgumentException("token");
            if (extensionId == null || !extensionId.matches("[a-p]{32}"))
                throw new IllegalArgumentException("extensionId");
        }
    }

    private static final Set<String> CAPTURE_BROWSERS = Set.of("chrome.exe", "brave.exe");

    private final Path databasePath;
    private final BridgeConfig bridgeConfig;
    private final Clock clock;
    private final Duration idleThreshold;
    private final Object lock = new Object();
    private final AtomicBoolean running = new AtomicBoolean(false);

    private volatile boolean pauseRequested;
    private volatile CaptureStatus status = CaptureStatus.stopped();
    private volatile CaptureSnapshot snapshot = CaptureSnapshot.stopped();
    private volatile Thread thread;

    public CaptureSession(Path databasePath, BridgeConfig bridgeConfig, Clock clock,
                          Duration idleThreshold) {
        this.databasePath = Objects.requireNonNull(databasePath, "databasePath").toAbsolutePath();
        this.bridgeConfig = bridgeConfig;
        this.clock = Objects.requireNonNull(clock, "clock");
        this.idleThreshold = Objects.requireNonNull(idleThreshold, "idleThreshold");
    }

    @Override
    public CaptureStatus status() {
        return status;
    }

    @Override
    public CaptureSnapshot snapshot() {
        return snapshot;
    }

    @Override
    public void start() {
        synchronized (lock) {
            CaptureState state = status.state();
            if (state == CaptureState.STARTING || state == CaptureState.RUNNING
                    || state == CaptureState.STOPPING) return;
            pauseRequested = false;
            running.set(true);
            status = new CaptureStatus(CaptureState.STARTING, "Iniciando captura…");
            thread = new Thread(this::runLoop, "deriva-capture");
            thread.setDaemon(false);
            thread.start();
        }
    }

    @Override
    public void stop() {
        synchronized (lock) {
            if (status.state() == CaptureState.PAUSED) {
                status = CaptureStatus.stopped();
                return;
            }
            if (!running.get() && status.state() != CaptureState.STARTING) return;
            pauseRequested = false;
            running.set(false);
            if (status.state() != CaptureState.ERROR)
                status = new CaptureStatus(CaptureState.STOPPING, "Deteniendo captura…");
        }
    }

    @Override
    public void pause() {
        synchronized (lock) {
            if (!running.get()) return;
            pauseRequested = true;
            running.set(false);
            if (status.state() != CaptureState.ERROR)
                status = new CaptureStatus(CaptureState.STOPPING, "Pausando captura…");
        }
    }

    @Override
    public void deleteAllRecords() {
        boolean wasActive = running.get() || status.state() == CaptureState.STARTING
                || status.state() == CaptureState.RUNNING || status.state() == CaptureState.PAUSED
                || status.state() == CaptureState.STOPPING;
        stop();
        Thread captureThread = thread;
        if (captureThread != null && captureThread != Thread.currentThread()) {
            try {
                captureThread.join(10_000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        if (captureThread != null && captureThread.isAlive()) {
            status = new CaptureStatus(CaptureState.ERROR,
                    "No se pudo detener la captura; no se borraron los registros.");
            return;
        }
        try {
            SQLiteDataMaintenance.deleteActivity(databasePath);
            snapshot = CaptureSnapshot.stopped();
            status = new CaptureStatus(CaptureState.STOPPED, wasActive
                    ? "Registros borrados. Captura detenida."
                    : "Registros borrados.");
        } catch (RuntimeException e) {
            snapshot = CaptureSnapshot.stopped();
            status = new CaptureStatus(CaptureState.ERROR,
                    "No se pudieron borrar los registros: " + e.getMessage());
        }
    }

    @Override
    public void shutdown() {
        stop();
        Thread captureThread = thread;
        if (captureThread != null && captureThread != Thread.currentThread()) {
            try {
                captureThread.join(10_000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            if (captureThread.isAlive())
                status = new CaptureStatus(CaptureState.ERROR,
                        "La captura no terminó al cerrar la aplicación.");
        }
    }

    private void runLoop() {
        LocalBridgeServer bridge = null;
        SQLiteSegmentRepository segmentRepository = null;
        SQLiteDriftRepository driftRepository = null;
        Segmenter segmenter = new Segmenter();
        DriftStateMachine drift = new DriftStateMachine();
        boolean failed = false;
        String failure = null;
        try {
            segmentRepository = new SQLiteSegmentRepository(databasePath);
            driftRepository = new SQLiteDriftRepository(databasePath);
            driftRepository.recoverOpenEpisodes();
            SQLitePrivacyCheck.verify(databasePath);

            BrowserStateStore browserState = new BrowserStateStore(clock, System::nanoTime);
            if (bridgeConfig != null) {
                bridge = new LocalBridgeServer(bridgeConfig.port(), bridgeConfig.token(),
                        bridgeConfig.extensionId(), browserState, System::nanoTime);
                bridge.start();
            }
            PrivacyFilter filter = new PrivacyFilter(ProcessHandle.current().pid(), idleThreshold,
                    bridge == null ? Optional::empty : browserState::freshNormalHost,
                    CAPTURE_BROWSERS);

            WindowsWindowSensor sensor = new WindowsWindowSensor();
            WindowsIdleDetector idleDetector = new WindowsIdleDetector();

            status = new CaptureStatus(CaptureState.RUNNING, bridge == null
                    ? "Captura activa · sin bridge; navegadores excluidos."
                    : "Captura activa · bridge 127.0.0.1:" + bridgeConfig.port()
                    + " · Chrome/Brave + un perfil.");

            while (running.get()) {
                Instant tickStart = clock.instant();
                ActivitySample sample = sensor.sample();
                Duration idleTime;
                try {
                    idleTime = idleDetector.idleTime();
                } catch (RuntimeException e) {
                    sample = unavailable(sample.timestamp());
                    idleTime = Duration.ZERO;
                }

                Observation observation = filter.filter(sample, idleTime);
                TickResult result = segmenter.onObservation(observation);
                saveSegments(segmentRepository, result.completedSegments());

                List<DriftUpdate> driftUpdates = drift.onTick(result.tick());
                for (DriftUpdate update : driftUpdates) driftRepository.upsertDetection(update);

                Segmenter.Snapshot live = segmenter.snapshot(clock.instant());
                snapshot = new CaptureSnapshot(live.at(), live.open(), live.pending(), true);

                long elapsed = Duration.between(tickStart, clock.instant()).toMillis();
                if (elapsed < 1000) Thread.sleep(1000 - elapsed);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (Throwable e) {
            failed = true;
            failure = "La captura se detuvo por un error: " + failureMessage(e);
        } finally {
            boolean pausedClose = pauseRequested;
            pauseRequested = false;
            try {
                if (segmentRepository != null && driftRepository != null) {
                    Instant closingAt = clock.instant();
                    saveSegments(segmentRepository, segmenter.close(closingAt));
                    List<DriftUpdate> exitUpdates = drift.close(closingAt,
                            pausedClose ? CloseReason.PAUSE : CloseReason.EXIT);
                    for (DriftUpdate update : exitUpdates) driftRepository.upsertDetection(update);
                }
            } catch (RuntimeException e) {
                failed = true;
                String closeFailure = "No se pudo cerrar la captura: " + failureMessage(e);
                failure = failure == null ? closeFailure : failure + " · el cierre también falló.";
            } finally {
                if (bridge != null) bridge.close();
                running.set(false);
                snapshot = CaptureSnapshot.stopped();
                status = failed
                        ? new CaptureStatus(CaptureState.ERROR, failure == null
                        ? "La captura se detuvo con error." : failure)
                        : pausedClose
                        ? CaptureStatus.paused()
                        : new CaptureStatus(CaptureState.STOPPED, "Captura detenida.");
            }
        }
    }

    private static String failureMessage(Throwable error) {
        if (error instanceof BindException)
            return "no se pudo abrir el bridge; el puerto está ocupado.";
        if (error instanceof StorageException)
            return "no se pudo guardar en la base local.";
        return error.getClass().getSimpleName();
    }

    private static ActivitySample unavailable(Instant timestamp) {
        return new ActivitySample(timestamp, null, 0, 0, false, false);
    }

    private static void saveSegments(SQLiteSegmentRepository repository, List<Segment> segments) {
        if (!segments.isEmpty()) repository.saveAll(segments);
    }
}
