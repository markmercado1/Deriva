package com.deriva.app;

import com.deriva.app.usecase.EmCaptureControl;
import com.deriva.app.usecase.EmCaptureSettingsPolicy;
import com.deriva.app.usecase.EmCaptureSnapshot;
import com.deriva.app.usecase.EmCaptureState;
import com.deriva.app.usecase.EmCaptureStatus;
import com.deriva.app.usecase.EmGoalAlertMonitor;
import com.deriva.app.usecase.EmSettingsService;
import com.deriva.bridge.EmBrowserStateStore;
import com.deriva.bridge.http.EmLocalBridgeServer;
import com.deriva.coach.EmDriftStateMachine;
import com.deriva.coach.model.EmCloseReason;
import com.deriva.coach.model.EmDetectionSettings;
import com.deriva.coach.model.EmDriftUpdate;
import com.deriva.goals.EmDriftSignal;
import com.deriva.goals.EmPassiveActivity;
import com.deriva.goals.EmPassiveGoalProvider;
import com.deriva.goals.EmPassiveGoalSnapshot;
import com.deriva.privacy.EmObservation;
import com.deriva.privacy.EmObservationKind;
import com.deriva.privacy.EmPrivacyFilter;
import com.deriva.sensor.EmActivitySample;
import com.deriva.sensor.windows.EmWindowsIdleDetector;
import com.deriva.sensor.windows.EmWindowsWindowSensor;
import com.deriva.storage.EmStorageException;
import com.deriva.storage.sqlite.EmSQLiteDataMaintenance;
import com.deriva.storage.sqlite.EmSQLiteDriftRepository;
import com.deriva.storage.sqlite.EmSQLitePrivacyCheck;
import com.deriva.storage.sqlite.EmSQLiteSegmentRepository;
import com.deriva.tracking.EmSegmenter;
import com.deriva.tracking.model.EmSegment;
import com.deriva.tracking.model.EmTickResult;

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
public final class EmCaptureSession implements EmCaptureControl {
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
    private final java.util.function.Supplier<BridgeConfig> bridgeSource;
    private volatile BridgeConfig bridgeConfig;
    private final Clock clock;
    private final EmSettingsService settingsService;
    private final EmPassiveGoalProvider passiveGoals;
    private final EmGoalAlertMonitor goalAlerts;
    private final Object lock = new Object();
    private final AtomicBoolean running = new AtomicBoolean(false);

    private volatile boolean pauseRequested;
    private volatile EmCaptureStatus status = EmCaptureStatus.stopped();
    private volatile EmCaptureSnapshot snapshot = EmCaptureSnapshot.stopped();
    private volatile EmDetectionSettings effectiveSettings;
    private volatile Duration effectivePassiveMax;
    private volatile Duration effectiveAlertDeferral;
    private volatile Thread thread;

    public EmCaptureSession(Path databasePath, BridgeConfig bridgeConfig, Clock clock,
                          EmSettingsService settingsService, EmPassiveGoalProvider passiveGoals,
                          EmGoalAlertMonitor goalAlerts) {
        this(databasePath, () -> bridgeConfig, clock, settingsService, passiveGoals, goalAlerts);
    }

    /**
     * The bridge settings are read again on every start (not on resume), so a change saved in
     * Ajustes applies after Detener/Iniciar, like the detection settings.
     */
    public EmCaptureSession(Path databasePath, java.util.function.Supplier<BridgeConfig> bridgeSource,
                          Clock clock, EmSettingsService settingsService,
                          EmPassiveGoalProvider passiveGoals, EmGoalAlertMonitor goalAlerts) {
        this.databasePath = Objects.requireNonNull(databasePath, "databasePath").toAbsolutePath();
        this.bridgeSource = Objects.requireNonNull(bridgeSource, "bridgeSource");
        this.clock = Objects.requireNonNull(clock, "clock");
        this.settingsService = Objects.requireNonNull(settingsService, "settingsService");
        this.passiveGoals = Objects.requireNonNull(passiveGoals, "passiveGoals");
        this.goalAlerts = Objects.requireNonNull(goalAlerts, "goalAlerts");
        this.effectivePassiveMax = settingsService.passiveMax();
        this.effectiveAlertDeferral = settingsService.alertDeferralMax();
    }

    @Override
    public EmCaptureStatus status() {
        return status;
    }

    @Override
    public EmCaptureSnapshot snapshot() {
        return snapshot;
    }

    @Override
    public EmDetectionSettings effectiveSettings() {
        EmCaptureState state = status.state();
        EmDetectionSettings session = effectiveSettings;
        if (session != null && (state == EmCaptureState.STARTING || state == EmCaptureState.RUNNING
                || state == EmCaptureState.PAUSED || state == EmCaptureState.STOPPING))
            return session;
        return settingsService.current();
    }

