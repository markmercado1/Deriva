package com.deriva.app;

import com.deriva.ui.viewmodel.EmInterruptionRow;
import com.deriva.ui.viewmodel.EmReasonOption;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import javafx.util.Duration;

import java.time.LocalDate;

/**
 * Diagnostic UI path for F4.1/F4.2. It drives the real controls and the Guardar
 * button; it never calls the repository directly.
 */
final class EmUiSmoke {
    private EmUiSmoke() {}

    @SuppressWarnings("unchecked")
    static void annotateFirstRow(Scene scene, Runnable onComplete) {
        waitForRows(scene, 12, () -> {
            TableView<EmInterruptionRow> table =
                    (TableView<EmInterruptionRow>) scene.lookup("#table");
            if (table == null || table.getItems().isEmpty()) {
                System.out.println("UI smoke: no hay episodios para anotar");
                onComplete.run();
                return;
            }

            table.getSelectionModel().selectFirst();
            EmInterruptionRow row = table.getSelectionModel().getSelectedItem();
            ComboBox<EmReasonOption> reason =
                    (ComboBox<EmReasonOption>) scene.lookup("#reasonCombo");
            EmReasonOption messages = reason.getItems().stream()
                    .filter(option -> "MESSAGES".equals(option.code()))
                    .findFirst()
                    .orElse(null);
            reason.setValue(messages);
            CheckBox justified = (CheckBox) scene.lookup("#justifiedCheck");
            justified.setSelected(true);

            Button save = (Button) scene.lookup("#saveButton");
            System.out.println("UI smoke: episodio=" + row.id()
                    + ", motivo=" + (messages == null ? "null" : messages.code())
                    + ", justificada=true, guardarDeshabilitado=" + save.isDisabled());
            save.fire();
            waitForSaved(scene, 12, onComplete);
        });
    }

    @SuppressWarnings("unchecked")
    static void demoEpisodes(Scene scene, Runnable onComplete) {
        Button demo = (Button) scene.lookup("#demoButton");
        if (demo == null) {
            System.out.println("UI smoke: no se encontro el boton de datos de ejemplo");
            onComplete.run();
            return;
        }
        demo.fire();
        waitForDemo(scene, 12, () -> {
            TableView<EmInterruptionRow> table =
                    (TableView<EmInterruptionRow>) scene.lookup("#table");
            Button save = (Button) scene.lookup("#saveButton");
            Label note = (Label) scene.lookup("#dataNote");
            System.out.println("UI smoke: modo ejemplo=" + demo.getText()
                    + ", filas=" + (table == null ? -1 : table.getItems().size())
                    + ", guardarDeshabilitado=" + (save == null || save.isDisabled())
                    + ", nota=" + (note == null ? "null" : note.getText()));
            onComplete.run();
        });
    }

    private static void waitForDemo(Scene scene, int attempts, Runnable onComplete) {
        PauseTransition wait = new PauseTransition(Duration.seconds(0.3));
        wait.setOnFinished(event -> {
            Label note = (Label) scene.lookup("#dataNote");
            if ((note != null && note.getText().contains("Modo prueba")) || attempts <= 1) {
                onComplete.run();
            } else {
                waitForDemo(scene, attempts - 1, onComplete);
            }
        });
        wait.play();
    }

    private static String labelText(Scene scene, String selector) {
        Label label = (Label) scene.lookup(selector);
        return label == null ? "null" : label.getText();
    }

    private static boolean labelVisible(Scene scene, String selector) {
        Label label = (Label) scene.lookup(selector);
        return label != null && label.isVisible();
    }

    private static void waitForSaved(Scene scene, int attempts, Runnable onComplete) {
        PauseTransition wait = new PauseTransition(Duration.seconds(0.3));
        wait.setOnFinished(event -> {
            Label status = (Label) scene.lookup("#saveStatus");
            String text = status == null ? "" : status.getText();
            if (text.startsWith("Guardado") || text.startsWith("No se pudo") || attempts <= 1) {
                System.out.println("UI smoke: estado tras Guardar="
                        + (status == null ? "null" : text));
                onComplete.run();
            } else {
                waitForSaved(scene, attempts - 1, onComplete);
            }
        });
        wait.play();
    }

