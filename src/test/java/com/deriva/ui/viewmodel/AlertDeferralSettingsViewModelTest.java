package com.deriva.ui.viewmodel;

import com.deriva.app.usecase.AlertDeferralSnapshot;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AlertDeferralSettingsViewModelTest {
    private static AlertDeferralSettingsViewModel loaded(Duration saved, Duration effective) {
        AlertDeferralSettingsViewModel viewModel = new AlertDeferralSettingsViewModel();
        viewModel.load(new AlertDeferralSnapshot(saved, List.of()), effective);
        return viewModel;
    }

    @Test
    void defaultsAreFiveMinutesWithoutChanges() {
        AlertDeferralSettingsViewModel.Page page =
                loaded(Duration.ofMinutes(5), Duration.ofMinutes(5)).page();
        assertEquals("5", page.minutesText());
        assertTrue(page.valid());
        assertFalse(page.pending());
        assertFalse(page.unsavedChanges());
        assertFalse(page.canSave());
    }

    @Test
    void savedValueDifferentFromSessionIsPending() {
        AlertDeferralSettingsViewModel.Page page =
                loaded(Duration.ofMinutes(20), Duration.ofMinutes(5)).page();
        assertTrue(page.pending());
        assertTrue(page.savedText().startsWith("1200 s"));
        assertTrue(page.effectiveText().startsWith("300 s"));
    }

    @Test
    void editedValueEnablesSaveAndValidatesRange() {
        AlertDeferralSettingsViewModel viewModel =
                loaded(Duration.ofMinutes(5), Duration.ofMinutes(5));
        viewModel.update("20");
        AlertDeferralSettingsViewModel.Page page = viewModel.page();
        assertTrue(page.unsavedChanges());
        assertTrue(page.canSave());
        assertEquals(Duration.ofMinutes(20), viewModel.validatedValue());

        viewModel.update("0");
        assertFalse(viewModel.page().valid());
        assertFalse(viewModel.page().canSave());
        assertThrows(IllegalStateException.class, viewModel::validatedValue);

        viewModel.update("31");
        assertFalse(viewModel.page().valid());

        viewModel.update("cinco");
        assertTrue(viewModel.page().validationError().contains("entero"));
    }

    @Test
    void restoreDefaultsOnlyChangesTheForm() {
        AlertDeferralSettingsViewModel viewModel =
                loaded(Duration.ofMinutes(20), Duration.ofMinutes(5));
        viewModel.update("30");
        viewModel.restoreDefaults();
        assertEquals("5", viewModel.page().minutesText());
        assertTrue(viewModel.page().unsavedChanges());
        assertTrue(viewModel.page().savedText().startsWith("1200 s"));
    }
}
