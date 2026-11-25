package com.deriva.app;

import com.deriva.app.usecase.EmBridgeSettingsPort;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.SecureRandom;
import java.util.HexFormat;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/** Stores the extension settings in {@code bridge.properties} inside the user's data folder. */
final class EmBridgeSettingsFile implements EmBridgeSettingsPort {
    private static final SecureRandom RANDOM = new SecureRandom();

    private final Path file;
    private final Path extensionFolder;
    private final Map<String, String> environment;

    EmBridgeSettingsFile(Path file, Path extensionFolder, Map<String, String> environment) {
        this.file = Objects.requireNonNull(file, "file");
        this.extensionFolder = Objects.requireNonNull(extensionFolder, "extensionFolder");
        this.environment = Map.copyOf(environment);
    }

    /** New random local token: 32 bytes as 64 lowercase hex characters. */
    static String newToken() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return HexFormat.of().formatHex(bytes);
    }

    @Override
    public Optional<Values> load() {
        return EmAppPaths.bridgeFromFile(file)
                .map(config -> new Values(config.extensionId(), config.token(), config.port()));
    }

    @Override
    public void save(Values values) {
        Objects.requireNonNull(values, "values");
        // Validates exactly like the capture does before anything is written.
        new EmCaptureSession.BridgeConfig(values.port(), values.token(), values.extensionId());
        String text = "# Configuración de la extensión de Deriva (no compartas el token)\n"
                + "extensionId=" + values.extensionId() + "\n"
                + "token=" + values.token() + "\n"
                + "port=" + values.port() + "\n";
        try {
            Files.createDirectories(file.getParent());
            Path temporary = file.resolveSibling(file.getFileName() + ".tmp");
            Files.writeString(temporary, text, StandardCharsets.UTF_8);
            Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo guardar " + file.getFileName(), e);
        }
    }

    @Override
    public void clear() {
        try {
            Files.deleteIfExists(file);
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo borrar " + file.getFileName(), e);
        }
    }

    @Override
    public boolean overriddenByEnvironment() {
        String token = environment.get("DERIVA_BRIDGE_TOKEN");
        String extension = environment.get("DERIVA_EXTENSION_ID");
        return token != null && !token.isBlank() && extension != null && !extension.isBlank();
    }

    @Override
    public String extensionFolder() {
        return extensionFolder.toString();
    }
}