    static void captureCycle(Scene scene, int runSeconds, Runnable onLive, Runnable onComplete) {
        Button capture = (Button) scene.lookup("#captureButton");
        if (capture == null) {
            System.out.println("UI smoke: no se encontró el botón de captura");
            onComplete.run();
            return;
        }
        capture.fire();
        System.out.println("UI smoke: captura solicitada");

        PauseTransition run = new PauseTransition(Duration.seconds(runSeconds));
        run.setOnFinished(event -> {
            System.out.println("UI smoke: estado captura=" + labelText(scene, "#captureStatus"));
            Button usage = (Button) scene.lookup("#usageButton");
            if (usage != null) usage.fire();

            PauseTransition live = new PauseTransition(Duration.seconds(1.5));
            live.setOnFinished(liveEvent -> {
                System.out.println("UI smoke: uso durante captura=" + labelText(scene, "#statusLabel"));
                onLive.run();
                if (!"Reintentar captura".equals(capture.getText())) capture.fire();

                PauseTransition stopped = new PauseTransition(Duration.seconds(2.5));
                stopped.setOnFinished(stopEvent -> {
                    System.out.println("UI smoke: estado tras detener=" + labelText(scene, "#captureStatus"));
                    Button usageAgain = (Button) scene.lookup("#usageButton");
                    if (usageAgain != null) usageAgain.fire();

                    PauseTransition done = new PauseTransition(Duration.seconds(1.2));
                    done.setOnFinished(doneEvent -> onComplete.run());
                    done.play();
                });
                stopped.play();
            });
            live.play();
        });
        run.play();
    }

    static void captureThenClose(Scene scene, int runSeconds, Runnable onComplete) {
        Button capture = (Button) scene.lookup("#captureButton");
        if (capture == null) {
            System.out.println("UI smoke: no se encontró el botón de captura");
            onComplete.run();
            return;
        }
        capture.fire();
        PauseTransition wait = new PauseTransition(Duration.seconds(runSeconds));
        wait.setOnFinished(event -> {
            System.out.println("UI smoke: cierre con captura=" + labelText(scene, "#captureStatus"));
            onComplete.run();
        });
        wait.play();
    }

    static void captureThenWindowClose(Scene scene, int runSeconds, Runnable onBeforeClose) {
        Button capture = (Button) scene.lookup("#captureButton");
        if (capture == null) {
            System.out.println("UI smoke: no se encontró el botón de captura");
            onBeforeClose.run();
            return;
        }
        capture.fire();
        System.out.println("UI smoke: captura solicitada");
        PauseTransition wait = new PauseTransition(Duration.seconds(runSeconds));
        wait.setOnFinished(event -> {
            System.out.println("UI smoke: cierre de ventana con captura="
                    + labelText(scene, "#captureStatus"));
            onBeforeClose.run();
            if (scene.getWindow() instanceof Stage stage) stage.close();
            else Platform.exit();
        });
        wait.play();
    }

    static void pauseResume(Scene scene, int runSeconds, Runnable onPaused, Runnable onComplete) {
        Button capture = (Button) scene.lookup("#captureButton");
        Button pause = (Button) scene.lookup("#pauseButton");
        if (capture == null || pause == null) {
            System.out.println("UI smoke: no se encontro el control de pausa");
            onComplete.run();
            return;
        }
        capture.fire();
        PauseTransition running = new PauseTransition(Duration.seconds(runSeconds));
        running.setOnFinished(event -> {
            System.out.println("UI smoke: estado antes de pausar="
                    + labelText(scene, "#captureStatus"));
            pause.fire();
            PauseTransition paused = new PauseTransition(Duration.seconds(2.5));
            paused.setOnFinished(pausedEvent -> {
                System.out.println("UI smoke: estado en pausa="
                        + labelText(scene, "#captureStatus") + ", boton=" + pause.getText());
                onPaused.run();
                pause.fire();
                PauseTransition resumed = new PauseTransition(Duration.seconds(3));
                resumed.setOnFinished(resumedEvent -> {
                    System.out.println("UI smoke: estado tras reanudar="
                            + labelText(scene, "#captureStatus"));
                    capture.fire();
                    PauseTransition stopped = new PauseTransition(Duration.seconds(2.5));
                    stopped.setOnFinished(stopEvent -> {
                        System.out.println("UI smoke: estado final="
                                + labelText(scene, "#captureStatus"));
                        onComplete.run();
                    });
                    stopped.play();
                });
                resumed.play();
            });
            paused.play();
        });
        running.play();
    }

