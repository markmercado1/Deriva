package com.deriva.app.usecase;

import com.deriva.coach.model.DetectionSettings;
import com.deriva.storage.port.SettingsRepository;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class SettingsServiceTest {
    private static final class FakeRepository implements SettingsRepository {
        private final Map<String, String> values = new LinkedHashMap<>();
        private boolean failSave;

        @Override
        public Map<String, String> loadAll() {
            return Map.copyOf(values);
        }

        @Override
        public void saveAll(Map<String, String> saved) {
            if (failSave) throw new IllegalStateException("save failed");
            values.putAll(saved);
        }
    }

    @Test
    void missingKeysUseDefaultsWithoutWarnings() {
        SettingsSnapshot snapshot = new SettingsService(new FakeRepository()).snapshot();
        assertEquals(DetectionSettings.defaults(), snapshot.values());
        assertTrue(snapshot.warnings().isEmpty());
    }

    @Test
    void invalidValueFallsBackAndKeepsOtherStoredValues() {
        FakeRepository repository = new FakeRepository();
        repository.values.put(SettingsService.KEY_SWITCHES, "3");
        repository.values.put(SettingsService.KEY_WINDOW, "90");
        repository.values.put(SettingsService.KEY_STABILITY, "240");
        repository.values.put(SettingsService.KEY_IDLE, "300");

        SettingsSnapshot snapshot = new SettingsService(repository).snapshot();
        assertEquals(10, snapshot.values().switches());
        assertEquals(Duration.ofSeconds(90), snapshot.values().window());
        assertEquals(Duration.ofSeconds(240), snapshot.values().stability());
        assertEquals(Duration.ofSeconds(300), snapshot.values().idle());
        assertEquals(1, snapshot.warnings().size());
        assertTrue(snapshot.warnings().getFirst().contains("10"));
        assertEquals("3", repository.values.get(SettingsService.KEY_SWITCHES),
                "lectura no debe sobrescribir el valor guardado");
    }

    @Test
    void nonNumericValueFallsBackVisibly() {
        FakeRepository repository = new FakeRepository();
        repository.values.put(SettingsService.KEY_IDLE, "dos minutos");
        SettingsSnapshot snapshot = new SettingsService(repository).snapshot();
        assertEquals(120, snapshot.values().idle().toSeconds());
        assertTrue(snapshot.warnings().getFirst().contains("inactividad"));
    }

    @Test
    void saveWritesAllFourKeysAndClearsWarnings() {
        FakeRepository repository = new FakeRepository();
        repository.values.put(SettingsService.KEY_SWITCHES, "3");
        SettingsService service = new SettingsService(repository);
        DetectionSettings saved = new DetectionSettings(8,
                Duration.ofSeconds(90), Duration.ofSeconds(240), Duration.ofSeconds(300));

        SettingsSnapshot snapshot = service.save(saved);

        assertEquals(saved, snapshot.values());
        assertTrue(snapshot.warnings().isEmpty());
        assertEquals("8", repository.values.get(SettingsService.KEY_SWITCHES));
        assertEquals("90", repository.values.get(SettingsService.KEY_WINDOW));
        assertEquals("240", repository.values.get(SettingsService.KEY_STABILITY));
        assertEquals("300", repository.values.get(SettingsService.KEY_IDLE));
    }

    @Test
    void failedSaveLeavesPreviouslyLoadedSnapshot() {
        FakeRepository repository = new FakeRepository();
        SettingsService service = new SettingsService(repository);
        SettingsSnapshot before = service.snapshot();
        repository.failSave = true;

        assertThrows(IllegalStateException.class, () -> service.save(new DetectionSettings(8,
                Duration.ofSeconds(90), Duration.ofSeconds(240), Duration.ofSeconds(300))));
        assertEquals(before, service.snapshot());
    }
}
