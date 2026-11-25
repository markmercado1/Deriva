package com.deriva.ui.fx;

import com.deriva.ui.viewmodel.EmInterruptionRow;
import com.deriva.ui.viewmodel.EmInterruptionsPage;
import com.deriva.ui.viewmodel.EmInterruptionsViewModel;
import com.deriva.ui.viewmodel.EmReasonOption;
import com.deriva.ui.viewmodel.EmStatusFilter;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.shape.Circle;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;

public final class EmInterruptionsController {
    private static final DateTimeFormatter SAVED_TIME = DateTimeFormatter.ofPattern("HH:mm");

    @FXML private DatePicker fromPicker;
    @FXML private DatePicker toPicker;
    @FXML private ComboBox<EmStatusFilter> statusFilter;
    @FXML private CheckBox includeJustified;
    @FXML private Label rangeNote;
    @FXML private Label dataNote;
    @FXML private Label totalLabel;
    @FXML private Label openLabel;
    @FXML private TableView<EmInterruptionRow> table;
    @FXML private TableColumn<EmInterruptionRow, String> startColumn;
    @FXML private TableColumn<EmInterruptionRow, String> statusColumn;
    @FXML private TableColumn<EmInterruptionRow, String> annotationColumn;
    @FXML private TableColumn<EmInterruptionRow, String> firstChangeColumn;
    @FXML private TableColumn<EmInterruptionRow, String> switchColumn;
    @FXML private TableColumn<EmInterruptionRow, String> stabilityColumn;
    @FXML private Label detailTitle;
    @FXML private Label detailTime;
    @FXML private Label detailFirstChange;
    @FXML private Label detailState;
    @FXML private Label detailStability;
    @FXML private Label detailCloseReason;
    @FXML private Label detailAnnotation;
    @FXML private FlowPane contextsPane;
    @FXML private ComboBox<EmReasonOption> reasonCombo;
    @FXML private CheckBox justifiedCheck;
    @FXML private Button demoButton;
    @FXML private Button saveButton;
    @FXML private Label saveStatus;

