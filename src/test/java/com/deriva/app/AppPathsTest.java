package com.deriva.app;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AppPathsTest {
    private static final String HOME = "C:\\Users\\demo";

    @Test
    void packagedAppKeepsItsDatabaseInLocalAppData() {
        Path db = AppPaths.database(Map.of("LOCALAPPDATA", "D:\\Local"), HOME, false, true);
        assertEquals(Path.of("D:\\Local", "Deriva", "deriva.db"), db);
    }

    @Test
    void packagedAppFallsBackToTheProfileWhenLocalAppDataIsMissing() {
        Path db = AppPaths.database(Map.of(), HOME, false, true);
        assertEquals(Path.of(HOME, "AppData", "Local", "Deriva", "deriva.db"), db);
    }

    @Test
    void explicitDatabaseWinsAndDevelopmentKeepsTheWorkFolder() {
        assertEquals(Path.of("X:\\datos\\otra.db"),
                AppPaths.database(Map.of("DERIVA_UI_DB", "X:\\datos\\otra.db"), HOME, false, true));
        assertTrue(AppPaths.database(Map.of(), HOME, false, false)
                .endsWith(Path.of("work", "console-day", "deriva.db")));
        assertTrue(AppPaths.database(Map.of(), HOME, true, true)
                .endsWith(Path.of("work", "ui-preview", "demo.db")));
    }

    @Test
    void readsAValidBridgeFileAndRejectsMissingOrMalformedOnes(@TempDir Path dir) throws Exception {
        Path file = dir.resolve("bridge.properties");
        assertTrue(AppPaths.bridgeFromFile(file).isEmpty());

        String token = "0123456789abcdef".repeat(4);
        Files.writeString(file, "extensionId=cnipocakflhegoediialcdngmopbieip\ntoken=" + token
                + "\nport=47821\n");
        var config = AppPaths.bridgeFromFile(file).orElseThrow();
        assertEquals(47821, config.port());
        assertEquals("cnipocakflhegoediialcdngmopbieip", config.extensionId());

        Files.writeString(file, "extensionId=cnipocakflhegoediialcdngmopbieip\ntoken=corto\n");
        assertTrue(AppPaths.bridgeFromFile(file).isEmpty());
        Files.writeString(file, "extensionId=NOVALIDO\ntoken=" + token + "\nport=80\n");
        assertTrue(AppPaths.bridgeFromFile(file).isEmpty());
    }
}