    @Override
    public Duration effectivePassiveMax() {
        EmCaptureState state = status.state();
        Duration session = effectivePassiveMax;
        if (session != null && (state == EmCaptureState.STARTING || state == EmCaptureState.RUNNING
                || state == EmCaptureState.PAUSED || state == EmCaptureState.STOPPING))
            return session;
        return settingsService.passiveMax();
    }

    @Override
    public Duration effectiveAlertDeferral() {
        EmCaptureState state = status.state();
        Duration session = effectiveAlertDeferral;
        if (session != null && (state == EmCaptureState.STARTING || state == EmCaptureState.RUNNING
                || state == EmCaptureState.PAUSED || state == EmCaptureState.STOPPING))
            return session;
        return settingsService.alertDeferralMax();
    }

    @Override
    public void start() {
        synchronized (lock) {
            EmCaptureState state = status.state();
            if (state == EmCaptureState.STARTING || state == EmCaptureState.RUNNING
                    || state == EmCaptureState.STOPPING) return;
            pauseRequested = false;
            effectiveSettings = EmCaptureSettingsPolicy.onStart(state, effectiveSettings,
                    settingsService.current());
            effectivePassiveMax = EmCaptureSettingsPolicy.onStart(state, effectivePassiveMax,
                    settingsService.passiveMax());
            effectiveAlertDeferral = EmCaptureSettingsPolicy.onStart(state, effectiveAlertDeferral,
                    settingsService.alertDeferralMax());
            if (state != EmCaptureState.PAUSED) bridgeConfig = readBridge();
            running.set(true);
            status = new EmCaptureStatus(EmCaptureState.STARTING, "Iniciando captura…");
            thread = new Thread(this::runLoop, "deriva-capture");
            thread.setDaemon(false);
            thread.start();
        }
    }

    @Override
    public void stop() {
        synchronized (lock) {
            if (status.state() == EmCaptureState.PAUSED) {
                status = EmCaptureStatus.stopped();
                return;
            }
            if (!running.get() && status.state() != EmCaptureState.STARTING) return;
            pauseRequested = false;
            running.set(false);
            if (status.state() != EmCaptureState.ERROR)
                status = new EmCaptureStatus(EmCaptureState.STOPPING, "Deteniendo captura…");
        }
    }

    @Override
    public void pause() {
        synchronized (lock) {
            if (!running.get()) return;
            pauseRequested = true;
            running.set(false);
            if (status.state() != EmCaptureState.ERROR)
                status = new EmCaptureStatus(EmCaptureState.STOPPING, "Pausando captura…");
        }
    }

    @Override
    public void deleteAllRecords() {
        boolean wasActive = running.get() || status.state() == EmCaptureState.STARTING
                || status.state() == EmCaptureState.RUNNING || status.state() == EmCaptureState.PAUSED
                || status.state() == EmCaptureState.STOPPING;
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
            status = new EmCaptureStatus(EmCaptureState.ERROR,
                    "No se pudo detener la captura; no se borraron los registros.");
            return;
        }
        try {
            EmSQLiteDataMaintenance.deleteActivity(databasePath);
            snapshot = EmCaptureSnapshot.stopped();
            status = new EmCaptureStatus(EmCaptureState.STOPPED, wasActive
                    ? "Registros borrados. Captura detenida."
                    : "Registros borrados.");
        } catch (RuntimeException e) {
            snapshot = EmCaptureSnapshot.stopped();
            status = new EmCaptureStatus(EmCaptureState.ERROR,
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
                status = new EmCaptureStatus(EmCaptureState.ERROR,
                        "La captura no terminó al cerrar la aplicación.");
        }
    }

    private BridgeConfig readBridge() {
        try {
            return bridgeSource.get();
        } catch (RuntimeException e) {
            System.err.println("Deriva: configuración de la extensión no válida; navegadores excluidos.");
            return null;
        }
    }

