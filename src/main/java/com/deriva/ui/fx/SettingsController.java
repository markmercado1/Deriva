package com.deriva.ui.fx;

import com.deriva.app.usecase.CaptureControl;
import com.deriva.app.usecase.CaptureState;
import com.deriva.app.usecase.SettingsService;
import com.deriva.coach.model.DetectionSettings;
import com.deriva.ui.viewmodel.SettingsViewModel;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.UnaryOperator;

public final class SettingsController {
    @FXML private TextField switchesField;
    @FXML private TextField windowField;
    @FXML private TextField stabilityField;
    @FXML private TextField idleField;
    @FXML private Label windowHint;
    @FXML private Label stabilityHint;
    @FXML private Label idleHint;
    @FXML private Label effectiveValues;
    @FXML private Label pendingValues;
    @FXML private Label unsavedLabel;
    @FXML private Label validationLabel;
    @FXML private Label idleWarningLabel;
    @FXML private Label loadWarningLabel;
    @FXML private Label saveStatus;
    @FXML private Button saveButton;
    @FXML private Button restoreButton;

    private final ExecutorService executor = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "deriva-ui-settings");
        thread.setDaemon(true);
        return thread;
    });

    private SettingsViewModel viewModel;
    private SettingsService service;
    private CaptureControl capture;
    private boolean loading;
    private boolean saving;

    @FXML
    private void initialize() {
        UnaryOperator<TextFormatter.Change> numeric = change ->
                change.getControlNewText().matches("\\d{0,4}") ? change : null;
        for (TextField field : List.of(switchesField, windowField, stabilityField, idleField)) {
            field.setTextFormatter(new TextFormatter<>(numeric));
            field.textProperty().addListener((observable, old, value) -> {
                if (!loading) refresh();
            });
        }
        saveButton.setOnAction(event -> save());
        restoreButton.setOnAction(event -> restoreDefaults());
        hide(validationLabel);
        hide(pendingValues);
        hide(unsavedLabel);
        hide(idleWarningLabel);
        hide(loadWarningLabel);
    }

    public void init(SettingsViewModel viewModel, SettingsService service, CaptureControl capture) {
        this.viewModel = viewModel;
        this.service = service;
        this.capture = capture;
        showFields(viewModel.page());
        refresh();
    }

    public void updateEffective(DetectionSettings effective) {
        if (viewModel == null || saving) return;
        viewModel.setEffective(effective);
        apply(viewModel.page());
    }

    public void refresh() {
        if (viewModel == null || loading) return;
        viewModel.update(switchesField.getText(), windowField.getText(),
                stabilityField.getText(), idleField.getText());
        apply(viewModel.page());
    }

    private void save() {
        if (viewModel == null || service == null || saving) return;
        SettingsViewModel.Page page = viewModel.page();
        if (!page.canSave()) return;
        DetectionSettings values;
        try {
            values = viewModel.validatedValues();
        } catch (IllegalStateException e) {
            showValidation(e.getMessage());
            return;
        }

        saving = true;
        saveButton.setDisable(true);
        restoreButton.setDisable(true);
        saveStatus.getStyleClass().removeAll("save-ok", "save-error");
        saveStatus.setText("Guardando…");

        CompletableFuture.supplyAsync(() -> service.save(values), executor)
                .whenComplete((snapshot, error) -> Platform.runLater(() -> {
                    saving = false;
                    restoreButton.setDisable(false);
                    if (error != null) {
                        saveStatus.getStyleClass().add("save-error");
                        saveStatus.setText("No se pudo guardar: " + message(error));
                        refresh();
                        return;
                    }
                    viewModel.applySaved(snapshot);
                    if (capture != null) viewModel.setEffective(capture.effectiveSettings());
                    saveStatus.getStyleClass().add("save-ok");
                    CaptureState state = capture == null ? CaptureState.STOPPED : capture.status().state();
                    boolean active = state == CaptureState.STARTING || state == CaptureState.RUNNING
                            || state == CaptureState.PAUSED || state == CaptureState.STOPPING;
                    saveStatus.setText(active
                            ? "Guardado. La sesión actual conserva sus valores; se aplicará al detener e iniciar la captura."
                            : "Guardado. Se aplicará al iniciar la captura.");
                    refresh();
                }));
    }

    private void restoreDefaults() {
        if (viewModel == null || saving) return;
        viewModel.restoreDefaults();
        showFields(viewModel.page());
        refresh();
        saveStatus.getStyleClass().removeAll("save-ok", "save-error");
        saveStatus.setText("Predeterminados en el formulario. Pulsa Guardar para aplicarlos.");
    }

    private void apply(SettingsViewModel.Page page) {
        effectiveValues.setText("En uso ahora: " + page.effectiveSummary());
        if (page.pending()) {
            pendingValues.setText("Guardado, pendiente de aplicar: " + page.savedSummary());
            show(pendingValues);
        } else {
            hide(pendingValues);
            if (saveStatus.getStyleClass().contains("save-ok"))
                saveStatus.setText("Guardado. Los valores guardados están en uso.");
        }
        if (page.unsavedChanges()) show(unsavedLabel);
        else hide(unsavedLabel);

        if (page.valid()) {
            hide(validationLabel);
        } else {
            showValidation(page.validationError());
        }

        if (page.idleWarning()) {
            idleWarningLabel.setText("Aviso: el tiempo de inactividad es menor o igual que el de "
                    + "estabilidad. Si lees o ves vídeos sin tocar el teclado o el ratón, la "
                    + "inactividad puede cerrar el episodio antes de comprobar la estabilidad y "
                    + "quedará como No comprobada. Si lo aumentas por encima de la estabilidad, "
                    + "Deriva puede cerrar como Estable mientras no usabas el equipo y Uso diario "
                    + "puede contar ese tiempo como primer plano.");
            show(idleWarningLabel);
        } else {
            hide(idleWarningLabel);
        }

        if (page.loadWarnings().isEmpty()) {
            hide(loadWarningLabel);
        } else {
            StringBuilder text = new StringBuilder("La configuración guardada tenía valores no válidos. "
                    + "No se han sobrescrito; estos son los valores en uso:\n"
                    + page.effectiveSummary());
            for (String warning : page.loadWarnings()) text.append("\n• ").append(warning);
            loadWarningLabel.setText(text.toString());
            show(loadWarningLabel);
        }

        saveButton.setDisable(saving || !page.canSave());
        restoreButton.setDisable(saving);
        updateHints();
    }

    private void showFields(SettingsViewModel.Page page) {
        loading = true;
        switchesField.setText(page.switchesText());
        windowField.setText(page.windowText());
        stabilityField.setText(page.stabilityText());
        idleField.setText(page.idleText());
        loading = false;
    }

    private void updateHints() {
        windowHint.setText(hint(windowField.getText()));
        stabilityHint.setText(hint(stabilityField.getText()));
        idleHint.setText(hint(idleField.getText()));
    }

    private static String hint(String text) {
        try {
            int seconds = Integer.parseInt(text.trim());
            if (seconds < 0) return "—";
            return SettingsViewModel.seconds(Duration.ofSeconds(seconds));
        } catch (NumberFormatException e) {
            return "—";
        }
    }

    private void showValidation(String message) {
        validationLabel.setText(message == null ? "Revisa los valores." : message);
        show(validationLabel);
    }

    private static String message(Throwable error) {
        Throwable cause = error;
        while (cause.getCause() != null) cause = cause.getCause();
        return cause.getMessage() == null ? cause.getClass().getSimpleName() : cause.getMessage();
    }

    private static void show(Label label) {
        label.setManaged(true);
        label.setVisible(true);
    }

    private static void hide(Label label) {
        label.setManaged(false);
        label.setVisible(false);
    }

    public void shutdown() {
        executor.shutdownNow();
    }
}
