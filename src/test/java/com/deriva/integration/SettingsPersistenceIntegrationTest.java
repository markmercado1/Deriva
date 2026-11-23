package com.deriva.integration;

import com.deriva.app.usecase.SettingsService;
import com.deriva.coach.model.DetectionSettings;
import com.deriva.storage.sqlite.SQLiteSettingsRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.Duration;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class SettingsPersistenceIntegrationTest {
    @TempDir Path tempDirectory;

    @Test
    void savesReloadsAndKeepsInvalidStoredValueUntouched() {
        Path database = tempDirectory.resolve("settings.db");
        SQLiteSettingsRepository repository = new SQLiteSettingsRepository(database);
        SettingsService service = new SettingsService(repository);
        DetectionSettings custom = new DetectionSettings(8,
                Duration.ofSeconds(90), Duration.ofSeconds(240), Duration.ofSeconds(300));
        service.save(custom);

        SettingsService reopened = new SettingsService(new SQLiteSettingsRepository(database));
        assertEquals(custom, reopened.current());
        assertTrue(reopened.snapshot().warnings().isEmpty());

        repository.saveAll(Map.of(SettingsService.KEY_SWITCHES, "3",
                SettingsService.KEY_IDLE, "no-numerico"));
        SettingsService invalid = new SettingsService(new SQLiteSettingsRepository(database));
        assertEquals(DetectionSettings.DEFAULT_SWITCHES, invalid.current().switches());
        assertEquals(DetectionSettings.DEFAULT_IDLE_SECONDS, invalid.current().idle().toSeconds());
        assertEquals(2, invalid.snapshot().warnings().size());
        assertEquals("3", repository.loadAll().get(SettingsService.KEY_SWITCHES));
        assertEquals("no-numerico", repository.loadAll().get(SettingsService.KEY_IDLE));
    }
}
