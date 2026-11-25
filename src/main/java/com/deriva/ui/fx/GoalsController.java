package com.deriva.ui.fx;

import com.deriva.goals.model.GoalAlert;
import com.deriva.ui.viewmodel.GoalContextOption;
import com.deriva.ui.viewmodel.GoalJustificationView;
import com.deriva.ui.viewmodel.GoalRow;
import com.deriva.ui.viewmodel.GoalsPage;
import com.deriva.ui.viewmodel.GoalsViewModel;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;

/** JavaFX controller for the goals screen. All database work runs off the FX thread. */
public final class GoalsController {
    @FXML private Label summaryLabel;
    @FXML private Label listStatusLabel;
    @FXML private VBox goalsList;
    @FXML private Label formTitle;
    @FXML private ComboBox<GoalContextOption> contextCombo;
    @FXML private TextField processField;
    @FXML private TextField hostField;
    @FXML private TextField minutesField;
    @FXML private TextField entriesField;
    @FXML private CheckBox passiveCheck;
    @FXML private CheckBox activeCheck;
    @FXML private Label validationLabel;
    @FXML private Label saveStatus;
    @FXML private Button saveButton;
    @FXML private Button cancelButton;
    @FXML private Button newButton;
    @FXML private StackPane deleteOverlay;
    @FXML private Label deleteWarning;
    @FXML private Button confirmDeleteButton;
    @FXML private StackPane justifyOverlay;
    @FXML private Label justifyContextLabel;
    @FXML private TextArea justifyReasonField;
    @FXML private Label justifyCounterLabel;
    @FXML private Label justifyErrorLabel;
    @FXML private Button confirmJustifyButton;

