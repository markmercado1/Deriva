package com.deriva.ui.fx;

import com.deriva.app.usecase.EmCaptureControl;
import com.deriva.app.usecase.EmCaptureState;
import com.deriva.app.usecase.EmSettingsService;
import com.deriva.coach.model.EmDetectionSettings;
import com.deriva.ui.viewmodel.EmAlertDeferralSettingsViewModel;
import com.deriva.ui.viewmodel.EmPassiveMaxSettingsViewModel;
import com.deriva.ui.viewmodel.EmSettingsViewModel;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.Pane;
import javafx.scene.Node;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.UnaryOperator;

public final class EmSettingsController {
    @FXML private GridPane settingsMenu;
    @FXML private ScrollPane settingsScroll;
    @FXML private HBox sectionNavigation;
    @FXML private Label sectionTitle;
    @FXML private VBox detectionPanel, passivePanel, deferralPanel;
    @FXML private VBox valuesPanel, applicationPanel, detectionActions;
    private TitledPane valuesDisclosure, applicationDisclosure;
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
    @FXML private TextField passiveMaxField;
    @FXML private Label passiveMaxHint;
    @FXML private Label passiveEffectiveValues;
    @FXML private Label passivePendingValues;
    @FXML private Label passiveUnsavedLabel;
    @FXML private Label passiveValidationLabel;
    @FXML private Label passiveLoadWarningLabel;
    @FXML private Label passiveSaveStatus;
    @FXML private Button passiveSaveButton;
    @FXML private Button passiveRestoreButton;
    @FXML private TextField deferralMaxField;
    @FXML private Label deferralMaxHint;
    @FXML private Label deferralEffectiveValues;
    @FXML private Label deferralPendingValues;
    @FXML private Label deferralUnsavedLabel;
    @FXML private Label deferralValidationLabel;
    @FXML private Label deferralLoadWarningLabel;
    @FXML private Label deferralSaveStatus;
    @FXML private Button deferralSaveButton;
    @FXML private Button deferralRestoreButton;