    static void trayCycle(Scene scene, EmAppTray appTray, Runnable beforeExit, Runnable fallback) {
        Button capture = (Button) scene.lookup("#captureButton");
        Stage stage = scene.getWindow() instanceof Stage window ? window : null;
        if (appTray == null || capture == null || stage == null) {
            System.out.println("UI smoke: bandeja no disponible=" + (appTray == null));
            fallback.run();
            return;
        }
        capture.fire();
        PauseTransition running = new PauseTransition(Duration.seconds(4));
        running.setOnFinished(event -> {
            System.out.println("UI smoke: bandeja=instalada, estado="
                    + labelText(scene, "#captureStatus"));
            appTray.togglePauseForSmoke();
            PauseTransition paused = new PauseTransition(Duration.seconds(2.5));
            paused.setOnFinished(pausedEvent -> {
                System.out.println("UI smoke: estado tras pausa desde bandeja="
                        + labelText(scene, "#captureStatus"));
                appTray.togglePauseForSmoke();
                PauseTransition resumed = new PauseTransition(Duration.seconds(3));
                resumed.setOnFinished(resumedEvent -> {
                    System.out.println("UI smoke: estado tras reanudar desde bandeja="
                            + labelText(scene, "#captureStatus"));
                    stage.fireEvent(new WindowEvent(stage, WindowEvent.WINDOW_CLOSE_REQUEST));
                    PauseTransition hidden = new PauseTransition(Duration.seconds(1));
                    hidden.setOnFinished(hiddenEvent -> {
                        System.out.println("UI smoke: ventana oculta=" + !stage.isShowing()
                                + ", estado=" + labelText(scene, "#captureStatus"));
                        appTray.openForSmoke();
                        PauseTransition shown = new PauseTransition(Duration.seconds(1));
                        shown.setOnFinished(shownEvent -> {
                            System.out.println("UI smoke: ventana reabierta=" + stage.isShowing()
                                    + ", estado=" + labelText(scene, "#captureStatus"));
                            beforeExit.run();
                            appTray.exitForSmoke();
                        });
                        shown.play();
                    });
                    hidden.play();
                });
                resumed.play();
            });
            paused.play();
        });
        running.play();
    }

    static void settingsCycle(Scene scene, Runnable onComplete) {
        Button settings = (Button) scene.lookup("#settingsButton");
        if (settings == null) {
            System.out.println("UI smoke: no se encontró Ajustes");
            onComplete.run();
            return;
        }
        settings.fire();
        PauseTransition opened = new PauseTransition(Duration.seconds(0.5));
        opened.setOnFinished(event -> {
            TextField switches = (TextField) scene.lookup("#switchesField");
            TextField window = (TextField) scene.lookup("#windowField");
            TextField stability = (TextField) scene.lookup("#stabilityField");
            TextField idle = (TextField) scene.lookup("#idleField");
            Button save = (Button) scene.lookup("#saveButton");
            if (switches == null || window == null || stability == null || idle == null || save == null) {
                System.out.println("UI smoke: no se encontraron los campos de Ajustes");
                onComplete.run();
                return;
            }
            System.out.println("UI smoke: ajustes iniciales=" + labelText(scene, "#effectiveValues")
                    + ", guardarDeshabilitado=" + save.isDisabled());
            switches.setText("8");
            window.setText("90");
            stability.setText("240");
            idle.setText("300");

            PauseTransition edited = new PauseTransition(Duration.seconds(0.3));
            edited.setOnFinished(editedEvent -> {
                System.out.println("UI smoke: ajustes editados, guardarDeshabilitado=" + save.isDisabled()
                        + ", avisoInactividad=" + labelText(scene, "#idleWarningLabel"));
                save.fire();
                waitForSettingsSaved(scene, 15, () -> {
                    System.out.println("UI smoke: ajustes tras guardar="
                            + labelText(scene, "#saveStatus")
                            + "; efectivo=" + labelText(scene, "#effectiveValues")
                            + "; pendiente=" + labelText(scene, "#pendingValues"));
                    onComplete.run();
                });
            });
            edited.play();
        });
        opened.play();
    }