    private final ExecutorService executor = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "deriva-goals");
        thread.setDaemon(true);
        return thread;
    });
    private final AtomicLong loadRequest = new AtomicLong();
    private final Timeline autoRefresh = new Timeline(
            new KeyFrame(Duration.seconds(30), event -> refresh()));

    private GoalsViewModel viewModel;
    private GoalsPage current;
    private boolean loadingFields;
    private boolean loading;
    private boolean busy;
    private long pendingDeleteId;
    private long pendingJustifyId;

    @FXML
    private void initialize() {
        contextCombo.valueProperty().addListener((observable, old, option) -> {
            if (!loadingFields && option != null) show(viewModel.selectContext(option));
        });
        var numeric = new TextFormatter<String>(change ->
                change.getControlNewText().matches("\\d{0,5}") ? change : null);
        minutesField.setTextFormatter(numeric);
        entriesField.setTextFormatter(new TextFormatter<String>(change ->
                change.getControlNewText().matches("\\d{0,5}") ? change : null));
        for (TextField field : List.of(processField, hostField, minutesField, entriesField))
            field.textProperty().addListener((observable, old, value) -> formChanged());
        passiveCheck.selectedProperty().addListener((observable, old, value) -> formChanged());
        activeCheck.selectedProperty().addListener((observable, old, value) -> formChanged());
        saveButton.setOnAction(event -> save());
        cancelButton.setOnAction(event -> show(viewModel.cancelEdit()));
        newButton.setOnAction(event -> show(viewModel.cancelEdit()));
        justifyReasonField.setTextFormatter(new TextFormatter<String>(change ->
                change.getControlNewText().length() <= GoalAlert.MAX_JUSTIFICATION_LENGTH
                        ? change : null));
        justifyReasonField.textProperty().addListener((observable, old, value) -> {
            justifyCounterLabel.setText(value.length() + "/" + GoalAlert.MAX_JUSTIFICATION_LENGTH);
            hide(justifyErrorLabel);
        });
        autoRefresh.setCycleCount(Animation.INDEFINITE);
        hide(validationLabel);
        hide(justifyErrorLabel);
    }

    public void init(GoalsViewModel viewModel) {
        this.viewModel = viewModel;
    }

    public void refresh() {
        if (viewModel == null) return;
        long request = loadRequest.incrementAndGet();
        loading = true;
        updateButtons();
        CompletableFuture.supplyAsync(viewModel::load, executor)
                .whenComplete((page, error) -> Platform.runLater(() -> {
                    if (request != loadRequest.get()) return;
                    loading = false;
                    if (error != null) {
                        showLoadError(error);
                        updateButtons();
                    } else {
                        clearStatus();
                        apply(page);
                    }
                }));
    }

    public void startAutoRefresh() {
        autoRefresh.playFromStart();
    }

    public void stopAutoRefresh() {
        autoRefresh.stop();
    }

    public void shutdown() {
        autoRefresh.stop();
        executor.shutdownNow();
    }

    private void formChanged() {
        if (viewModel == null || loadingFields) return;
        applyForm(viewModel.updateForm(processField.getText(), hostField.getText(),
                minutesField.getText(), entriesField.getText(),
                passiveCheck.isSelected(), activeCheck.isSelected()));
    }

    private void save() {
        if (viewModel == null || busy || loading || current == null || !current.canSave()) return;
        busy = true;
        updateButtons();
        setStatus("Guardando…", false);
        runOutcome(viewModel::save, true);
    }

    private void edit(long id) {
        if (busy || loading) return;
        show(viewModel.beginEdit(id));
    }

    private void toggleActive(long id) {
        if (busy) return;
        busy = true;
        updateButtons();
        runOutcome(() -> viewModel.toggleActive(id), false);
    }

    private void requestDelete(long id, String context) {
        if (busy) return;
        pendingDeleteId = id;
        deleteWarning.setText("Se borrará «" + context + "» y sus avisos. Los segmentos de actividad "
                + "y el historial de uso no se tocan. Si solo quieres dejar de vigilarlo, desactívalo.");
        deleteOverlay.setManaged(true);
        deleteOverlay.setVisible(true);
        deleteOverlay.toFront();
    }

    @FXML
    private void cancelDelete() {
        if (busy) return;
        pendingDeleteId = 0;
        hideOverlay();
    }

    @FXML
    private void confirmDelete() {
        if (busy || pendingDeleteId <= 0) return;
        long id = pendingDeleteId;
        pendingDeleteId = 0;
        hideOverlay();
        busy = true;
        updateButtons();
        runOutcome(() -> viewModel.delete(id), false);
    }

    private void requestJustify(long id, String context, String existingReason) {
        if (busy || viewModel == null) return;
        pendingJustifyId = id;
        justifyContextLabel.setText("Objetivo: " + context);
        justifyReasonField.setText(existingReason == null ? "" : existingReason);
        justifyCounterLabel.setText(justifyReasonField.getText().length() + "/"
                + GoalAlert.MAX_JUSTIFICATION_LENGTH);
        hide(justifyErrorLabel);
        justifyOverlay.setManaged(true);
        justifyOverlay.setVisible(true);
        justifyOverlay.toFront();
        justifyReasonField.requestFocus();
    }

    @FXML
    private void cancelJustify() {
        if (busy) return;
        pendingJustifyId = 0;
        hideJustifyOverlay();
    }

    @FXML
    private void confirmJustify() {
        if (busy || viewModel == null || pendingJustifyId <= 0) return;
        String reason = justifyReasonField.getText();
        if (reason != null && reason.length() > GoalAlert.MAX_JUSTIFICATION_LENGTH) {
            justifyErrorLabel.setText("El motivo no puede superar los 500 caracteres.");
            show(justifyErrorLabel);
            return;
        }
        long id = pendingJustifyId;
        pendingJustifyId = 0;
        hideJustifyOverlay();
        busy = true;
        updateButtons();
        setStatus("Guardando justificación…", false);
        runOutcome(() -> viewModel.justify(id, reason), false);
    }

    private void removeJustification(long id) {
        if (busy || viewModel == null) return;
        busy = true;
        updateButtons();
        setStatus("Quitando justificación…", false);
        runOutcome(() -> viewModel.removeJustification(id), false);
    }

    private void runOutcome(Supplier<GoalsViewModel.Outcome> action, boolean formAction) {
        CompletableFuture.supplyAsync(action, executor)
                .whenComplete((outcome, error) -> Platform.runLater(() -> {
                    busy = false;
                    if (error != null) {
                        setStatus("No se pudo completar la operación. " + message(error), true);
                        updateButtons();
                        return;
                    }
                    apply(outcome.page());
                    setStatus(outcome.message(), outcome.error());
                }));
    }

    private void apply(GoalsPage page) {
        current = page;
        summaryLabel.setText(page.summary());
        listStatusLabel.setText("");
        listStatusLabel.getStyleClass().remove("range-error");
        render(page.goals());
        applyForm(page);
    }

    private void applyForm(GoalsPage page) {
        current = page;
        loadingFields = true;
        formTitle.setText(page.formTitle());
        processField.setText(page.processText());
        hostField.setText(page.hostText());
        minutesField.setText(page.minutesText());
        entriesField.setText(page.entriesText());
        passiveCheck.setSelected(page.passive());
        activeCheck.setSelected(page.active());
        contextCombo.getItems().setAll(page.contexts());
        contextCombo.setValue(contextOption(page));
        loadingFields = false;

        processField.setDisable(page.contextLocked());
        hostField.setDisable(page.contextLocked());
        contextCombo.setDisable(page.contextLocked());
        saveButton.setText(page.editing() ? "Guardar cambios" : "Crear objetivo");
        cancelButton.setVisible(page.editing());
        cancelButton.setManaged(page.editing());
        newButton.setVisible(!page.editing());
        newButton.setManaged(!page.editing());
        if (page.validationError() == null) {
            hide(validationLabel);
        } else {
            validationLabel.setText(page.validationError());
            show(validationLabel);
        }
        updateButtons();
    }

    private static GoalContextOption contextOption(GoalsPage page) {
        if (page.processText().isBlank()) return null;
        String process = page.processText().trim().toLowerCase(java.util.Locale.ROOT);
        String host = page.hostText().isBlank() ? null
                : page.hostText().trim().toLowerCase(java.util.Locale.ROOT);
        if (host != null && host.startsWith("www.")) host = host.substring(4);
        for (GoalContextOption option : page.contexts()) {
            if (option.context().processName().equals(process)
                    && java.util.Objects.equals(option.context().siteHost(), host))
                return option;
        }
        return null;
    }

    private void render(List<GoalRow> rows) {
        goalsList.getChildren().clear();
        if (rows.isEmpty()) {
            Label empty = new Label("Todavía no hay objetivos. Crea uno con el formulario de la derecha.");
            empty.getStyleClass().add("usage-empty");
            empty.setWrapText(true);
            goalsList.getChildren().add(empty);
            return;
        }
        for (GoalRow row : rows) goalsList.getChildren().add(renderRow(row));
    }

    private VBox renderRow(GoalRow row) {
        VBox card = new VBox(7);
        card.getStyleClass().add("goal-row");
        if (!row.active()) card.getStyleClass().add("goal-row-inactive");

        Label context = new Label(row.contextLabel());
        context.getStyleClass().add("goal-context");

        Label limits = new Label(row.limitsText());
        limits.getStyleClass().add("goal-limits");

        Label state = new Label(row.stateIcon() + "  " + row.stateLabel());
        state.getStyleClass().addAll("goal-state", "goal-state-" + row.stateKey());

        HBox heading = new HBox(10, context, new Region(), state);
        HBox.setHgrow(heading.getChildren().get(1), Priority.ALWAYS);
        heading.setAlignment(Pos.CENTER_LEFT);

        ProgressBar bar = new ProgressBar(row.ratio());
        bar.getStyleClass().add("goal-bar");
        bar.setMaxWidth(Double.MAX_VALUE);

        Label progress = new Label(row.progressText());
        progress.getStyleClass().add("goal-progress");

        HBox details = new HBox(12, limits, new Region(), progress, bar);
        details.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(details.getChildren().get(1), Priority.ALWAYS);
        HBox.setHgrow(bar, Priority.ALWAYS);
        bar.setMaxWidth(190);

        card.getChildren().addAll(heading, details);
        GoalJustificationView justification = row.justification();
        if (justification != null && justification.reason() != null) {
            Label reason = new Label("Motivo: " + justification.reason());
            reason.getStyleClass().add("goal-justification");
            reason.setWrapText(true);
            card.getChildren().add(reason);
        }
        if (justification != null && justification.coverage() != null) {
            Label coverage = new Label(justification.coverage());
            coverage.getStyleClass().add("goal-justification-coverage");
            coverage.setWrapText(true);
            card.getChildren().add(coverage);
        }
        if (justification != null && justification.note() != null) {
            Label note = new Label(justification.note());
            note.getStyleClass().add("goal-justification-note");
            note.setWrapText(true);
            card.getChildren().add(note);
        }
        if (row.alertOutcomeText() != null) {
            Label outcome = new Label(row.alertOutcomeText());
            outcome.getStyleClass().add("goal-alert-outcome");
            outcome.setWrapText(true);
            card.getChildren().add(outcome);
        }

        HBox actions = new HBox(8);
        actions.setAlignment(Pos.CENTER_RIGHT);
        if (justification != null && justification.canJustify()) {
            Button justify = new Button(justification.actionLabel());
            justify.getStyleClass().addAll("goal-action-button", "goal-justify-button");
            justify.setOnAction(event ->
                    requestJustify(row.id(), row.contextLabel(), justification.reason()));
            actions.getChildren().add(justify);
        }
        if (justification != null && justification.canRemove()) {
            Button remove = new Button("Quitar justificación");
            remove.getStyleClass().addAll("goal-action-button", "goal-justify-button");
            remove.setOnAction(event -> removeJustification(row.id()));
            actions.getChildren().add(remove);
        }
        Button edit = new Button("Editar");
        edit.getStyleClass().addAll("goal-action-button", "goal-edit-button");
        edit.setOnAction(event -> edit(row.id()));
        Button toggle = new Button(row.active() ? "Desactivar" : "Activar");
        toggle.getStyleClass().addAll("goal-action-button", "goal-toggle-button");
        toggle.setOnAction(event -> toggleActive(row.id()));
        Button delete = new Button("Borrar");
        delete.getStyleClass().addAll("goal-action-button", "goal-delete-button");
        delete.setOnAction(event -> requestDelete(row.id(), row.contextLabel()));
        actions.getChildren().addAll(edit, toggle, delete);

        card.getChildren().add(actions);
        if (row.unmeasurable()) {
            Label note = new Label("Puede haber subconteo: parte del tiempo excluido ante duda no se "
                    + "puede atribuir a este contexto.");
            note.getStyleClass().add("goal-warning");
            note.setWrapText(true);
            card.getChildren().add(note);
        }
        return card;
    }

    private void showLoadError(Throwable error) {
        current = null;
        listStatusLabel.setText("No se pudieron cargar los objetivos. " + message(error));
        listStatusLabel.getStyleClass().add("range-error");
        goalsList.getChildren().clear();
        renderMessage("No disponible por error de carga.");
    }

    private void renderMessage(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("usage-empty");
        label.setWrapText(true);
        goalsList.getChildren().add(label);
    }

    private void updateButtons() {
        if (saveButton == null) return;
        boolean editing = current != null && current.editing();
        saveButton.setDisable(busy || loading
                || current == null || !current.canSave());
        saveButton.setText(editing ? "Guardar cambios" : "Crear objetivo");
        cancelButton.setDisable(busy || loading);
        newButton.setDisable(busy || loading);
        confirmDeleteButton.setDisable(busy);
        if (confirmJustifyButton != null) confirmJustifyButton.setDisable(busy);
    }

    private void setStatus(String text, boolean error) {
        saveStatus.setText(text == null ? "" : text);
        saveStatus.getStyleClass().removeAll("save-ok", "save-error");
        if (text != null && !text.isBlank())
            saveStatus.getStyleClass().add(error ? "save-error" : "save-ok");
    }

    private void clearStatus() {
        saveStatus.setText("");
        saveStatus.getStyleClass().removeAll("save-ok", "save-error");
    }

    private void show(GoalsPage page) {
        apply(page);
    }

    private void hideOverlay() {
        deleteOverlay.setManaged(false);
        deleteOverlay.setVisible(false);
    }

    private void hideJustifyOverlay() {
        justifyOverlay.setManaged(false);
        justifyOverlay.setVisible(false);
    }

    private static void show(Label label) {
        label.setManaged(true);
        label.setVisible(true);
    }

    private static void hide(Label label) {
        label.setManaged(false);
        label.setVisible(false);
    }

    private static String message(Throwable error) {
        Throwable cause = error;
        while ((cause instanceof java.util.concurrent.CompletionException
                || cause instanceof ExecutionException) && cause.getCause() != null) {
            cause = cause.getCause();
        }
        return cause.getMessage() == null ? cause.getClass().getSimpleName() : cause.getMessage();
    }
}
