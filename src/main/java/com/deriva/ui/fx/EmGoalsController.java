package com.deriva.ui.fx;

import com.deriva.goals.model.EmGoalAlert;
import com.deriva.ui.viewmodel.EmGoalContextOption;
import com.deriva.ui.viewmodel.EmGoalJustificationView;
import com.deriva.ui.viewmodel.EmGoalRow;
import com.deriva.ui.viewmodel.EmGoalsPage;
import com.deriva.ui.viewmodel.EmGoalsViewModel;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.time.LocalDate;
import java.util.List;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;

/** JavaFX controller for the goals screen. All database work runs off the FX thread. */
public final class EmGoalsController {
    @FXML private Label summaryLabel;
    @FXML private Label listTitle;
    @FXML private DatePicker datePicker;
    @FXML private DatePicker toPicker;
    @FXML private Button todayButton;
    @FXML private CheckBox showArchivedCheck;
    @FXML private Label historyNoteLabel;
    @FXML private Label listStatusLabel;
    @FXML private VBox goalsList;
    @FXML private TitledPane revisionPane;
    @FXML private VBox revisionList;
    @FXML private Label formTitle;
    @FXML private Label historyFormNote;
    @FXML private ComboBox<EmGoalContextOption> contextCombo;
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

    private EmGoalsViewModel viewModel;
    private EmGoalsPage current;
    private final Set<Long> expandedSummaries = new HashSet<>();
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
        datePicker.valueProperty().addListener((observable, old, day) -> rangeChanged());
        toPicker.valueProperty().addListener((observable, old, day) -> rangeChanged());
        todayButton.setOnAction(event -> {
            if (viewModel == null) return;
            LocalDate today = viewModel.today();
            loadingFields = true;
            datePicker.setValue(today);
            toPicker.setValue(today);
            loadingFields = false;
            rangeChanged();
        });
        showArchivedCheck.selectedProperty().addListener((observable, old, value) -> {
            if (!loadingFields && viewModel != null) apply(viewModel.setShowArchived(value));
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
                change.getControlNewText().length() <= EmGoalAlert.MAX_JUSTIFICATION_LENGTH
                        ? change : null));
        justifyReasonField.textProperty().addListener((observable, old, value) -> {
            justifyCounterLabel.setText(value.length() + "/" + EmGoalAlert.MAX_JUSTIFICATION_LENGTH);
            hide(justifyErrorLabel);
        });
        autoRefresh.setCycleCount(Animation.INDEFINITE);
        hide(validationLabel);
        hide(justifyErrorLabel);
    }

    public void init(EmGoalsViewModel viewModel) {
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

    private void rangeChanged() {
        if (loadingFields || viewModel == null) return;
        LocalDate from = datePicker.getValue();
        LocalDate to = toPicker.getValue();
        if (from == null || to == null) return;
        String error = viewModel.selectRange(from, to);
        if (error != null) {
            listStatusLabel.setText(error);
            if (!listStatusLabel.getStyleClass().contains("range-error"))
                listStatusLabel.getStyleClass().add("range-error");
            return;
        }
        refresh();
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
        deleteWarning.setText("Se retirará «" + context + "» de la captura. Su configuración y sus avisos "
                + "se conservarán para consultar los días en que existió; los segmentos de actividad "
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
                + EmGoalAlert.MAX_JUSTIFICATION_LENGTH);
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
        if (reason != null && reason.length() > EmGoalAlert.MAX_JUSTIFICATION_LENGTH) {
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

    private void runOutcome(Supplier<EmGoalsViewModel.Outcome> action, boolean formAction) {
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

    private void apply(EmGoalsPage page) {
        current = page;
        summaryLabel.setText(page.summary());
        listTitle.setText(page.listTitle());
        listStatusLabel.setText("");
        listStatusLabel.getStyleClass().remove("range-error");
        render(page.goals(), page.historical(), page.summaryTitle());
        applyForm(page);
    }

    private void applyForm(EmGoalsPage page) {
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
        if (datePicker.getValue() == null || !datePicker.getValue().equals(page.selectedDate()))
            datePicker.setValue(page.selectedDate());
        if (toPicker.getValue() == null || !toPicker.getValue().equals(page.rangeTo()))
            toPicker.setValue(page.rangeTo());
        showArchivedCheck.setSelected(page.showArchived());
        loadingFields = false;

        boolean readOnly = page.historical();
        historyNoteLabel.setText(page.historyNote() == null ? "" : page.historyNote());
        setVisible(historyNoteLabel, readOnly);
        setVisible(historyFormNote, readOnly);
        renderRevisions(page);
        processField.setDisable(readOnly || page.contextLocked());
        hostField.setDisable(readOnly || page.contextLocked());
        contextCombo.setDisable(readOnly || page.contextLocked());
        minutesField.setDisable(readOnly);
        entriesField.setDisable(readOnly);
        passiveCheck.setDisable(readOnly);
        activeCheck.setDisable(readOnly);
        saveButton.setText(page.editing() ? "Guardar cambios" : "Crear objetivo");
        setVisible(saveButton, !readOnly);
        setVisible(cancelButton, !readOnly && page.editing());
        setVisible(newButton, !readOnly && !page.editing());
        if (page.validationError() == null) {
            hide(validationLabel);
        } else {
            validationLabel.setText(page.validationError());
            show(validationLabel);
        }
        updateButtons();
    }

    private void renderRevisions(EmGoalsPage page) {
        revisionList.getChildren().clear();
        setVisible(revisionPane, page.historical());
        if (!page.historical()) return;
        for (String line : page.revisionLines()) {
            Label label = new Label(line);
            label.getStyleClass().add("data-note");
            label.setWrapText(true);
            revisionList.getChildren().add(label);
        }
    }

    private static EmGoalContextOption contextOption(EmGoalsPage page) {
        if (page.processText().isBlank()) return null;
        String process = page.processText().trim().toLowerCase(java.util.Locale.ROOT);
        String host = page.hostText().isBlank() ? null
                : page.hostText().trim().toLowerCase(java.util.Locale.ROOT);
        if (host != null && host.startsWith("www.")) host = host.substring(4);
        for (EmGoalContextOption option : page.contexts()) {
            if (option.context().processName().equals(process)
                    && java.util.Objects.equals(option.context().siteHost(), host))
                return option;
        }
        return null;
    }

    private void render(List<EmGoalRow> rows, boolean historical, String summaryTitle) {
        expandedSummaries.retainAll(rows.stream().map(EmGoalRow::id).toList());
        goalsList.getChildren().clear();
        if (rows.isEmpty()) {
            Label empty = new Label(historical
                    ? EmGoalsViewModel.HISTORY_EMPTY_TEXT
                    : "Todavía no hay objetivos. Crea uno con el formulario de la derecha.");
            empty.getStyleClass().add("usage-empty");
            empty.setWrapText(true);
            goalsList.getChildren().add(empty);
            return;
        }
        for (EmGoalRow row : rows)
            goalsList.getChildren().add(renderRow(row, historical || row.archived(), summaryTitle));
    }

    private VBox renderRow(EmGoalRow row, boolean readOnly, String summaryTitle) {
        VBox card = new VBox(7);
        card.getStyleClass().add("goal-row");
        if (!row.active() || row.archived()) card.getStyleClass().add("goal-row-inactive");

        Label context = new Label(row.contextLabel());
        context.getStyleClass().add("goal-context");

        Label limits = new Label(row.limitsText());
        limits.getStyleClass().add("goal-limits");

        Label state = new Label(row.stateIcon() + "  " + row.stateLabel());
        state.getStyleClass().addAll("goal-state", "goal-state-" + row.stateKey());

        HBox heading = new HBox(10, context, new Region(), state);
        if (row.archived()) {
            Label archived = new Label("Borrado · solo lectura");
            archived.getStyleClass().addAll("goal-state", "goal-state-inactive");
            heading.getChildren().add(2, archived);
        }
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
        if (row.configurationNote() != null) {
            Label configuration = new Label(row.configurationNote());
            configuration.getStyleClass().add("goal-limits");
            configuration.setWrapText(true);
            card.getChildren().add(configuration);
        }
        VBox summaryContent = new VBox(8);
        if (row.balanceText() != null) {
            Label balance = new Label(row.balanceText());
            balance.getStyleClass().add("goal-balance");
            balance.setWrapText(true);
            summaryContent.getChildren().add(balance);
        }
        for (String line : row.alertLines()) {
            Label alert = new Label(line);
            alert.getStyleClass().add("goal-alert-line");
            alert.setWrapText(true);
            summaryContent.getChildren().add(alert);
        }
        if (!summaryContent.getChildren().isEmpty()) {
            TitledPane summary = new TitledPane(summaryTitle, summaryContent);
            summary.getStyleClass().add("goal-summary");
            summary.setAnimated(false);
            summary.setExpanded(expandedSummaries.contains(row.id()));
            summary.expandedProperty().addListener((observable, old, expanded) -> {
                if (expanded) expandedSummaries.add(row.id());
                else expandedSummaries.remove(row.id());
            });
            card.getChildren().add(summary);
        }
        EmGoalJustificationView justification = row.justification();
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
        if (readOnly) return card;
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
        boolean readOnly = current != null && current.historical();
        saveButton.setDisable(busy || loading || readOnly
                || current == null || !current.canSave());
        saveButton.setText(editing ? "Guardar cambios" : "Crear objetivo");
        cancelButton.setDisable(busy || loading || readOnly);
        newButton.setDisable(busy || loading || readOnly);
        confirmDeleteButton.setDisable(busy || readOnly);
        if (confirmJustifyButton != null) confirmJustifyButton.setDisable(busy || readOnly);
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

    private void show(EmGoalsPage page) {
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

    private static void setVisible(javafx.scene.Node node, boolean visible) {
        node.setManaged(visible);
        node.setVisible(visible);
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
