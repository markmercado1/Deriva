package com.deriva.ui.fx;

import com.deriva.ui.viewmodel.EmDailyUsageViewModel;
import com.deriva.ui.viewmodel.EmUsagePage;
import com.deriva.ui.viewmodel.EmUsageRow;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.DatePicker;
import javafx.scene.control.ComboBox;
import com.deriva.ui.viewmodel.EmUsagePeriod;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;

public final class EmUsageController {
    @FXML private DatePicker datePicker;
    @FXML private DatePicker endDatePicker;
    @FXML private ComboBox<EmUsagePeriod> periodCombo;
    @FXML private Label rangeLabel;
    private boolean updatingRange;
    private int pendingQueries;
    @FXML private Label totalLabel;
    @FXML private Label coverageLabel;
    @FXML private Label zoneLabel;
    @FXML private Label sourceLabel;
    @FXML private Label statusLabel;
    @FXML private VBox appsList;
    @FXML private VBox sitesList;

    private final ExecutorService executor = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "deriva-usage-query");
        thread.setDaemon(true);
        return thread;
    });
    private final AtomicLong refreshRequest = new AtomicLong();
    private final Timeline autoRefresh = new Timeline(
            new KeyFrame(Duration.seconds(30), event -> { if (pendingQueries == 0) refresh(); }));
    private EmDailyUsageViewModel viewModel;

    @FXML
    private void initialize() {
        datePicker.setValue(LocalDate.now());
        datePicker.setEditable(false);
        endDatePicker.setEditable(false);
        periodCombo.getItems().setAll(EmUsagePeriod.values());
        periodCombo.setValue(EmUsagePeriod.DAY);
        datePicker.valueProperty().addListener((observable, old, value) -> updateRange());
        endDatePicker.valueProperty().addListener((observable, old, value) -> { if (!updatingRange) refresh(); });
        periodCombo.valueProperty().addListener((observable, old, value) -> {
            if (value == EmUsagePeriod.DAY) {
                updatingRange = true;
                datePicker.setValue(LocalDate.now());
                updatingRange = false;
            }
            updateRange();
        });
        updateRange();
        autoRefresh.setCycleCount(Animation.INDEFINITE);
    }

    public void init(EmDailyUsageViewModel viewModel, boolean demo) {
        this.viewModel = viewModel;
        sourceLabel.setText(demo ? "Datos de ejemplo · F4.3" : "Datos locales · Solo en este equipo");
        refresh();
    }

    public void refresh() {
        if (viewModel == null) return;
        LocalDate day = datePicker.getValue() == null ? LocalDate.now() : datePicker.getValue();
        long request = refreshRequest.incrementAndGet();
        LocalDate through = endDatePicker.getValue();
        if (through == null || through.isBefore(day)) {
            showLoadError(new IllegalArgumentException("Desde debe ser anterior o igual a Hasta."));
            return;
        }
        pendingQueries++;
        CompletableFuture.supplyAsync(() -> viewModel.load(day, through), executor)
                .whenComplete((page, error) -> Platform.runLater(() -> {
                    pendingQueries--;
                    if (request != refreshRequest.get()) return;
                    if (error != null) showLoadError(error);
                    else apply(page);
                }));
    }

    private void updateRange() {
        if (updatingRange || periodCombo.getValue() == null || datePicker.getValue() == null) return;
        updatingRange = true;
        EmUsagePeriod period = periodCombo.getValue();
        if (period != EmUsagePeriod.CUSTOM) {
            LocalDate anchor = datePicker.getValue();
            datePicker.setValue(period.start(anchor));
            endDatePicker.setValue(period.end(anchor));
        }
        endDatePicker.setDisable(period != EmUsagePeriod.CUSTOM);
        rangeLabel.setText(period == EmUsagePeriod.WEEK ? "Semana de lunes a domingo · fechas inclusivas"
                : "Fechas inclusivas · solo se suma el tiempo registrado");
        updatingRange = false;
        refresh();
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

    private void apply(EmUsagePage page) {
        clearErrorStyle();
        totalLabel.setText(page.totalText());
        coverageLabel.setText(page.coverageText());
        zoneLabel.setText(page.zoneText());
        statusLabel.setText(page.statusText());

        String empty = page.hasSegments() ? "Sin uso en primer plano." : "Sin datos para este periodo.";
        render(appsList, page.apps(), empty);
        render(sitesList, page.sites(), page.hasSegments() ? "Sin sitios registrados." : empty);
    }

    private void showLoadError(Throwable error) {
        totalLabel.setText("—");
        coverageLabel.setText("—");
        zoneLabel.setText("");
        statusLabel.setText("No se pudieron cargar los totales. " + message(error));
        if (!statusLabel.getStyleClass().contains("range-error"))
            statusLabel.getStyleClass().add("range-error");
        renderMessage(appsList, "No disponible por error de carga.");
        renderMessage(sitesList, "No disponible por error de carga.");
    }

    private void clearErrorStyle() {
        statusLabel.getStyleClass().remove("range-error");
    }

    private static void render(VBox container, List<EmUsageRow> rows, String emptyText) {
        container.getChildren().clear();
        if (rows.isEmpty()) {
            renderMessage(container, emptyText);
            return;
        }
        for (EmUsageRow row : rows) {
            Label name = new Label(row.name());
            name.getStyleClass().add("usage-name");
            name.setMinWidth(130);
            name.setPrefWidth(150);
            name.setMaxWidth(150);

            ProgressBar bar = new ProgressBar(row.ratio());
            bar.getStyleClass().add("usage-bar");
            bar.setMaxWidth(Double.MAX_VALUE);
            HBox.setHgrow(bar, Priority.ALWAYS);

            Label duration = new Label(row.durationText());
            duration.getStyleClass().add("usage-duration");
            duration.setMinWidth(92);
            duration.setAlignment(Pos.CENTER_RIGHT);

            HBox line = new HBox(10, name, bar, duration);
            line.setAlignment(Pos.CENTER_LEFT);
            line.setMaxWidth(Double.MAX_VALUE);
            line.getStyleClass().add("usage-row");
            container.getChildren().add(line);
        }
    }

    private static void renderMessage(VBox container, String text) {
        container.getChildren().clear();
        Label label = new Label(text);
        label.getStyleClass().add("usage-empty");
        label.setWrapText(true);
        container.getChildren().add(label);
    }

    private static String message(Throwable error) {
        Throwable cause = error;
        while ((cause instanceof CompletionException || cause instanceof ExecutionException)
                && cause.getCause() != null) {
            cause = cause.getCause();
        }
        return cause.getMessage() == null ? cause.getClass().getSimpleName() : cause.getMessage();
    }
}
