package com.deriva.integration;

import com.deriva.app.usecase.EmSettingsService;
import com.deriva.coach.model.EmDetectionSettings;
import com.deriva.storage.sqlite.EmSQLiteSettingsRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.Duration;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class EmSettingsPersistenceIntegrationTest {
    @TempDir Path tempDirectory;

    @Test
    void savesReloadsAndKeepsInvalidStoredValueUntouched() {
        Path database = tempDirectory.resolve("settings.db");
        EmSQLiteSettingsRepository repository = new EmSQLiteSettingsRepository(database);
        EmSettingsService service = new EmSettingsService(repository);
        EmDetectionSettings custom = new EmDetectionSettings(8,
                Duration.ofSeconds(90), Duration.ofSeconds(240), Duration.ofSeconds(300));
        service.save(custom);

        EmSettingsService reopened = new EmSettingsService(new EmSQLiteSettingsRepository(database));
        assertEquals(custom, reopened.current());
        assertTrue(reopened.snapshot().warnings().isEmpty());

        repository.saveAll(Map.of(EmSettingsService.KEY_SWITCHES, "3",
                EmSettingsService.KEY_IDLE, "no-numerico"));
        EmSettingsService invalid = new EmSettingsService(new EmSQLiteSettingsRepository(database));
        assertEquals(EmDetectionSettings.DEFAULT_SWITCHES, invalid.current().switches());
        assertEquals(EmDetectionSettings.DEFAULT_IDLE_SECONDS, invalid.current().idle().toSeconds());
        assertEquals(2, invalid.snapshot().warnings().size());
        assertEquals("3", repository.loadAll().get(EmSettingsService.KEY_SWITCHES));
        assertEquals("no-numerico", repository.loadAll().get(EmSettingsService.KEY_IDLE));
    }
}