    static void settingsSessionCycle(Scene scene, Runnable onComplete) {
        Button capture = (Button) scene.lookup("#captureButton");
        Button pause = (Button) scene.lookup("#pauseButton");
        Button settings = (Button) scene.lookup("#settingsButton");
        if (capture == null || pause == null || settings == null) {
            System.out.println("UI smoke: no se encontraron los controles de sesión");
            onComplete.run();
            return;
        }
        capture.fire();
        PauseTransition running = new PauseTransition(Duration.seconds(2.5));
        running.setOnFinished(event -> {
            settings.fire();
            PauseTransition opened = new PauseTransition(Duration.seconds(0.5));
            opened.setOnFinished(openedEvent -> {
                TextField switches = (TextField) scene.lookup("#switchesField");
                TextField window = (TextField) scene.lookup("#windowField");
                TextField stability = (TextField) scene.lookup("#stabilityField");
                TextField idle = (TextField) scene.lookup("#idleField");
                Button save = (Button) scene.lookup("#saveButton");
                if (switches == null || window == null || stability == null || idle == null || save == null) {
                    System.out.println("UI smoke: no se encontraron los campos de sesión");
                    onComplete.run();
                    return;
                }
                switches.setText("8");
                window.setText("90");
                stability.setText("240");
                idle.setText("300");
                PauseTransition edited = new PauseTransition(Duration.seconds(0.3));
                edited.setOnFinished(editedEvent -> {
                    save.fire();
                    waitForSettingsSaved(scene, 15, () -> {
                        System.out.println("UI smoke: efectivo tras guardar con captura="
                                + labelText(scene, "#effectiveValues")
                                + "; pendienteVisible=" + labelVisible(scene, "#pendingValues"));
                        pause.fire();
                        PauseTransition paused = new PauseTransition(Duration.seconds(2.5));
                        paused.setOnFinished(pausedEvent -> {
                            System.out.println("UI smoke: efectivo en pausa="
                                    + labelText(scene, "#effectiveValues")
                                    + "; pendienteVisible=" + labelVisible(scene, "#pendingValues"));
                            pause.fire();
                            PauseTransition resumed = new PauseTransition(Duration.seconds(2.5));
                            resumed.setOnFinished(resumedEvent -> {
                                System.out.println("UI smoke: efectivo tras reanudar="
                                        + labelText(scene, "#effectiveValues")
                                        + "; pendienteVisible=" + labelVisible(scene, "#pendingValues"));
                                capture.fire();
                                PauseTransition stopped = new PauseTransition(Duration.seconds(2.5));
                                stopped.setOnFinished(stopEvent -> {
                                    System.out.println("UI smoke: efectivo tras detener="
                                            + labelText(scene, "#effectiveValues")
                                            + "; pendienteVisible=" + labelVisible(scene, "#pendingValues"));
                                    capture.fire();
                                    PauseTransition restarted = new PauseTransition(Duration.seconds(2.5));
                                    restarted.setOnFinished(restartEvent -> {
                                        System.out.println("UI smoke: efectivo tras iniciar="
                                                + labelText(scene, "#effectiveValues")
                                                + "; pendienteVisible=" + labelVisible(scene, "#pendingValues"));
                                        capture.fire();
                                        PauseTransition done = new PauseTransition(Duration.seconds(2));
                                        done.setOnFinished(doneEvent -> onComplete.run());
                                        done.play();
                                    });
                                    restarted.play();
                                });
                                stopped.play();
                            });
                            resumed.play();
                        });
                        paused.play();
                    });
                });
                edited.play();
            });
            opened.play();
        });
        running.play();
    }