    private final ExecutorService executor = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "deriva-ui-query");
        thread.setDaemon(true);
        return thread;
    });
    private final AtomicLong refreshRequest = new AtomicLong();
    private EmInterruptionsViewModel viewModel;
    private String savedStatusText;
    private boolean appDemo;
    private boolean demoMode;
    private boolean saving;

    @FXML
    private void initialize() {
        LocalDate today = LocalDate.now();
        fromPicker.setValue(today);
        toPicker.setValue(today);
        fromPicker.setEditable(false);
        toPicker.setEditable(false);
        statusFilter.getItems().setAll(EmStatusFilter.values());
        statusFilter.setValue(EmStatusFilter.ALL);
        includeJustified.setSelected(true);
        reasonCombo.setPromptText("Selecciona un motivo");

        table.setFixedCellSize(46);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        table.setPlaceholder(new Label("Sin datos registrados para este rango de fechas."));
        startColumn.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().startText()));
        statusColumn.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().statusText()));
        annotationColumn.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().annotationText()));
        firstChangeColumn.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().firstChange()));
        switchColumn.setCellValueFactory(data -> new ReadOnlyStringWrapper(String.valueOf(data.getValue().switchCount())));
        stabilityColumn.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().stabilityText()));
        startColumn.setCellFactory(column -> monoCell());
        switchColumn.setCellFactory(column -> monoCell());
        stabilityColumn.setCellFactory(column -> monoCell());
        statusColumn.setCellFactory(column -> statusCell());
        annotationColumn.setCellFactory(column -> annotationCell());

        table.getSelectionModel().selectedItemProperty().addListener((observable, old, row) -> {
            updateDetail(row);
            if (old == null || row == null || old.id() != row.id()) resetSaveFeedback();
        });
        fromPicker.valueProperty().addListener((observable, old, value) -> refreshAsync());
        toPicker.valueProperty().addListener((observable, old, value) -> refreshAsync());
        statusFilter.valueProperty().addListener((observable, old, value) -> refreshAsync());
        includeJustified.selectedProperty().addListener((observable, old, value) -> refreshAsync());
        reasonCombo.valueProperty().addListener((observable, old, value) -> {
            updateSaveEnabled();
            clearSavedNote();
        });
        justifiedCheck.selectedProperty().addListener((observable, old, value) -> {
            updateSaveEnabled();
            clearSavedNote();
        });
        demoButton.setOnAction(event -> toggleDemoMode());
        saveButton.setOnAction(event -> save());
    }

    public void init(EmInterruptionsViewModel viewModel, boolean demo) {
        this.viewModel = viewModel;
        this.appDemo = demo;
        reasonCombo.getItems().setAll(viewModel.reasons());
        updateDataNote();
        refreshAsync();
    }

    public void refresh() {
        refreshAsync();
    }

    public void shutdown() {
        executor.shutdownNow();
    }

    private void toggleDemoMode() {
        demoMode = !demoMode;
        savedStatusText = null;
        saveStatus.getStyleClass().removeAll("save-ok", "save-error");
        saveButton.setText("Guardar");
        demoButton.setText(demoMode ? "Volver a datos reales" : "Ver datos de ejemplo");
        reasonCombo.setDisable(demoMode);
        justifiedCheck.setDisable(demoMode);
        saveStatus.setText(demoMode ? "Modo prueba: los cambios no se guardan." : "");
        table.getSelectionModel().clearSelection();
        reasonCombo.setValue(null);
        updateDataNote();
        updateSaveEnabled();
        refreshAsync();
    }

    private void updateDataNote() {
        dataNote.getStyleClass().remove("demo-note");
        if (demoMode) {
            dataNote.setText("Modo prueba · episodios de ejemplo · no se guardan");
            dataNote.getStyleClass().add("demo-note");
        } else {
            dataNote.setText(appDemo ? "Datos de ejemplo · F4.2" : "Datos locales · Solo en este equipo");
        }
    }

    private void refreshAsync() {
        refreshAsync(false);
    }

    private void refreshAsync(boolean finishSave) {
        if (viewModel == null || (saving && !finishSave)) return;
        LocalDate from = fromPicker.getValue() == null ? LocalDate.now() : fromPicker.getValue();
        LocalDate to = toPicker.getValue() == null ? LocalDate.now() : toPicker.getValue();
        if (from.isAfter(to)) {
            showRangeError();
            return;
        }
        clearRangeError();
        EmStatusFilter filter = statusFilter.getValue() == null ? EmStatusFilter.ALL : statusFilter.getValue();
        boolean include = includeJustified.isSelected();
        boolean useDemo = demoMode;
        EmInterruptionRow selected = table.getSelectionModel().getSelectedItem();
        long selectedId = selected == null ? -1 : selected.id();
        long request = refreshRequest.incrementAndGet();

        CompletableFuture.supplyAsync(() -> useDemo
                        ? viewModel.demoPage(from, filter, include)
                        : viewModel.load(from, to, filter, include), executor)
                .whenComplete((page, error) -> Platform.runLater(() -> {
                    if (request != refreshRequest.get()) return;
                    if (finishSave) saving = false;
                    if (error != null) showLoadError(error);
                    else applyPage(page, selectedId);
                    updateSaveEnabled();
                }));
    }

    private void showRangeError() {
        refreshRequest.incrementAndGet();
        savedStatusText = null;
        saveStatus.getStyleClass().removeAll("save-ok", "save-error");
        table.getItems().clear();
        table.setPlaceholder(new Label(
                "El rango no es válido: 'Desde' no puede ser posterior a 'Hasta'."));
        totalLabel.setText("—");
        openLabel.setText("—");
        updateDetail(null);
        saveStatus.setText("");
        rangeNote.setText("Desde no puede ser posterior a Hasta.");
        rangeNote.setManaged(true);
        rangeNote.setVisible(true);
    }

    private void clearRangeError() {
        rangeNote.setText("");
        rangeNote.setManaged(false);
        rangeNote.setVisible(false);
    }

    private void applyPage(EmInterruptionsPage page, long selectedId) {
        String pendingSaved = savedStatusText;
        table.setItems(FXCollections.observableArrayList(page.rows()));
        table.setPlaceholder(new Label("Sin datos registrados para este rango de fechas."));
        totalLabel.setText(String.valueOf(page.totalEpisodes()));
        openLabel.setText(String.valueOf(page.openEpisodes()));

        EmInterruptionRow selected = null;
        for (EmInterruptionRow row : page.rows()) {
            if (row.id() == selectedId) {
                selected = row;
                break;
            }
        }
        if (selected != null) {
            table.getSelectionModel().select(selected);
        } else if (!page.rows().isEmpty()) {
            selected = page.rows().getFirst();
            table.getSelectionModel().selectFirst();
        } else {
            table.getSelectionModel().clearSelection();
        }
        updateDetail(selected);
        if (pendingSaved != null) {
            saveStatus.setText(pendingSaved);
            saveStatus.getStyleClass().removeAll("save-error");
            saveStatus.getStyleClass().add("save-ok");
        }
        savedStatusText = null;
    }

    private void showLoadError(Throwable error) {
        savedStatusText = null;
        table.getItems().clear();
        table.setPlaceholder(new Label("No se pudieron cargar las interrupciones.\n" + message(error)));
        totalLabel.setText("—");
        openLabel.setText("—");
        updateDetail(null);
        saveStatus.setText("Error al cargar");
        saveStatus.getStyleClass().removeAll("save-ok", "save-error");
    }

    private void updateDetail(EmInterruptionRow row) {
        contextsPane.getChildren().clear();
        if (row == null) {
            detailTitle.setText("Detalle del episodio");
            detailTime.setText("Selecciona un episodio");
            detailFirstChange.setText("—");
            detailState.setText("—");
            detailStability.setText("—");
            detailCloseReason.setText("");
            detailAnnotation.setText("—");
            reasonCombo.setValue(null);
            justifiedCheck.setSelected(false);
            updateSaveEnabled();
            return;
        }

        detailTitle.setText("Episodio · " + row.startText());
        detailTime.setText(row.statusText() + " · " + row.firstChange());
        detailFirstChange.setText(row.firstChange());
        detailState.setText(row.statusText());
        detailStability.setText(row.stabilityText());
        detailCloseReason.setText(viewModel.stabilityExplanation(row));
        detailAnnotation.setText(viewModel.annotationDetail(row));
        for (var context : row.contexts()) {
            Label chip = new Label(viewModel.display(context));
            chip.getStyleClass().add("chip");
            contextsPane.getChildren().add(chip);
        }
        reasonCombo.setValue(viewModel.reason(row.reasonCode()));
        justifiedCheck.setSelected(row.justified());
        updateSaveEnabled();
    }

    private void updateSaveEnabled() {
        EmInterruptionRow row = table.getSelectionModel().getSelectedItem();
        EmReasonOption reason = reasonCombo.getValue();
        boolean changed = row != null && row.hasAnnotationChanges(
                reason == null ? null : reason.code(), justifiedCheck.isSelected());
        saveButton.setDisable(saving || demoMode || !changed);
        saveButton.setText(saving ? "Guardando…" : row != null && row.answeredAt() != null
                ? (changed ? "Guardar cambios" : "Guardado ✓") : "Guardar");
        reasonCombo.setDisable(saving || demoMode);
        justifiedCheck.setDisable(saving || demoMode);
        table.setDisable(saving);
        fromPicker.setDisable(saving);
        toPicker.setDisable(saving);
        statusFilter.setDisable(saving);
        includeJustified.setDisable(saving);
        demoButton.setDisable(saving);
    }

    private void save() {
        if (demoMode || saving) return;
        EmInterruptionRow row = table.getSelectionModel().getSelectedItem();
        EmReasonOption reason = reasonCombo.getValue();
        if (row == null || reason == null
                || !row.hasAnnotationChanges(reason.code(), justifiedCheck.isSelected())) return;
        boolean justified = justifiedCheck.isSelected();

        saving = true;
        refreshRequest.incrementAndGet();
        updateSaveEnabled();
        saveStatus.getStyleClass().removeAll("save-ok", "save-error");
        saveStatus.setText("Guardando…");
        CompletableFuture.runAsync(
                        () -> viewModel.annotate(row.id(), reason.code(), justified),
                        executor)
                .whenComplete((ignored, error) -> Platform.runLater(() -> {
                    if (error != null) {
                        saving = false;
                        updateSaveEnabled();
                        saveStatus.setText("No se pudo guardar: " + message(error));
                        saveStatus.getStyleClass().removeAll("save-ok");
                        saveStatus.getStyleClass().add("save-error");
                    } else {
                        savedStatusText = "Guardado · " + reason.label()
                                + (justified ? " · Justificada" : "")
                                + " · " + SAVED_TIME.format(LocalTime.now());
                        saveStatus.setText(savedStatusText);
                        saveStatus.getStyleClass().removeAll("save-error");
                        saveStatus.getStyleClass().add("save-ok");
                        refreshAsync(true);
                    }
                }));
    }

    private void resetSaveFeedback() {
        savedStatusText = null;
        saveStatus.setText("");
        saveStatus.getStyleClass().removeAll("save-ok", "save-error");
        updateSaveEnabled();
    }

    private void clearSavedNote() {
        if (saveStatus.getStyleClass().contains("save-ok")) {
            saveStatus.setText("");
            saveStatus.getStyleClass().remove("save-ok");
        }
    }

    private static TableCell<EmInterruptionRow, String> annotationCell() {
        return new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                getStyleClass().removeAll("annotation-saved", "annotation-pending");
                if (empty || item == null) {
                    setText(null);
                    return;
                }
                setText(item);
                getStyleClass().add("Guardada".equals(item)
                        ? "annotation-saved" : "annotation-pending");
            }
        };
    }

    private static TableCell<EmInterruptionRow, String> monoCell() {
        return new TableCell<>() {
            {
                getStyleClass().add("mono");
            }

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty ? null : item);
            }
        };
    }

    private static TableCell<EmInterruptionRow, String> statusCell() {
        return new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                getStyleClass().removeAll("status-open", "status-stable", "status-unconfirmed");
                setText(null);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setGraphic(null);
                    return;
                }
                EmInterruptionRow row = getTableRow().getItem();
                String stateClass = switch (row.status()) {
                    case OPEN -> "status-open";
                    case STABLE -> "status-stable";
                    case UNCONFIRMED -> "status-unconfirmed";
                };
                Circle dot = new Circle(4.2);
                dot.getStyleClass().add("status-dot-" + stateClass.substring("status-".length()));
                Label label = new Label(row.statusText());
                label.getStyleClass().add("status-text");
                HBox content = new HBox(8, dot, label);
                content.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
                setGraphic(content);
                getStyleClass().add(stateClass);
            }
        };
    }

    private static String message(Throwable error) {
        Throwable cause = error;
        if (cause instanceof CompletionException || cause instanceof ExecutionException) {
            if (cause.getCause() != null) cause = cause.getCause();
        }
        return cause.getMessage() == null ? cause.getClass().getSimpleName() : cause.getMessage();
    }
}
