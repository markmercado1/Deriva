package com.deriva.ui.fx;

import com.deriva.app.usecase.EmCaptureControl;
import com.deriva.app.usecase.EmCaptureState;
import com.deriva.app.usecase.EmCaptureStatus;
import com.deriva.app.usecase.EmSettingsService;
import com.deriva.ui.viewmodel.EmDailyUsageViewModel;
import com.deriva.ui.viewmodel.EmGoalsViewModel;
import com.deriva.ui.viewmodel.EmInterruptionsViewModel;
import com.deriva.ui.viewmodel.EmAlertDeferralSettingsViewModel;
import com.deriva.ui.viewmodel.EmPassiveMaxSettingsViewModel;
import com.deriva.ui.viewmodel.EmSettingsViewModel;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;

import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Shell with lateral navigation and capture controls shared by both views. */
public final class EmMainController {
    @FXML private StackPane content;
    @FXML private Button interruptionsButton;
    @FXML private Button usageButton;
    @FXML private Button goalsButton;
    @FXML private Button settingsButton;
    @FXML private Button captureButton;
    @FXML private Button pauseButton;
    @FXML private Label captureStatus;
    @FXML private Button deleteButton;
    @FXML private StackPane deleteOverlay;
    @FXML private Button confirmDeleteButton;
    @FXML private Label deleteWarning;

