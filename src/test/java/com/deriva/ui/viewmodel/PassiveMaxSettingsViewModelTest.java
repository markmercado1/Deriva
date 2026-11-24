package com.deriva.ui.viewmodel;

import com.deriva.app.usecase.PassiveMaxSnapshot;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PassiveMaxSettingsViewModelTest {
    private static PassiveMaxSettingsViewModel loaded(Duration saved, Duration effective) {
        PassiveMaxSettingsViewModel viewModel = new PassiveMaxSettingsViewModel();
        viewModel.load(new PassiveMaxSnapshot(saved, List.of()), effective);
        return viewModel;
    }

    @Test
    void defaultsAreTenMinutesWithoutChanges() {
        PassiveMaxSettingsViewModel.Page page = loaded(Duration.ofMinutes(10), Duration.ofMinutes(10)).page();
        assertEquals("10", page.minutesText());
        assertTrue(page.valid());
        assertFalse(page.pending());
        assertFalse(page.unsavedChanges());
        assertFalse(page.canSave());
    }

    @Test
    void savedValueDifferentFromSessionIsPending() {
        PassiveMaxSettingsViewModel.Page page = loaded(Duration.ofMinutes(25), Duration.ofMinutes(10)).page();
        assertTrue(page.pending());
        assertTrue(page.savedText().startsWith("1500 s"));
        assertTrue(page.effectiveText().startsWith("600 s"));
    }

    @Test
    void editedValueEnablesSaveAndValidatesRange() {
        PassiveMaxSettingsViewModel viewModel = loaded(Duration.ofMinutes(10), Duration.ofMinutes(10));
        viewModel.update("25");
        PassiveMaxSettingsViewModel.Page page = viewModel.page();
        assertTrue(page.unsavedChanges());
        assertTrue(page.canSave());
        assertEquals(Duration.ofMinutes(25), viewModel.validatedValue());

        viewModel.update("0");
        assertFalse(viewModel.page().valid());
        assertFalse(viewModel.page().canSave());
        assertThrows(IllegalStateException.class, viewModel::validatedValue);

        viewModel.update("sesenta");
        assertTrue(viewModel.page().validationError().contains("entero"));
    }

    @Test
    void restoreDefaultsOnlyChangesTheForm() {
        PassiveMaxSettingsViewModel viewModel = loaded(Duration.ofMinutes(25), Duration.ofMinutes(10));
        viewModel.update("30");
        viewModel.restoreDefaults();
        assertEquals("10", viewModel.page().minutesText());
        assertTrue(viewModel.page().unsavedChanges());
        assertTrue(viewModel.page().savedText().startsWith("1500 s"));
    }
}