    static void goalsCycle(Scene scene, Runnable onCreated, Runnable onComplete) {
        Button goals = (Button) scene.lookup("#goalsButton");
        if (goals == null) {
            System.out.println("UI smoke: no se encontró Objetivos");
            onComplete.run();
            return;
        }
        goals.fire();
        PauseTransition opened = new PauseTransition(Duration.seconds(0.8));
        opened.setOnFinished(event -> {
            TextField process = (TextField) scene.lookup("#processField");
            TextField host = (TextField) scene.lookup("#hostField");
            TextField minutes = (TextField) scene.lookup("#minutesField");
            TextField entries = (TextField) scene.lookup("#entriesField");
            CheckBox passive = (CheckBox) scene.lookup("#passiveCheck");
            Button save = (Button) scene.lookup("#saveButton");
            if (process == null || host == null || minutes == null || entries == null
                    || passive == null || save == null) {
                System.out.println("UI smoke: no se encontraron los campos de Objetivos");
                onComplete.run();
                return;
            }
            process.setText("chess.exe");
            host.setText("");
            minutes.setText("60");
            entries.setText("4");
            passive.setSelected(true);

            PauseTransition edited = new PauseTransition(Duration.seconds(0.3));
            edited.setOnFinished(editedEvent -> {
                System.out.println("UI smoke: objetivo a crear=chess.exe 60 min 4 entradas, "
                        + "guardarDeshabilitado=" + save.isDisabled());
                save.fire();
                waitForGoalSaved(scene, 20, () -> {
                    Label context = (Label) scene.lookup(".goal-context");
                    Label state = (Label) scene.lookup(".goal-state");
                    Label progress = (Label) scene.lookup(".goal-progress");
                    System.out.println("UI smoke: objetivo creado="
                            + (context == null ? "null" : context.getText())
                            + "; estado=" + (state == null ? "null" : state.getText())
                            + "; progreso=" + (progress == null ? "null" : progress.getText()));
                    onCreated.run();
                    Button delete = (Button) scene.getRoot().lookupAll(".goal-delete-button").stream()
                            .findFirst().orElse(null);
                    if (delete == null) {
                        System.out.println("UI smoke: no se encontró Borrar objetivo");
                        onComplete.run();
                        return;
                    }
                    delete.fire();
                    PauseTransition confirm = new PauseTransition(Duration.seconds(0.4));
                    confirm.setOnFinished(confirmEvent -> {
                        System.out.println("UI smoke: confirmación de borrado="
                                + labelText(scene, "#deleteWarning"));
                        Button confirmButton = (Button) scene.lookup("#confirmDeleteButton");
                        if (confirmButton == null) {
                            System.out.println("UI smoke: no se encontró confirmar borrado");
                            onComplete.run();
                            return;
                        }
                        confirmButton.fire();
                        PauseTransition done = new PauseTransition(Duration.seconds(1.5));
                        done.setOnFinished(doneEvent -> {
                            System.out.println("UI smoke: objetivo borrado="
                                    + labelText(scene, "#saveStatus")
                                    + "; resumen=" + labelText(scene, "#summaryLabel"));
                            onComplete.run();
                        });
                        done.play();
                    });
                    confirm.play();
                });
            });
            edited.play();
        });
        opened.play();
    }

