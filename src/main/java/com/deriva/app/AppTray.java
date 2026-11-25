package com.deriva.app;

import com.deriva.app.usecase.CaptureControl;
import com.deriva.app.usecase.CaptureState;
import com.deriva.app.usecase.GoalAlertNotice;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.awt.AWTException;
import java.awt.Color;
import java.awt.EventQueue;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.MenuItem;
import java.awt.PopupMenu;
import java.awt.RenderingHints;
import java.awt.SystemTray;
import java.awt.TrayIcon;
import java.awt.image.BufferedImage;
import java.util.Objects;

/**
 * Basic Windows tray for F5.2: open, pause/resume and exit. It only lives in the
 * app layer; the capture port stays UI-agnostic. Closing the window hides it,
 * and the Salir item always stops and flushes the capture before exiting.
 */
final class AppTray {
    private final Stage stage;
    private final CaptureControl capture;
    private final Runnable exitAction;
    private final TrayIcon icon;
    private final MenuItem pauseItem;
    private final Timeline poll;

    static AppTray install(Stage stage, CaptureControl capture, Runnable exitAction) {
        Objects.requireNonNull(stage, "stage");
        Objects.requireNonNull(capture, "capture");
        Objects.requireNonNull(exitAction, "exitAction");
        if (!SystemTray.isSupported()) {
            System.out.println("Tray: no soportada");
            return null;
        }
        try {
            return new AppTray(stage, capture, exitAction);
        } catch (AWTException | UnsupportedOperationException | SecurityException e) {
            System.out.println("Tray: no se pudo instalar (" + e.getClass().getSimpleName() + ")");
            return null;
        } catch (RuntimeException e) {
            System.out.println("Tray: no se pudo instalar (" + e.getClass().getSimpleName() + ")");
            return null;
        }
    }

    private AppTray(Stage stage, CaptureControl capture, Runnable exitAction) throws AWTException {
        this.stage = stage;
        this.capture = capture;
        this.exitAction = exitAction;

        PopupMenu menu = new PopupMenu();
        MenuItem openItem = new MenuItem("Abrir Deriva");
        pauseItem = new MenuItem("Pausar captura");
        MenuItem exitItem = new MenuItem("Salir");
        openItem.addActionListener(event -> open());
        pauseItem.addActionListener(event -> Platform.runLater(this::togglePause));
        exitItem.addActionListener(event -> Platform.runLater(exitAction));
        menu.add(openItem);
        menu.add(pauseItem);
        menu.addSeparator();
        menu.add(exitItem);

        icon = new TrayIcon(iconImage(), "Deriva — actividad local", menu);
        icon.setImageAutoSize(true);
        icon.addActionListener(event -> open());
        SystemTray.getSystemTray().add(icon);

        poll = new Timeline(new KeyFrame(Duration.millis(500), event -> updatePauseItem()));
        poll.setCycleCount(Animation.INDEFINITE);
        poll.play();
        updatePauseItem();
        System.out.println("Tray: instalada");
    }

    void notifyHidden() {
        try {
            icon.displayMessage("Deriva sigue en la bandeja",
                    "La captura continúa. Usa el menú para abrir, pausar o salir.",
                    TrayIcon.MessageType.INFO);
        } catch (RuntimeException ignored) {
            // A notification failure must not affect capture or shutdown.
        }
    }

    void notifyGoal(GoalAlertNotice notice) {
        Objects.requireNonNull(notice, "notice");
        EventQueue.invokeLater(() -> {
            try {
                icon.displayMessage(notice.title(), notice.body(), TrayIcon.MessageType.INFO);
            } catch (RuntimeException e) {
                System.err.println("Tray: no se pudo mostrar el aviso ("
                        + e.getClass().getSimpleName() + ")");
            }
        });
    }

    void remove() {
        poll.stop();
        try {
            SystemTray.getSystemTray().remove(icon);
        } catch (RuntimeException ignored) {
            // Already removed or tray disposed during shutdown.
        }
    }

    private void open() {
        Platform.runLater(() -> {
            stage.show();
            stage.setIconified(false);
            stage.toFront();
            stage.requestFocus();
        });
    }

    private void togglePause() {
        CaptureState state = capture.status().state();
        if (state == CaptureState.RUNNING || state == CaptureState.STARTING) {
            capture.pause();
        } else if (state == CaptureState.PAUSED) {
            capture.start();
        }
        updatePauseItem();
    }

    private void updatePauseItem() {
        CaptureState state = capture.status().state();
        String label = state == CaptureState.PAUSED ? "Reanudar captura" : "Pausar captura";
        boolean enabled = state == CaptureState.RUNNING || state == CaptureState.STARTING
                || state == CaptureState.PAUSED;
        EventQueue.invokeLater(() -> {
            pauseItem.setLabel(label);
            pauseItem.setEnabled(enabled);
        });
    }

    private static BufferedImage iconImage() {
        BufferedImage image = new BufferedImage(32, 32, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        try {
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
            graphics.setColor(new Color(0xB7, 0x54, 0x36));
            graphics.fillRoundRect(2, 2, 28, 28, 9, 9);
            graphics.setColor(new Color(0xFF, 0xFE, 0xFB));
            graphics.setFont(new Font("Segoe UI", Font.BOLD, 21));
            graphics.drawString("D", 9, 24);
        } finally {
            graphics.dispose();
        }
        return image;
    }

    void togglePauseForSmoke() {
        togglePause();
    }

    void openForSmoke() {
        open();
    }

    void exitForSmoke() {
        Platform.runLater(exitAction);
    }
}
