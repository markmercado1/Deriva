package com.deriva.ui.viewmodel;

import com.deriva.app.usecase.EmBridgeSettingsPort;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.regex.Pattern;

/**
 * Presentation model of the «Extensión del navegador» settings: validates the extension id,
 * the token and the port, and explains when the new values apply. No JavaFX, no files.
 */
public final class EmBrowserExtensionViewModel {
    public static final int DEFAULT_PORT = 47821;
    public static final String NOT_CONFIGURED =
            "No configurada: el tiempo en Chrome y Brave queda excluido y no se registran sitios.";
    public static final String SAVED =
            "Configuración guardada. Pega el token en las opciones de la extensión y vuelve a iniciar la captura para aplicarla.";
    public static final String CLEARED =
            "Configuración quitada. Al volver a iniciar la captura, los navegadores quedarán excluidos.";
    public static final String ID_ERROR =
            "El ID de la extensión debe tener 32 letras minúsculas entre la a y la p. Cópialo de la página de extensiones del navegador.";
    public static final String PORT_ERROR = "El puerto debe ser un número entre 1024 y 65535.";
    public static final String TOKEN_ERROR = "Pulsa «Generar token» antes de guardar.";
    public static final String ENVIRONMENT_NOTE =
            "Deriva se abrió con Start-Ui.ps1 -ExtensionId: esa configuración tiene prioridad sobre la guardada aquí.";

    private static final Pattern EXTENSION_ID = Pattern.compile("^[a-p]{32}$");
    private static final Pattern TOKEN = Pattern.compile("^[0-9a-f]{64}$");

    public record Page(String status, boolean configured, String extensionId, String portText,
                       String token, String extensionFolder, String environmentNote,
                       String validationError, boolean canSave, boolean canClear) {}

    public record Outcome(Page page, String message, boolean error) {}

    private final EmBridgeSettingsPort port;
    private final Supplier<String> tokens;
    private Optional<EmBridgeSettingsPort.Values> saved = Optional.empty();
    private String extensionId = "";
    private String portText = Integer.toString(DEFAULT_PORT);
    private String token = "";
    private String validationError;

    public EmBrowserExtensionViewModel(EmBridgeSettingsPort port, Supplier<String> tokens) {
        this.port = Objects.requireNonNull(port, "port");
        this.tokens = Objects.requireNonNull(tokens, "tokens");
    }

    public Page load() {
        saved = port.load();
        saved.ifPresentOrElse(values -> {
            extensionId = values.extensionId();
            portText = Integer.toString(values.port());
            token = values.token();
        }, () -> {
            extensionId = "";
            portText = Integer.toString(DEFAULT_PORT);
            token = "";
        });
        validationError = null;
        return page();
    }

    public Page update(String extensionIdText, String portValue) {
        extensionId = extensionIdText == null ? "" : extensionIdText.trim().toLowerCase(java.util.Locale.ROOT);
        portText = portValue == null ? "" : portValue.trim();
        validationError = null;
        return page();
    }

    /** Creates a new token; the old one stops working once this one is saved. */
    public Page generateToken() {
        token = tokens.get();
        validationError = null;
        return page();
    }

    public Outcome save() {
        String error = validate();
        if (error != null) {
            validationError = error;
            return new Outcome(page(), error, true);
        }
        try {
            port.save(new EmBridgeSettingsPort.Values(extensionId, token, Integer.parseInt(portText)));
            saved = port.load();
            validationError = null;
            return new Outcome(page(), SAVED, false);
        } catch (RuntimeException e) {
            return new Outcome(page(), "No se pudo guardar la configuración: " + e.getMessage(), true);
        }
    }

    public Outcome clear() {
        try {
            port.clear();
            return new Outcome(load(), CLEARED, false);
        } catch (RuntimeException e) {
            return new Outcome(page(), "No se pudo quitar la configuración: " + e.getMessage(), true);
        }
    }

    public Page page() {
        String status = saved.map(values -> "Configurada: extensión " + values.extensionId()
                        + " · puerto " + values.port() + ".")
                .orElse(NOT_CONFIGURED);
        return new Page(status, saved.isPresent(), extensionId, portText, token, port.extensionFolder(),
                port.overriddenByEnvironment() ? ENVIRONMENT_NOTE : null, validationError,
                !extensionId.isBlank() && !portText.isBlank(), saved.isPresent());
    }

    private String validate() {
        if (!EXTENSION_ID.matcher(extensionId).matches()) return ID_ERROR;
        int value;
        try {
            value = Integer.parseInt(portText);
        } catch (NumberFormatException e) {
            return PORT_ERROR;
        }
        if (value < 1024 || value > 65535) return PORT_ERROR;
        if (!TOKEN.matcher(token).matches()) return TOKEN_ERROR;
        return null;
    }
}