    static void goalsJustifyCycle(Scene scene, Runnable onJustified, Runnable onComplete) {
        Button goals = (Button) scene.lookup("#goalsButton");
        if (goals == null) {
            System.out.println("UI smoke: no se encontró Objetivos");
            onComplete.run();
            return;
        }
        goals.fire();
        PauseTransition opened = new PauseTransition(Duration.seconds(0.8));
        opened.setOnFinished(event -> {
            Button justify = buttonByText(scene, "Justificar exceso");
            if (justify == null) {
                System.out.println("UI smoke: no se encontró Justificar exceso");
                onComplete.run();
                return;
            }
            justify.fire();
            PauseTransition modal = new PauseTransition(Duration.seconds(0.4));
            modal.setOnFinished(modalEvent -> {
                TextArea reason = (TextArea) scene.lookup("#justifyReasonField");
                Button confirm = (Button) scene.lookup("#confirmJustifyButton");
                if (reason == null || confirm == null) {
                    System.out.println("UI smoke: no se encontró el diálogo de justificación");
                    onComplete.run();
                    return;
                }
                reason.setText("Partida con un amigo");
                confirm.fire();
                waitForJustificationStatus(scene, "Justificación guardada", 20, () -> {
                    System.out.println("UI smoke: justificado=" + firstText(scene, ".goal-state")
                            + "; motivo=" + firstText(scene, ".goal-justification")
                            + "; cobertura=" + firstText(scene, ".goal-justification-coverage")
                            + "; estado=" + labelText(scene, "#saveStatus"));
                    onJustified.run();
                    Button remove = buttonByText(scene, "Quitar justificación");
                    if (remove == null) {
                        System.out.println("UI smoke: no se encontró Quitar justificación");
                        onComplete.run();
                        return;
                    }
                    remove.fire();
                    waitForJustificationStatus(scene, "Justificación quitada", 20, () -> {
                        System.out.println("UI smoke: tras quitar=" + firstText(scene, ".goal-state")
                                + "; nota=" + firstText(scene, ".goal-justification-note")
                                + "; estado=" + labelText(scene, "#saveStatus"));
                        onComplete.run();
                    });
                });
            });
            modal.play();
        });
        opened.play();
    }

    private static void waitForJustificationStatus(Scene scene, String prefix, int attempts,
                                                   Runnable onComplete) {
        PauseTransition wait = new PauseTransition(Duration.seconds(0.3));
        wait.setOnFinished(event -> {
            String text = labelText(scene, "#saveStatus");
            if (text.startsWith(prefix) || text.startsWith("No se pudo") || attempts <= 1) {
                onComplete.run();
            } else {
                waitForJustificationStatus(scene, prefix, attempts - 1, onComplete);
            }
        });
        wait.play();
    }

    private static Button buttonByText(Scene scene, String text) {
        return scene.getRoot().lookupAll(".goal-justify-button").stream()
                .filter(node -> node instanceof Button button && text.equals(button.getText()))
                .map(node -> (Button) node)
                .findFirst()
                .orElse(null);
    }

    private static String firstText(Scene scene, String selector) {
        return scene.getRoot().lookupAll(selector).stream()
                .filter(node -> node instanceof Label)
                .map(node -> ((Label) node).getText())
                .findFirst()
                .orElse("null");
    }

    private static void waitForGoalSaved(Scene scene, int attempts, Runnable onComplete) {
        PauseTransition wait = new PauseTransition(Duration.seconds(0.3));
        wait.setOnFinished(event -> {
            String text = labelText(scene, "#saveStatus");
            if (text.startsWith("Objetivo creado") || text.startsWith("No se pudo") || attempts <= 1) {
                onComplete.run();
            } else {
                waitForGoalSaved(scene, attempts - 1, onComplete);
            }
        });
        wait.play();
    }

    private static void waitForSettingsSaved(Scene scene, int attempts, Runnable onComplete) {
        PauseTransition wait = new PauseTransition(Duration.seconds(0.3));
        wait.setOnFinished(event -> {
            String text = labelText(scene, "#saveStatus");
            if (text.startsWith("Guardado") || text.startsWith("No se pudo") || attempts <= 1) {
                onComplete.run();
            } else {
                waitForSettingsSaved(scene, attempts - 1, onComplete);
            }
        });
        wait.play();
    }

    static void showDeleteConfirmation(Scene scene, Runnable onComplete) {
        Button delete = (Button) scene.lookup("#deleteButton");
        if (delete == null) {
            System.out.println("UI smoke: no se encontró Borrar registros");
            onComplete.run();
            return;
        }
        delete.fire();
        PauseTransition wait = new PauseTransition(Duration.seconds(0.5));
        wait.setOnFinished(event -> {
            System.out.println("UI smoke: confirmación visible="
                    + labelText(scene, "#deleteWarning"));
            onComplete.run();
        });
        wait.play();
    }

