package com.deriva.app;

import com.deriva.app.usecase.EmDailyUsageService;
import com.deriva.app.usecase.EmGoalAlertMonitor;
import com.deriva.app.usecase.EmGoalAlertService;
import com.deriva.app.usecase.EmGoalsService;
import com.deriva.app.usecase.EmInterruptionsService;
import com.deriva.app.usecase.EmSettingsService;
import com.deriva.storage.sqlite.EmSQLiteDriftRepository;
import com.deriva.storage.sqlite.EmSQLiteGoalAlertRepository;
import com.deriva.storage.sqlite.EmSQLiteGoalRepository;
import com.deriva.storage.sqlite.EmSQLiteSegmentRepository;
import com.deriva.storage.sqlite.EmSQLiteSettingsRepository;
import com.deriva.ui.fx.EmMainController;
import com.deriva.ui.viewmodel.EmBrowserExtensionViewModel;
import com.deriva.ui.viewmodel.EmDailyUsageViewModel;
import com.deriva.ui.viewmodel.EmGoalsViewModel;
import com.deriva.ui.viewmodel.EmInterruptionsViewModel;
import com.deriva.ui.viewmodel.EmAlertDeferralSettingsViewModel;
import com.deriva.ui.viewmodel.EmPassiveMaxSettingsViewModel;
import com.deriva.ui.viewmodel.EmSettingsViewModel;
import javafx.animation.PauseTransition;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Rectangle2D;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.PixelReader;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.Screen;
import javafx.util.Duration;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.ZoneId;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;

