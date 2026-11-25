package com.deriva.storage.sqlite;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class EmSQLiteSettingsRepositoryTest {
    @TempDir Path tempDirectory;

    private EmSQLiteSettingsRepository repository() {
        return new EmSQLiteSettingsRepository(tempDirectory.resolve("settings.db"));
    }

    @Test
    void emptyDatabaseHasNoSettings() {
        assertTrue(repository().loadAll().isEmpty());
    }

    @Test
    void savesUpdatesAndPreservesUnrelatedKeys() {
        EmSQLiteSettingsRepository repository = repository();
        Map<String, String> first = new LinkedHashMap<>();
        first.put("drift_switches", "8");
        first.put("drift_window_s", "90");
        first.put("stable_s", "240");
        first.put("idle_threshold_s", "300");
        first.put("other_key", "keep-me");
        repository.saveAll(first);

        Map<String, String> second = new LinkedHashMap<>();
        second.put("drift_switches", "12");
        repository.saveAll(second);

        Map<String, String> stored = repository.loadAll();
        assertEquals("12", stored.get("drift_switches"));
        assertEquals("90", stored.get("drift_window_s"));
        assertEquals("240", stored.get("stable_s"));
        assertEquals("300", stored.get("idle_threshold_s"));
        assertEquals("keep-me", stored.get("other_key"));
    }

    @Test
    void reopensWithPersistedValues() {
        EmSQLiteSettingsRepository first = repository();
        first.saveAll(Map.of("drift_switches", "6", "idle_threshold_s", "600"));
        Map<String, String> stored = repository().loadAll();
        assertEquals("6", stored.get("drift_switches"));
        assertEquals("600", stored.get("idle_threshold_s"));
    }
}
