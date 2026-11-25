package com.deriva.ui.viewmodel;

import com.deriva.app.usecase.EmSettingsSnapshot;
import com.deriva.coach.model.EmDetectionSettings;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class EmSettingsViewModelTest {
    private static final EmDetectionSettings DEFAULTS = EmDetectionSettings.defaults();
    private static final EmDetectionSettings CUSTOM = new EmDetectionSettings(8,
            Duration.ofSeconds(90), Duration.ofSeconds(240), Duration.ofSeconds(300));

    private static EmSettingsViewModel loaded(EmDetectionSettings saved, EmDetectionSettings effective,
                                            List<String> warnings) {
        EmSettingsViewModel viewModel = new EmSettingsViewModel();
        viewModel.load(new EmSettingsSnapshot(saved, warnings), effective);
        return viewModel;
    }

    @Test
    void defaultsAreValidWithoutSaveAndWarnAboutIdle() {
        EmSettingsViewModel.Page page = loaded(DEFAULTS, DEFAULTS, List.of()).page();
        assertTrue(page.valid());
        assertFalse(page.canSave());
        assertFalse(page.unsavedChanges());
        assertFalse(page.pending());
        assertTrue(page.idleWarning());
        assertEquals("10", page.switchesText());
        assertEquals("120", page.windowText());
        assertEquals("180", page.stabilityText());
    }

    @Test
    void editedValuesEnableSaveAndFollowIdleWarningRule() {
        EmSettingsViewModel viewModel = loaded(DEFAULTS, DEFAULTS, List.of());
        viewModel.update("8", "90", "240", "300");
        EmSettingsViewModel.Page page = viewModel.page();

        assertTrue(page.valid());
        assertTrue(page.canSave());
        assertTrue(page.unsavedChanges());
        assertFalse(page.idleWarning());
        assertEquals("90 s (1 min 30 s)", EmSettingsViewModel.seconds(Duration.ofSeconds(90)));
    }

    @Test
    void invalidValuesDisableSave() {
        EmSettingsViewModel viewModel = loaded(DEFAULTS, DEFAULTS, List.of());
        viewModel.update("3", "90", "240", "300");
        EmSettingsViewModel.Page page = viewModel.page();
        assertFalse(page.valid());
        assertFalse(page.canSave());
        assertTrue(page.validationError().contains("entre 4 y 30"));
        assertThrows(IllegalStateException.class, viewModel::validatedValues);

        viewModel.update("8", "90", "treinta", "300");
        assertTrue(viewModel.page().validationError().contains("números enteros"));
    }

    @Test
    void restoredDefaultsOnlyChangeTheForm() {
        EmSettingsViewModel viewModel = loaded(CUSTOM, CUSTOM, List.of());
        viewModel.restoreDefaults();
        EmSettingsViewModel.Page page = viewModel.page();

        assertEquals("10", page.switchesText());
        assertTrue(page.unsavedChanges());
        assertTrue(page.canSave());
        assertEquals(DEFAULTS, viewModel.validatedValues());
        assertTrue(page.savedSummary().startsWith("8 cambios"));
    }

    @Test
    void savedPendingIsDistinctFromEffectiveValues() {
        EmSettingsViewModel viewModel = loaded(CUSTOM, DEFAULTS, List.of());
        EmSettingsViewModel.Page page = viewModel.page();

        assertTrue(page.pending());
        assertFalse(page.canSave());
        assertTrue(page.savedSummary().startsWith("8 cambios"));
        assertTrue(page.effectiveSummary().startsWith("10 cambios"));
    }

    @Test
    void loadWarningsStayVisibleAndAreCopied() {
        EmSettingsViewModel viewModel = loaded(DEFAULTS, DEFAULTS,
                List.of("Cambios: el valor guardado no es válido; se usa 10 cambios."));
        EmSettingsViewModel.Page page = viewModel.page();
        assertEquals(1, page.loadWarnings().size());
    }
}
