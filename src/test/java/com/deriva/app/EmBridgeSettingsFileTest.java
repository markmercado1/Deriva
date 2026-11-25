package com.deriva.app;

import com.deriva.app.usecase.EmBridgeSettingsPort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EmBridgeSettingsFileTest {
    private static final String ID = "cnipocakflhegoediialcdngmopbieip";

    @Test
    void savesReadsBackAndClearsTheBridgeFile(@TempDir Path dir) {
        Path file = dir.resolve("Deriva").resolve("bridge.properties");
        var store = new EmBridgeSettingsFile(file, dir.resolve("extension"), Map.of());
        assertTrue(store.load().isEmpty());

        String token = EmBridgeSettingsFile.newToken();
        store.save(new EmBridgeSettingsPort.Values(ID, token, 47821));

        assertTrue(Files.isRegularFile(file));
        assertEquals(new EmBridgeSettingsPort.Values(ID, token, 47821), store.load().orElseThrow());
        // The capture reads the very same file.
        assertEquals(token, EmAppPaths.bridgeFromFile(file).orElseThrow().token());

        store.clear();
        assertFalse(Files.exists(file));
        assertTrue(store.load().isEmpty());
    }

    @Test
    void refusesInvalidValuesAndCreatesDistinctTokens(@TempDir Path dir) {
        var store = new EmBridgeSettingsFile(dir.resolve("bridge.properties"), dir, Map.of());
        assertThrows(IllegalArgumentException.class,
                () -> store.save(new EmBridgeSettingsPort.Values("corto", EmBridgeSettingsFile.newToken(), 47821)));
        assertFalse(Files.exists(dir.resolve("bridge.properties")));
        String first = EmBridgeSettingsFile.newToken();
        assertTrue(first.matches("[0-9a-f]{64}"));
        assertNotEquals(first, EmBridgeSettingsFile.newToken());
    }

    @Test
    void environmentFromStartUiTakesPrecedence(@TempDir Path dir) {
        Path file = dir.resolve("bridge.properties");
        assertTrue(new EmBridgeSettingsFile(file, dir,
                Map.of("DERIVA_BRIDGE_TOKEN", "x", "DERIVA_EXTENSION_ID", "y")).overriddenByEnvironment());
        assertFalse(new EmBridgeSettingsFile(file, dir, Map.of()).overriddenByEnvironment());
    }
}