    private final Timeline capturePoll = new Timeline(
            new KeyFrame(Duration.millis(500), event -> updateCaptureStatus()));
    private final ExecutorService actionExecutor = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "deriva-ui-action");
        thread.setDaemon(true);
        return thread;
    });

    private Parent interruptionsRoot;
    private Parent usageRoot;
    private Parent goalsRoot;
    private Parent settingsRoot;
    private EmInterruptionsController interruptionsController;
    private EmUsageController usageController;
    private EmGoalsController goalsController;
    private EmSettingsController settingsController;
    private EmCaptureControl capture;
    private boolean initialized;
    private boolean deleting;

    public void init(EmInterruptionsViewModel interruptions, EmDailyUsageViewModel usage,
                     EmGoalsViewModel goals, EmSettingsViewModel settings,
                     EmPassiveMaxSettingsViewModel passiveMax,
                     EmAlertDeferralSettingsViewModel alertDeferral,
                     EmSettingsService settingsService,
                     EmCaptureControl capture, boolean demo) throws IOException {
        this.capture = Objects.requireNonNull(capture, "capture");

        FXMLLoader interruptionsLoader = new FXMLLoader(
                EmMainController.class.getResource("/com/deriva/ui/interrupciones.fxml"));
        interruptionsRoot = interruptionsLoader.load();
        interruptionsController = interruptionsLoader.getController();
        interruptionsController.init(interruptions, demo);

        FXMLLoader usageLoader = new FXMLLoader(
                EmMainController.class.getResource("/com/deriva/ui/uso-diario.fxml"));
        usageRoot = usageLoader.load();
        usageController = usageLoader.getController();
        usageController.init(usage, demo);

        FXMLLoader goalsLoader = new FXMLLoader(
                EmMainController.class.getResource("/com/deriva/ui/objetivos.fxml"));
        goalsRoot = goalsLoader.load();
        goalsController = goalsLoader.getController();
        goalsController.init(goals);

        FXMLLoader settingsLoader = new FXMLLoader(
                EmMainController.class.getResource("/com/deriva/ui/ajustes.fxml"));
        settingsRoot = settingsLoader.load();
        settingsController = settingsLoader.getController();
        settingsController.init(settings, passiveMax, alertDeferral, settingsService, capture);

        initialized = true;
        setContent(interruptionsRoot);
        setActive(interruptionsButton);
        updateCaptureStatus();
        capturePoll.setCycleCount(Animation.INDEFINITE);
        capturePoll.play();
    }

    @FXML
    private void showInterruptions() {
        if (!initialized) return;
        setContent(interruptionsRoot);
        setActive(interruptionsButton);
        usageController.stopAutoRefresh();
        goalsController.stopAutoRefresh();
        interruptionsController.refresh();
    }

    @FXML
    private void showUsage() {
        if (!initialized) return;
        setContent(usageRoot);
        setActive(usageButton);
        goalsController.stopAutoRefresh();
        usageController.startAutoRefresh();
        usageController.refresh();
    }

    @FXML
    private void showGoals() {
        if (!initialized) return;
        setContent(goalsRoot);
        setActive(goalsButton);
        usageController.stopAutoRefresh();
        goalsController.startAutoRefresh();
        goalsController.refresh();
    }

    @FXML
    private void showSettings() {
        settingsController.showOverview();
        if (!initialized) return;
        setContent(settingsRoot);
        setActive(settingsButton);
        usageController.stopAutoRefresh();
        goalsController.stopAutoRefresh();
        settingsController.refresh();
        settingsController.refreshPassive();
        settingsController.refreshDeferral();
    }

    @FXML
    private void toggleCapture() {
        if (capture == null || deleting) return;
        EmCaptureState state = capture.status().state();
        if (state == EmCaptureState.RUNNING || state == EmCaptureState.STARTING
                || state == EmCaptureState.PAUSED) capture.stop();
        else capture.start();
        updateCaptureStatus();
    }

    @FXML
    private void togglePause() {
        if (capture == null || deleting) return;
        EmCaptureState state = capture.status().state();
        if (state == EmCaptureState.RUNNING || state == EmCaptureState.STARTING) capture.pause();
        else if (state == EmCaptureState.PAUSED) capture.start();
        updateCaptureStatus();
    }

    @FXML
    private void requestDelete() {
        if (deleting) return;
        deleteOverlay.setManaged(true);
        deleteOverlay.setVisible(true);
        deleteOverlay.toFront();
        updateCaptureStatus();
    }

    @FXML
    private void cancelDelete() {
        if (deleting) return;
        deleteOverlay.setManaged(false);
        deleteOverlay.setVisible(false);
        updateCaptureStatus();
    }

    @FXML
    private void confirmDelete() {
        if (deleting || capture == null) return;
        deleteOverlay.setManaged(false);
        deleteOverlay.setVisible(false);
        deleting = true;
        deleteButton.setDisable(true);
        deleteButton.setText("Borrando…");
        updateCaptureStatus();

        CompletableFuture.runAsync(capture::deleteAllRecords, actionExecutor)
                .whenComplete((ignored, error) -> Platform.runLater(() -> {
                    deleting = false;
                    deleteButton.setDisable(false);
                    deleteButton.setText("Borrar registros");
                    updateCaptureStatus();
                    interruptionsController.refresh();
                    usageController.refresh();
                    goalsController.refresh();
                    if (error != null) {
                        captureStatus.setText("No se pudieron borrar los registros: "
                                + (error.getMessage() == null
                                ? error.getClass().getSimpleName() : error.getMessage()));
                        if (!captureStatus.getStyleClass().contains("capture-error"))
                            captureStatus.getStyleClass().add("capture-error");
                    }
                }));
    }

    public void showUsageView() {
        showUsage();
    }

    public void showSettingsView() {
        showSettings();
    }

    public void showGoalsView() {
        showGoals();
    }

    public void shutdown() {
        capturePoll.stop();
        actionExecutor.shutdownNow();
        if (capture != null) capture.shutdown();
        if (interruptionsController != null) interruptionsController.shutdown();
        if (usageController != null) usageController.shutdown();
        if (goalsController != null) goalsController.shutdown();
        if (settingsController != null) settingsController.shutdown();
    }

    private void updateCaptureStatus() {
        if (capture == null || captureStatus == null) return;
        EmCaptureStatus status = capture.status();
        captureStatus.setText(status.message());
        captureStatus.getStyleClass().removeAll("capture-running", "capture-error", "capture-paused");
        switch (status.state()) {
            case RUNNING -> captureStatus.getStyleClass().add("capture-running");
            case PAUSED -> captureStatus.getStyleClass().add("capture-paused");
            case ERROR -> captureStatus.getStyleClass().add("capture-error");
            default -> { }
        }
        captureButton.setText(switch (status.state()) {
            case RUNNING, PAUSED -> "Detener captura";
            case STARTING, STOPPING -> "…";
            case ERROR -> "Reintentar captura";
            case STOPPED -> "Iniciar captura";
        });
        boolean confirming = deleteOverlay != null && deleteOverlay.isVisible();
        boolean canPause = status.state() == EmCaptureState.RUNNING
                || status.state() == EmCaptureState.PAUSED;
        if (pauseButton != null) {
            pauseButton.setText(status.state() == EmCaptureState.PAUSED
                    ? "Reanudar captura" : "Pausar captura");
            pauseButton.setVisible(canPause);
            pauseButton.setManaged(canPause);
            pauseButton.setDisable(!canPause || deleting || confirming);
        }
        captureButton.setDisable(deleting || confirming
                || status.state() == EmCaptureState.STARTING
                || status.state() == EmCaptureState.STOPPING);
        if (deleteButton != null) deleteButton.setDisable(deleting || confirming);
        if (settingsController != null && capture != null)
            settingsController.updateEffective(capture.effectiveSettings());
        if (settingsController != null && capture != null)
            settingsController.updatePassiveEffective(capture.effectivePassiveMax());
        if (settingsController != null && capture != null)
            settingsController.updateDeferralEffective(capture.effectiveAlertDeferral());
    }

    private void setContent(Parent root) {
        content.getChildren().setAll(Objects.requireNonNull(root, "root"));
    }

    private void setActive(Button active) {
        for (Button button : List.of(interruptionsButton, usageButton, goalsButton, settingsButton))
            button.getStyleClass().remove("nav-active");
        active.getStyleClass().add("nav-active");
    }
}