    static void deleteData(Scene scene, int runSeconds, Runnable onComplete) {
        Button capture = (Button) scene.lookup("#captureButton");
        if (capture == null) {
            System.out.println("UI smoke: no se encontró el botón de captura");
            onComplete.run();
            return;
        }
        capture.fire();
        PauseTransition wait = new PauseTransition(Duration.seconds(runSeconds));
        wait.setOnFinished(event -> {
            Button delete = (Button) scene.lookup("#deleteButton");
            if (delete == null) {
                System.out.println("UI smoke: no se encontró Borrar registros");
                onComplete.run();
                return;
            }
            delete.fire();
            PauseTransition confirm = new PauseTransition(Duration.seconds(0.3));
            confirm.setOnFinished(confirmEvent -> {
                System.out.println("UI smoke: confirmación=" + labelText(scene, "#deleteWarning"));
                Button confirmButton = (Button) scene.lookup("#confirmDeleteButton");
                if (confirmButton == null) {
                    System.out.println("UI smoke: no se encontró confirmar borrado");
                    onComplete.run();
                    return;
                }
                confirmButton.fire();
                PauseTransition after = new PauseTransition(Duration.seconds(2.5));
                after.setOnFinished(afterEvent -> {
                    System.out.println("UI smoke: estado tras borrar="
                            + labelText(scene, "#captureStatus"));
                    Button usage = (Button) scene.lookup("#usageButton");
                    if (usage != null) usage.fire();
                    PauseTransition done = new PauseTransition(Duration.seconds(1.2));
                    done.setOnFinished(doneEvent -> onComplete.run());
                    done.play();
                });
                after.play();
            });
            confirm.play();
        });
        wait.play();
    }

    static void captureFailure(Scene scene, int runSeconds, Runnable onComplete) {
        Button capture = (Button) scene.lookup("#captureButton");
        if (capture == null) {
            System.out.println("UI smoke: no se encontró el botón de captura");
            onComplete.run();
            return;
        }
        capture.fire();
        PauseTransition wait = new PauseTransition(Duration.seconds(runSeconds));
        wait.setOnFinished(event -> {
            String status = labelText(scene, "#captureStatus");
            System.out.println("UI smoke: estado antes de detener=" + status);
            if (!status.startsWith("Error")) capture.fire();
            PauseTransition after = new PauseTransition(Duration.seconds(2.5));
            after.setOnFinished(afterEvent -> {
                System.out.println("UI smoke: estado tras fallo="
                        + labelText(scene, "#captureStatus"));
                onComplete.run();
            });
            after.play();
        });
        wait.play();
    }

    static void showInvalidRange(Scene scene, Runnable onComplete) {
        DatePicker from = (DatePicker) scene.lookup("#fromPicker");
        DatePicker to = (DatePicker) scene.lookup("#toPicker");
        LocalDate today = LocalDate.now();
        from.setValue(today.plusDays(1));
        to.setValue(today);

        PauseTransition pause = new PauseTransition(Duration.seconds(0.6));
        pause.setOnFinished(event -> {
            Label note = (Label) scene.lookup("#rangeNote");
            System.out.println("UI smoke: aviso de rango="
                    + (note == null ? "null" : note.getText()));
            onComplete.run();
        });
        pause.play();
    }

    @SuppressWarnings("unchecked")
    private static void waitForRows(Scene scene, int attempts, Runnable action) {
        PauseTransition wait = new PauseTransition(Duration.seconds(0.4));
        wait.setOnFinished(event -> {
            TableView<EmInterruptionRow> table =
                    (TableView<EmInterruptionRow>) scene.lookup("#table");
            if ((table != null && !table.getItems().isEmpty()) || attempts <= 1) {
                action.run();
            } else {
                waitForRows(scene, attempts - 1, action);
            }
        });
        wait.play();
    }
}
