package com.deriva.app;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.regex.Pattern;

/**
 * Where the desktop app keeps its data. From the source tree it uses the development
 * paths under {@code work/}; once packaged with jpackage it uses the user's local profile
 * ({@code %LOCALAPPDATA%\Deriva}) because the installation folder may be read-only.
 */
final class EmAppPaths {
    static final String DATA_FOLDER = "Deriva";
    static final String DATABASE_FILE = "deriva.db";
    static final String BRIDGE_FILE = "bridge.properties";
    private static final Pattern EXTENSION_ID = Pattern.compile("^[a-p]{32}$");
    private static final Pattern TOKEN = Pattern.compile("^[0-9a-f]{64}$");

    private EmAppPaths() {}

    /** True when running from a jpackage launcher (Deriva.exe). */
    static boolean packaged() {
        return System.getProperty("jpackage.app-path") != null;
    }

    /** {@code %LOCALAPPDATA%\Deriva}, falling back to the profile's AppData\Local. */
    static Path dataDirectory(Map<String, String> environment, String userHome) {
        String local = environment.get("LOCALAPPDATA");
        Path base = local == null || local.isBlank()
                ? Path.of(userHome, "AppData", "Local")
                : Path.of(local);
        return base.resolve(DATA_FOLDER);
    }

    /**
     * An explicit {@code DERIVA_UI_DB} always wins; otherwise demo, packaged and
     * development launches each keep their own database.
     */
    static Path database(Map<String, String> environment, String userHome,
                         boolean demo, boolean packaged) {
        String explicit = environment.get("DERIVA_UI_DB");
        if (explicit != null && !explicit.isBlank()) return Path.of(explicit).toAbsolutePath();
        if (demo) return Path.of("work/ui-preview/demo.db").toAbsolutePath();
        if (packaged) return dataDirectory(environment, userHome).resolve(DATABASE_FILE);
        return Path.of("work/console-day/deriva.db").toAbsolutePath();
    }

    /** Browser bridge settings read from {@code bridge.properties}; empty when absent or invalid. */
    static Optional<EmCaptureSession.BridgeConfig> bridgeFromFile(Path file) {
        if (file == null || !Files.isRegularFile(file)) return Optional.empty();
        Properties properties = new Properties();
        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            properties.load(reader);
        } catch (IOException | IllegalArgumentException e) {
            System.err.println("Deriva: no se pudo leer " + BRIDGE_FILE + "; navegadores excluidos.");
            return Optional.empty();
        }
        String extension = properties.getProperty("extensionId", "").trim();
        String token = properties.getProperty("token", "").trim();
        String portText = properties.getProperty("port", "47821").trim();
        int port;
        try {
            port = Integer.parseInt(portText);
        } catch (NumberFormatException e) {
            port = -1;
        }
        if (!EXTENSION_ID.matcher(extension).matches() || !TOKEN.matcher(token).matches()
                || port < 1024 || port > 65535) {
            System.err.println("Deriva: " + BRIDGE_FILE
                    + " incompleto o con formato incorrecto; navegadores excluidos.");
            return Optional.empty();
        }
        return Optional.of(new EmCaptureSession.BridgeConfig(port, token, extension));
    }
}