    private final ExecutorService executor = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "deriva-ui-settings");
        thread.setDaemon(true);
        return thread;
    });

    private EmSettingsViewModel viewModel;
    private EmPassiveMaxSettingsViewModel passiveViewModel;
    private EmAlertDeferralSettingsViewModel alertDeferralViewModel;
    private EmSettingsService service;
    private EmCaptureControl capture;
    private boolean loading;
    private boolean saving;
    private boolean passiveSaving;
    private boolean deferralSaving;

    @FXML
    private void initialize() {
        String[] titles = {"Detección", "Actividad pasiva", "Avisos durante una deriva"};
        String[] descriptions = {"Cambios, estabilidad e inactividad.",
                "Tiempo sin teclado ni ratón en objetivos pasivos.",
                "Cuánto puede esperar un aviso de objetivo."};
        for (int i = 0; i < titles.length; i++) {
            final int section = i;
            Label number = new Label("0" + (i + 1) + "   →");
            number.getStyleClass().add("settings-card-marker");
            Label title = new Label(titles[i]);
            title.getStyleClass().add("settings-card-title");
            title.setWrapText(true);
            Label description = new Label(descriptions[i]);
            description.getStyleClass().add("settings-card-description");
            description.setWrapText(true);
            VBox content = new VBox(10, number, title, description);
            content.setFillWidth(true);
            Button card = new Button();
            card.setId("settingsSection" + i);
            card.setAccessibleText(titles[i] + ". " + descriptions[i]);
            card.setGraphic(content);
            card.setMaxWidth(Double.MAX_VALUE);
            card.setPrefHeight(180);
            card.setMaxHeight(180);
            card.getStyleClass().add("settings-menu-card");
            card.setOnAction(event -> openSection(section, titles[section]));
            settingsMenu.add(card, i % 2, i / 2);
        }
        foldHelp(detectionPanel);
        foldHelp(passivePanel);
        foldHelp(deferralPanel);
        for (Label status : List.of(unsavedLabel, validationLabel, loadWarningLabel)) {
            valuesPanel.getChildren().remove(status);
            detectionActions.getChildren().add(0, status);
        }
        valuesDisclosure = foldPanel(valuesPanel, "Valores y avisos");
        applicationDisclosure = foldPanel(applicationPanel, "Cómo se aplica");
        showOverview();
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
        var passiveNumeric = new TextFormatter<String>(change ->
                change.getControlNewText().matches("\\d{0,3}") ? change : null);
        passiveMaxField.setTextFormatter(passiveNumeric);
        passiveMaxField.textProperty().addListener((observable, old, value) -> {
            if (!loading) refreshPassive();
        });
        passiveSaveButton.setOnAction(event -> savePassive());
        passiveRestoreButton.setOnAction(event -> restorePassiveDefaults());
        var deferralNumeric = new TextFormatter<String>(change ->
                change.getControlNewText().matches("\\d{0,2}") ? change : null);
        deferralMaxField.setTextFormatter(deferralNumeric);
        deferralMaxField.textProperty().addListener((observable, old, value) -> {
            if (!loading) refreshDeferral();
        });
        deferralSaveButton.setOnAction(event -> saveDeferral());
        deferralRestoreButton.setOnAction(event -> restoreDeferralDefaults());
        hide(validationLabel);
        hide(pendingValues);
        hide(unsavedLabel);
        hide(idleWarningLabel);
        hide(loadWarningLabel);
        hide(passiveValidationLabel);
        hide(passivePendingValues);
        hide(passiveUnsavedLabel);
        hide(passiveLoadWarningLabel);
        hide(deferralValidationLabel);
        hide(deferralPendingValues);
        hide(deferralUnsavedLabel);
        hide(deferralLoadWarningLabel);
    }

    public void init(EmSettingsViewModel viewModel, EmPassiveMaxSettingsViewModel passiveViewModel,
                     EmAlertDeferralSettingsViewModel alertDeferralViewModel,
                     EmSettingsService service, EmCaptureControl capture) {
        this.viewModel = viewModel;
        this.passiveViewModel = passiveViewModel;
        this.alertDeferralViewModel = alertDeferralViewModel;
        this.service = service;
        this.capture = capture;
        showFields(viewModel.page());
        showPassiveFields(passiveViewModel.page());
        showDeferralFields(alertDeferralViewModel.page());
        refresh();
        refreshPassive();
        refreshDeferral();
    }

    @FXML
    public void showOverview() {
        openSection(-1, "");
    }

    private void openSection(int section, String title) {
        setShown(settingsMenu, section == -1);
        setShown(sectionNavigation, section != -1);
        sectionTitle.setText(title);
        setShown(detectionPanel, section == 0);
        setShown(passivePanel, section == 1);
        setShown(deferralPanel, section == 2);
        // Keep warnings and validation next to the detection form as well.
        setShown(valuesDisclosure, section == 0);
        setShown(applicationDisclosure, section == 0);
        setShown(detectionActions, section == 0);
        settingsScroll.setVvalue(0);
    }

    private static void setShown(Node node, boolean shown) {
        node.setVisible(shown);
        node.setManaged(shown);
    }

    private static void foldHelp(Pane parent) {
        VBox explanations = new VBox(8);
        int firstIndex = -1;
        for (Node child : List.copyOf(parent.getChildren())) {
            if (child instanceof Label label && (label.getStyleClass().contains("settings-help")
                    || label.getStyleClass().contains("settings-note"))) {
                if (firstIndex < 0) firstIndex = parent.getChildren().indexOf(child);
                parent.getChildren().remove(child);
                explanations.getChildren().add(label);
            } else if (child instanceof Pane nested) foldHelp(nested);
        }
        if (firstIndex >= 0) parent.getChildren().add(firstIndex,
                disclosure("Más información", explanations));
    }

    private static TitledPane disclosure(String title, Node content) {
        TitledPane pane = new TitledPane(title, content);
        pane.setExpanded(false);
        pane.setAnimated(false);
        pane.getStyleClass().add("goal-summary");
        return pane;
    }

    private static TitledPane foldPanel(VBox panel, String title) {
        Pane parent = (Pane) panel.getParent();
        int index = parent.getChildren().indexOf(panel);
        parent.getChildren().remove(panel);
        // The title is now supplied by the disclosure header.
        panel.getChildren().remove(0);
        TitledPane pane = disclosure(title, panel);
        parent.getChildren().add(index, pane);
        return pane;
    }

    public void updateEffective(EmDetectionSettings effective) {
        if (viewModel == null || saving) return;
        viewModel.setEffective(effective);
        apply(viewModel.page());
    }

    public void updatePassiveEffective(Duration effective) {
        if (passiveViewModel == null || passiveSaving) return;
        passiveViewModel.setEffective(effective);
        applyPassive(passiveViewModel.page());
    }

    public void updateDeferralEffective(Duration effective) {
        if (alertDeferralViewModel == null || deferralSaving) return;
        alertDeferralViewModel.setEffective(effective);
        applyDeferral(alertDeferralViewModel.page());
    }

    public void refresh() {
        if (viewModel == null || loading) return;
        viewModel.update(switchesField.getText(), windowField.getText(),
                stabilityField.getText(), idleField.getText());
        apply(viewModel.page());
    }

    private void save() {
        if (viewModel == null || service == null || saving) return;
        EmSettingsViewModel.Page page = viewModel.page();
        if (!page.canSave()) return;
        EmDetectionSettings values;
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
                    EmCaptureState state = capture == null ? EmCaptureState.STOPPED : capture.status().state();
                    boolean active = state == EmCaptureState.STARTING || state == EmCaptureState.RUNNING
                            || state == EmCaptureState.PAUSED || state == EmCaptureState.STOPPING;
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

    public void refreshPassive() {
        if (passiveViewModel == null || loading) return;
        passiveViewModel.update(passiveMaxField.getText());
        applyPassive(passiveViewModel.page());
    }

    private void savePassive() {
        if (passiveViewModel == null || service == null || passiveSaving) return;
        EmPassiveMaxSettingsViewModel.Page page = passiveViewModel.page();
        if (!page.canSave()) return;
        Duration value;
        try {
            value = passiveViewModel.validatedValue();
        } catch (IllegalStateException e) {
            showPassiveValidation(e.getMessage());
            return;
        }

        passiveSaving = true;
        passiveSaveButton.setDisable(true);
        passiveRestoreButton.setDisable(true);
        passiveSaveStatus.getStyleClass().removeAll("save-ok", "save-error");
        passiveSaveStatus.setText("Guardando…");

        CompletableFuture.supplyAsync(() -> service.savePassiveMax(value), executor)
                .whenComplete((snapshot, error) -> Platform.runLater(() -> {
                    passiveSaving = false;
                    passiveRestoreButton.setDisable(false);
                    if (error != null) {
                        passiveSaveStatus.getStyleClass().add("save-error");
                        passiveSaveStatus.setText("No se pudo guardar: " + message(error));
                        refreshPassive();
                        return;
                    }
                    passiveViewModel.applySaved(snapshot);
                    if (capture != null) passiveViewModel.setEffective(capture.effectivePassiveMax());
                    passiveSaveStatus.getStyleClass().add("save-ok");
                    EmCaptureState state = capture == null ? EmCaptureState.STOPPED : capture.status().state();
                    boolean active = state == EmCaptureState.STARTING || state == EmCaptureState.RUNNING
                            || state == EmCaptureState.PAUSED || state == EmCaptureState.STOPPING;
                    passiveSaveStatus.setText(active
                            ? "Guardado. La sesión actual conserva su máximo; se aplicará al detener e iniciar la captura."
                            : "Guardado. Se aplicará al iniciar la captura.");
                    refreshPassive();
                }));
    }

    private void restorePassiveDefaults() {
        if (passiveViewModel == null || passiveSaving) return;
        passiveViewModel.restoreDefaults();
        showPassiveFields(passiveViewModel.page());
        refreshPassive();
        passiveSaveStatus.getStyleClass().removeAll("save-ok", "save-error");
        passiveSaveStatus.setText("Predeterminados en el formulario. Pulsa Guardar para aplicarlos.");
    }

    public void refreshDeferral() {
        if (alertDeferralViewModel == null || loading) return;
        alertDeferralViewModel.update(deferralMaxField.getText());
        applyDeferral(alertDeferralViewModel.page());
    }

    private void saveDeferral() {
        if (alertDeferralViewModel == null || service == null || deferralSaving) return;
        EmAlertDeferralSettingsViewModel.Page page = alertDeferralViewModel.page();
        if (!page.canSave()) return;
        Duration value;
        try {
            value = alertDeferralViewModel.validatedValue();
        } catch (IllegalStateException e) {
            showDeferralValidation(e.getMessage());
            return;
        }

        deferralSaving = true;
        deferralSaveButton.setDisable(true);
        deferralRestoreButton.setDisable(true);
        deferralSaveStatus.getStyleClass().removeAll("save-ok", "save-error");
        deferralSaveStatus.setText("Guardando…");

        CompletableFuture.supplyAsync(() -> service.saveAlertDeferralMax(value), executor)
                .whenComplete((snapshot, error) -> Platform.runLater(() -> {
                    deferralSaving = false;
                    deferralRestoreButton.setDisable(false);
                    if (error != null) {
                        deferralSaveStatus.getStyleClass().add("save-error");
                        deferralSaveStatus.setText("No se pudo guardar: " + message(error));
                        refreshDeferral();
                        return;
                    }
                    alertDeferralViewModel.applySaved(snapshot);
                    if (capture != null)
                        alertDeferralViewModel.setEffective(capture.effectiveAlertDeferral());
                    deferralSaveStatus.getStyleClass().add("save-ok");
                    EmCaptureState state = capture == null ? EmCaptureState.STOPPED : capture.status().state();
                    boolean active = state == EmCaptureState.STARTING || state == EmCaptureState.RUNNING
                            || state == EmCaptureState.PAUSED || state == EmCaptureState.STOPPING;
                    deferralSaveStatus.setText(active
                            ? "Guardado. La sesión actual conserva su máximo; se aplicará al detener e iniciar la captura."
                            : "Guardado. Se aplicará al iniciar la captura.");
                    refreshDeferral();
                }));
    }

    private void restoreDeferralDefaults() {
        if (alertDeferralViewModel == null || deferralSaving) return;
        alertDeferralViewModel.restoreDefaults();
        showDeferralFields(alertDeferralViewModel.page());
        refreshDeferral();
        deferralSaveStatus.getStyleClass().removeAll("save-ok", "save-error");
        deferralSaveStatus.setText("Predeterminados en el formulario. Pulsa Guardar para aplicarlos.");
    }

    private void apply(EmSettingsViewModel.Page page) {
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
            idleWarningLabel.setText("Aviso: con los valores del formulario, el tiempo de inactividad "
                    + "es menor o igual que el de estabilidad. Si lees o ves vídeos sin tocar el teclado o el ratón, la "
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

    private void applyPassive(EmPassiveMaxSettingsViewModel.Page page) {
        passiveEffectiveValues.setText("En uso ahora: " + page.effectiveText());
        if (page.pending()) {
            passivePendingValues.setText("Guardado, pendiente de aplicar: " + page.savedText());
            show(passivePendingValues);
        } else {
            hide(passivePendingValues);
            if (passiveSaveStatus.getStyleClass().contains("save-ok"))
                passiveSaveStatus.setText("Guardado. El máximo guardado está en uso.");
        }
        if (page.unsavedChanges()) show(passiveUnsavedLabel);
        else hide(passiveUnsavedLabel);

        if (page.valid()) hide(passiveValidationLabel);
        else showPassiveValidation(page.validationError());

        if (page.loadWarnings().isEmpty()) {
            hide(passiveLoadWarningLabel);
        } else {
            StringBuilder text = new StringBuilder("La configuración guardada tenía valores no válidos. "
                    + "No se han sobrescrito; estos son los valores en uso:\n" + page.effectiveText());
            for (String warning : page.loadWarnings()) text.append("\n• ").append(warning);
            passiveLoadWarningLabel.setText(text.toString());
            show(passiveLoadWarningLabel);
        }

        passiveMaxHint.setText(passiveHint(passiveMaxField.getText()));
        passiveSaveButton.setDisable(passiveSaving || !page.canSave());
        passiveRestoreButton.setDisable(passiveSaving);
    }

    private void applyDeferral(EmAlertDeferralSettingsViewModel.Page page) {
        deferralEffectiveValues.setText("En uso ahora: " + page.effectiveText());
        if (page.pending()) {
            deferralPendingValues.setText("Guardado, pendiente de aplicar: " + page.savedText());
            show(deferralPendingValues);
        } else {
            hide(deferralPendingValues);
            if (deferralSaveStatus.getStyleClass().contains("save-ok"))
                deferralSaveStatus.setText("Guardado. El máximo de espera guardado está en uso.");
        }
        if (page.unsavedChanges()) show(deferralUnsavedLabel);
        else hide(deferralUnsavedLabel);

        if (page.valid()) hide(deferralValidationLabel);
        else showDeferralValidation(page.validationError());

        if (page.loadWarnings().isEmpty()) {
            hide(deferralLoadWarningLabel);
        } else {
            StringBuilder text = new StringBuilder("La configuración guardada tenía valores no válidos. "
                    + "No se han sobrescrito; estos son los valores en uso:\n" + page.effectiveText());
            for (String warning : page.loadWarnings()) text.append("\n• ").append(warning);
            deferralLoadWarningLabel.setText(text.toString());
            show(deferralLoadWarningLabel);
        }

        deferralMaxHint.setText(passiveHint(deferralMaxField.getText()));
        deferralSaveButton.setDisable(deferralSaving || !page.canSave());
        deferralRestoreButton.setDisable(deferralSaving);
    }

    private void showPassiveFields(EmPassiveMaxSettingsViewModel.Page page) {
        loading = true;
        passiveMaxField.setText(page.minutesText());
        loading = false;
    }

    private void showDeferralFields(EmAlertDeferralSettingsViewModel.Page page) {
        loading = true;
        deferralMaxField.setText(page.minutesText());
        loading = false;
    }

    private void showPassiveValidation(String message) {
        passiveValidationLabel.setText(message == null ? "Revisa el valor." : message);
        show(passiveValidationLabel);
    }

    private void showDeferralValidation(String message) {
        deferralValidationLabel.setText(message == null ? "Revisa el valor." : message);
        show(deferralValidationLabel);
    }

    private static String passiveHint(String text) {
        try {
            long minutes = Long.parseLong(text.trim());
            if (minutes < 0) return "—";
            return minutes + " min";
        } catch (NumberFormatException e) {
            return "—";
        }
    }

    private void showFields(EmSettingsViewModel.Page page) {
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
            return EmSettingsViewModel.seconds(Duration.ofSeconds(seconds));
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