    private void runLoop() {
        BridgeConfig bridgeConfig = this.bridgeConfig;
        EmLocalBridgeServer bridge = null;
        EmSQLiteSegmentRepository segmentRepository = null;
        EmSQLiteDriftRepository driftRepository = null;
        EmSegmenter segmenter = new EmSegmenter();
        EmDetectionSettings sessionSettings = effectiveSettings;
        EmDriftStateMachine drift = new EmDriftStateMachine(sessionSettings);
        DriftTracker driftTracker = new DriftTracker();
        boolean failed = false;
        String failure = null;
        try {
            segmentRepository = new EmSQLiteSegmentRepository(databasePath);
            driftRepository = new EmSQLiteDriftRepository(databasePath);
            driftRepository.recoverOpenEpisodes();
            EmSQLitePrivacyCheck.verify(databasePath);
            Duration sessionPassiveMax = effectivePassiveMax;
            Duration sessionAlertDeferral = effectiveAlertDeferral;
            EmPassiveGoalSnapshot goalSnapshot = passiveGoals.snapshot();
            long goalsRevision = goalSnapshot.revision();
            EmPassiveActivity passiveActivity = new EmPassiveActivity(sessionPassiveMax,
                    goalSnapshot.activeGoals());

            EmBrowserStateStore browserState = new EmBrowserStateStore(clock, System::nanoTime);
            if (bridgeConfig != null) {
                bridge = new EmLocalBridgeServer(bridgeConfig.port(), bridgeConfig.token(),
                        bridgeConfig.extensionId(), browserState, System::nanoTime);
                bridge.start();
            }
            EmPrivacyFilter filter = new EmPrivacyFilter(ProcessHandle.current().pid(),
                    sessionSettings.idle(),
                    bridge == null ? Optional::empty : browserState::freshNormalHost,
                    CAPTURE_BROWSERS);

            EmWindowsWindowSensor sensor = new EmWindowsWindowSensor();
            EmWindowsIdleDetector idleDetector = new EmWindowsIdleDetector();

            status = new EmCaptureStatus(EmCaptureState.RUNNING, bridge == null
                    ? "Captura activa · sin bridge; navegadores excluidos."
                    : "Captura activa · bridge 127.0.0.1:" + bridgeConfig.port()
                    + " · Chrome/Brave + un perfil.");

            while (running.get()) {
                Instant tickStart = clock.instant();
                EmPassiveGoalSnapshot currentGoals = passiveGoals.snapshot();
                if (currentGoals.revision() != goalsRevision) {
                    passiveActivity.updateGoals(currentGoals.activeGoals());
                    goalsRevision = currentGoals.revision();
                }
                EmActivitySample sample = sensor.sample();
                Duration idleTime;
                try {
                    idleTime = idleDetector.idleTime();
                } catch (RuntimeException e) {
                    sample = unavailable(sample.timestamp());
                    idleTime = Duration.ZERO;
                }

                EmObservation observation = filter.filter(sample, idleTime);
                EmObservation foreground = observation.kind() == EmObservationKind.IDLE
                        && passiveActivity.hasContexts()
                        ? filter.filter(sample, Duration.ZERO)
                        : observation;
                observation = passiveActivity.apply(observation, foreground, idleTime);
                EmTickResult result = segmenter.onObservation(observation);
                saveSegments(segmentRepository, result.completedSegments());

                List<EmDriftUpdate> driftUpdates = drift.onTick(result.tick());
                driftTracker.apply(driftUpdates);
                for (EmDriftUpdate update : driftUpdates) driftRepository.upsertDetection(update);

                Instant now = clock.instant();
                EmSegmenter.Snapshot live = segmenter.snapshot(now);
                snapshot = new EmCaptureSnapshot(live.at(), live.open(), live.pending(), true);
                goalAlerts.tick(status.state(), observation.kind(), now,
                        at -> Optional.ofNullable(segmenter.snapshot(at).open()),
                        driftTracker::current, sessionAlertDeferral);

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
                    List<EmDriftUpdate> exitUpdates = drift.close(closingAt,
                            pausedClose ? EmCloseReason.PAUSE : EmCloseReason.EXIT);
                    for (EmDriftUpdate update : exitUpdates) driftRepository.upsertDetection(update);
                }
            } catch (RuntimeException e) {
                failed = true;
                String closeFailure = "No se pudo cerrar la captura: " + failureMessage(e);
                failure = failure == null ? closeFailure : failure + " · el cierre también falló.";
            } finally {
                goalAlerts.interrupt(clock.instant());
                if (bridge != null) bridge.close();
                running.set(false);
                snapshot = EmCaptureSnapshot.stopped();
                status = failed
                        ? new EmCaptureStatus(EmCaptureState.ERROR, failure == null
                        ? "La captura se detuvo con error." : failure)
                        : pausedClose
                        ? EmCaptureStatus.paused()
                        : new EmCaptureStatus(EmCaptureState.STOPPED, "Captura detenida.");
            }
        }
    }

    private static String failureMessage(Throwable error) {
        if (error instanceof BindException)
            return "no se pudo abrir el bridge; el puerto está ocupado.";
        if (error instanceof EmStorageException)
            return "no se pudo guardar en la base local.";
        return error.getClass().getSimpleName();
    }

    private static EmActivitySample unavailable(Instant timestamp) {
        return new EmActivitySample(timestamp, null, 0, 0, false, false);
    }

    private static void saveSegments(EmSQLiteSegmentRepository repository, List<EmSegment> segments) {
        if (!segments.isEmpty()) repository.saveAll(segments);
    }

    /** Read-only view of the drift updates already produced by the state machine. */
    private static final class DriftTracker {
        private EmDriftSignal signal = EmDriftSignal.inactive();

        void apply(List<EmDriftUpdate> updates) {
            for (EmDriftUpdate update : updates) {
                switch (update.type()) {
                    case OPENED -> signal = EmDriftSignal.activeOf(
                            signal.generation() + 1, update.episode().detectedAt());
                    case CLOSED -> signal = new EmDriftSignal(false, signal.generation(),
                            update.episode().closedAt());
                    case UPDATED -> { }
                }
            }
        }

        EmDriftSignal current() {
            return signal;
        }
    }
}