/** JavaFX entry point; reads persisted drift but never captures. */
public final class EmDerivaApp extends Application {
    private static final AtomicBoolean SHUTDOWN_DONE = new AtomicBoolean(false);
    private static volatile EmAppTray tray;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) {
        boolean demo = flag("DERIVA_UI_DEMO");
        Path databasePath = EmAppPaths.database(System.getenv(), System.getProperty("user.home"),
                demo, EmAppPaths.packaged());

        CompletableFuture.supplyAsync(() -> {
            try {
                if (demo) EmUiDemoData.seedIfMissing(databasePath);
                Clock clock = Clock.systemUTC();
                ZoneId zone = ZoneId.systemDefault();
                EmSQLiteDriftRepository driftRepository = new EmSQLiteDriftRepository(databasePath);
                EmSQLiteSegmentRepository segmentRepository = new EmSQLiteSegmentRepository(databasePath);
                EmInterruptionsService interruptionsService =
                        new EmInterruptionsService(driftRepository, clock);
                EmDailyUsageService usageService = new EmDailyUsageService(segmentRepository, clock);
                EmSettingsService settingsService =
                        new EmSettingsService(new EmSQLiteSettingsRepository(databasePath));
                EmSQLiteGoalRepository goalRepository = new EmSQLiteGoalRepository(databasePath, clock);
                EmSQLiteGoalAlertRepository alertRepository = new EmSQLiteGoalAlertRepository(databasePath);
                EmGoalsService goalsService =
                        new EmGoalsService(goalRepository, segmentRepository, alertRepository, clock);
                EmGoalAlertService alertService = new EmGoalAlertService(goalRepository, segmentRepository,
                        alertRepository,
                        notice -> {
                            EmAppTray current = tray;
                            if (current != null) current.notifyGoal(notice);
                        }, clock, zone);
                alertService.recoverAfterRestart(clock.instant());
                EmCaptureSession capture = new EmCaptureSession(databasePath, EmDerivaApp::bridgeConfig,
                        clock, settingsService, goalsService, new EmGoalAlertMonitor(alertService));
                EmGoalsViewModel goals = new EmGoalsViewModel(goalsService, zone,
                        now -> java.util.Optional.ofNullable(capture.snapshot().open()));
                EmSettingsViewModel settings = new EmSettingsViewModel();
                settings.load(settingsService.snapshot(), capture.effectiveSettings());
                EmPassiveMaxSettingsViewModel passiveMax = new EmPassiveMaxSettingsViewModel();
                passiveMax.load(settingsService.passiveMaxSnapshot(), capture.effectivePassiveMax());
                EmAlertDeferralSettingsViewModel alertDeferral = new EmAlertDeferralSettingsViewModel();
                alertDeferral.load(settingsService.alertDeferralSnapshot(),
                        capture.effectiveAlertDeferral());
                EmBrowserExtensionViewModel extension = new EmBrowserExtensionViewModel(
                        new EmBridgeSettingsFile(bridgeFile(), extensionFolder(), System.getenv()),
                        EmBridgeSettingsFile::newToken);
                extension.load();
                return new ViewModels(
                        new EmInterruptionsViewModel(interruptionsService, zone),
                        new EmDailyUsageViewModel(usageService, zone, capture),
                        goals,
                        settings,
                        passiveMax,
                        alertDeferral,
                        settingsService,
                        capture,
                        extension);
            } catch (Exception e) {
                throw new CompletionException(e);
            }
        }).whenComplete((viewModels, error) -> Platform.runLater(() -> {
            if (error != null) showStartupError(stage, error);
            else showMain(stage, viewModels, demo);
        }));
    }

    private static void showMain(Stage stage, ViewModels viewModels, boolean demo) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    EmDerivaApp.class.getResource("/com/deriva/ui/main.fxml"));
            Parent root = loader.load();
            EmMainController controller = loader.getController();
            controller.init(viewModels.interruptions(), viewModels.usage(),
                    viewModels.goals(), viewModels.settings(), viewModels.passiveMax(),
                    viewModels.alertDeferral(), viewModels.settingsService(),
                    viewModels.capture(), viewModels.extension(), demo);

            Scene scene = new Scene(root, 1440, 900);
            scene.getStylesheets().add(EmDerivaApp.class
                    .getResource("/com/deriva/ui/interrupciones.css").toExternalForm());
            stage.setTitle("Deriva — Actividad");
            stage.setScene(scene);
            fitInitialWindow(stage);

            String smoke = environment("DERIVA_UI_SMOKE", "");
            boolean trayAllowed = smoke.isBlank() || "tray".equalsIgnoreCase(smoke);
            EmAppTray installed = trayAllowed
                    ? EmAppTray.install(stage, viewModels.capture(),
                    () -> exitApplication(controller))
                    : null;
            tray = installed;
            if (installed != null) {
                Platform.setImplicitExit(false);
                stage.setOnCloseRequest(event -> {
                    event.consume();
                    stage.hide();
                    installed.notifyHidden();
                });
            } else {
                stage.setOnHidden(event -> shutdownOnce(controller));
            }
            stage.show();

            if ("usage".equalsIgnoreCase(environment("DERIVA_UI_VIEW", "")))
                controller.showUsageView();
            else if ("goals".equalsIgnoreCase(environment("DERIVA_UI_VIEW", "")))
                controller.showGoalsView();
            else if ("settings".equalsIgnoreCase(environment("DERIVA_UI_VIEW", "")))
                controller.showSettingsView();

            String screenshot = System.getenv("DERIVA_UI_SCREENSHOT");
            if ("annotate".equalsIgnoreCase(smoke)) {
                EmUiSmoke.annotateFirstRow(scene, () -> finish(scene, controller, screenshot));
            } else if ("invalid-range".equalsIgnoreCase(smoke)) {
                EmUiSmoke.showInvalidRange(scene, () -> finish(scene, controller, screenshot));
            } else if ("capture".equalsIgnoreCase(smoke)) {
                EmUiSmoke.captureCycle(scene, 10, () -> saveLiveSnapshot(scene, screenshot),
                        () -> finish(scene, controller, screenshot));
            } else if ("capture-close".equalsIgnoreCase(smoke)) {
                EmUiSmoke.captureThenClose(scene, 5, () -> finish(scene, controller, screenshot));
            } else if ("shutdown-close".equalsIgnoreCase(smoke)) {
                EmUiSmoke.captureThenWindowClose(scene, 5,
                        () -> saveScreenshot(scene, screenshot));
            } else if ("capture-hold".equalsIgnoreCase(smoke)) {
                EmUiSmoke.captureThenWindowClose(scene, holdSeconds(),
                        () -> saveScreenshot(scene, screenshot));
            } else if ("delete".equalsIgnoreCase(smoke)) {
                EmUiSmoke.deleteData(scene, 4, () -> finish(scene, controller, screenshot));
            } else if ("delete-confirm".equalsIgnoreCase(smoke)) {
                EmUiSmoke.showDeleteConfirmation(scene, () -> finish(scene, controller, screenshot));
            } else if ("capture-failure".equalsIgnoreCase(smoke)) {
                EmUiSmoke.captureFailure(scene, 4, () -> finish(scene, controller, screenshot));
            } else if ("demo-episodes".equalsIgnoreCase(smoke)) {
                EmUiSmoke.demoEpisodes(scene, () -> finish(scene, controller, screenshot));
            } else if ("pause".equalsIgnoreCase(smoke)) {
                EmUiSmoke.pauseResume(scene, 4,
                        () -> saveScreenshot(scene, variantPath(screenshot, "-paused")),
                        () -> finish(scene, controller, screenshot));
            } else if ("tray".equalsIgnoreCase(smoke)) {
                EmUiSmoke.trayCycle(scene, tray,
                        () -> saveScreenshot(scene, screenshot),
                        () -> finish(scene, controller, screenshot));
            } else if ("settings".equalsIgnoreCase(smoke)) {
                EmUiSmoke.settingsCycle(scene, () -> finish(scene, controller, screenshot));
            } else if ("settings-session".equalsIgnoreCase(smoke)) {
                EmUiSmoke.settingsSessionCycle(scene, () -> finish(scene, controller, screenshot));
            } else if ("goals".equalsIgnoreCase(smoke)) {
                EmUiSmoke.goalsCycle(scene,
                        () -> saveScreenshot(scene, variantPath(screenshot, "-creado")),
                        () -> finish(scene, controller, screenshot));
            } else if ("goals-justify".equalsIgnoreCase(smoke)) {
                EmUiSmoke.goalsJustifyCycle(scene,
                        () -> saveScreenshot(scene, variantPath(screenshot, "-justificado")),
                        () -> finish(scene, controller, screenshot));
            } else if (screenshot != null && !screenshot.isBlank()) {
                PauseTransition pause = new PauseTransition(Duration.seconds(1.2));
                pause.setOnFinished(event -> finish(scene, controller, screenshot));
                pause.play();
            }
        } catch (Exception e) {
            showStartupError(stage, e);
        }
    }

    private static void showStartupError(Stage stage, Throwable error) {
        Label title = new Label("No se pudo abrir la base local");
        title.getStyleClass().add("page-title");
        Label detail = new Label(message(error));
        detail.getStyleClass().add("page-subtitle");
        detail.setWrapText(true);
        Button close = new Button("Cerrar");
        close.getStyleClass().add("primary-button");
        close.setOnAction(event -> Platform.exit());

        VBox root = new VBox(16, title, detail, close);
        root.getStyleClass().add("content");
        Scene scene = new Scene(root, 760, 320);
        scene.getStylesheets().add(EmDerivaApp.class
                .getResource("/com/deriva/ui/interrupciones.css").toExternalForm());
        stage.setTitle("Deriva — Error de base local");
        stage.setMinWidth(640);
        stage.setMinHeight(280);
        stage.setScene(scene);
        stage.show();
    }

    private static void fitInitialWindow(Stage stage) {
        // Visual bounds are logical pixels: they account for Windows scaling and
        // exclude the taskbar. Size the whole decorated window, not just its scene.
        Rectangle2D bounds = Screen.getPrimary().getVisualBounds();
        double width = Math.min(1440, bounds.getWidth() * 0.94);
        double height = Math.min(900, bounds.getHeight() * 0.94);
        stage.setMinWidth(Math.min(1180, width));
        stage.setMinHeight(Math.min(720, height));
        stage.setWidth(width);
        stage.setHeight(height);
        stage.setX(bounds.getMinX() + (bounds.getWidth() - width) / 2);
        stage.setY(bounds.getMinY() + (bounds.getHeight() - height) / 2);
    }

    private static void saveLiveSnapshot(Scene scene, String screenshot) {
        if (screenshot == null || screenshot.isBlank()) return;
        try {
            String live = screenshot.toLowerCase().endsWith(".png")
                    ? screenshot.substring(0, screenshot.length() - 4) + "-live.png"
                    : screenshot + "-live.png";
            Path target = Path.of(live).toAbsolutePath();
            saveSnapshot(scene, target);
            System.out.println("UI live screenshot: " + target);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void finish(Scene scene, EmMainController controller, String screenshot) {
        saveScreenshot(scene, screenshot);
        if (tray != null) {
            exitApplication(controller);
            return;
        }
        shutdownOnce(controller);
        Platform.exit();
    }

    private static void exitApplication(EmMainController controller) {
        EmAppTray current = tray;
        tray = null;
        if (current != null) current.remove();
        shutdownOnce(controller);
        Platform.exit();
        // The AWT tray keeps its own native loop; capture is already stopped and joined.
        if (current != null) System.exit(0);
    }

    private static void shutdownOnce(EmMainController controller) {
        if (!SHUTDOWN_DONE.compareAndSet(false, true)) return;
        controller.shutdown();
        reportCaptureThreads();
    }

    private static void saveScreenshot(Scene scene, String screenshot) {
        if (screenshot == null || screenshot.isBlank()) return;
        try {
            Path target = Path.of(screenshot).toAbsolutePath();
            saveSnapshot(scene, target);
            System.out.println("UI screenshot: " + target);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void reportCaptureThreads() {
        System.out.println("UI shutdown: hilos deriva-capture vivos="
                + liveThreads("deriva-capture") + "; deriva-goals vivos="
                + liveThreads("deriva-goals"));
    }

    private static long liveThreads(String name) {
        return Thread.getAllStackTraces().keySet().stream()
                .filter(thread -> thread.isAlive() && name.equals(thread.getName()))
                .count();
    }

    private static void saveSnapshot(Scene scene, Path target) throws Exception {
        scene.getRoot().applyCss();
        scene.getRoot().layout();
        WritableImage image = scene.snapshot(null);
        int width = (int) image.getWidth();
        int height = (int) image.getHeight();
        BufferedImage buffered = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        PixelReader reader = image.getPixelReader();
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                buffered.setRGB(x, y, reader.getArgb(x, y));
            }
        }
        if (target.getParent() != null) Files.createDirectories(target.getParent());
        ImageIO.write(buffered, "png", target.toFile());
    }

    private static String variantPath(String screenshot, String suffix) {
        if (screenshot == null || screenshot.isBlank()) return null;
        return screenshot.toLowerCase().endsWith(".png")
                ? screenshot.substring(0, screenshot.length() - 4) + suffix + ".png"
                : screenshot + suffix + ".png";
    }

    private static String message(Throwable error) {
        Throwable cause = error;
        while ((cause instanceof CompletionException || cause instanceof ExecutionException)
                && cause.getCause() != null) {
            cause = cause.getCause();
        }
        String text = cause.getMessage() == null
                ? cause.getClass().getSimpleName() : cause.getMessage();
        if (cause.getCause() != null && cause.getCause().getMessage() != null) {
            text += " (" + cause.getCause().getMessage() + ")";
        }
        return text;
    }

    private static boolean flag(String name) {
        String value = System.getenv(name);
        return value != null && (value.equals("1") || value.equalsIgnoreCase("true"));
    }

    private static EmCaptureSession.BridgeConfig bridgeConfig() {
        String token = System.getenv("DERIVA_BRIDGE_TOKEN");
        String extension = System.getenv("DERIVA_EXTENSION_ID");
        if (token == null || token.isBlank() || extension == null || extension.isBlank()) {
            // Without Start-Ui.ps1 -ExtensionId, use what was saved in Ajustes > Extensión del navegador.
            return EmAppPaths.bridgeFromFile(bridgeFile()).orElse(null);
        }
        int port = Integer.parseInt(environment("DERIVA_BRIDGE_PORT", "47821"));
        return new EmCaptureSession.BridgeConfig(port, token, extension);
    }

    private static Path bridgeFile() {
        return EmAppPaths.dataDirectory(System.getenv(), System.getProperty("user.home"))
                .resolve(EmAppPaths.BRIDGE_FILE);
    }

    /** Unpacked extension: next to Deriva.exe when packaged, extension/chromium in the source tree. */
    private static Path extensionFolder() {
        String exe = System.getProperty("jpackage.app-path");
        if (exe != null) return Path.of(exe).getParent().resolve("extension");
        return Path.of("extension", "chromium").toAbsolutePath();
    }

    private static int holdSeconds() {
        try {
            int seconds = Integer.parseInt(environment("DERIVA_UI_HOLD_SECONDS", "60"));
            return seconds >= 5 && seconds <= 7200 ? seconds : 60;
        } catch (NumberFormatException e) {
            return 60;
        }
    }

    private static String environment(String name, String fallback) {
        String value = System.getenv(name);
        return value == null || value.isBlank() ? fallback : value;
    }

    private record ViewModels(EmInterruptionsViewModel interruptions, EmDailyUsageViewModel usage,
                              EmGoalsViewModel goals, EmSettingsViewModel settings,
                              EmPassiveMaxSettingsViewModel passiveMax,
                              EmAlertDeferralSettingsViewModel alertDeferral,
                              EmSettingsService settingsService, EmCaptureSession capture,
                              EmBrowserExtensionViewModel extension) {}
}
