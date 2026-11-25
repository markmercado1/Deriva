package com.deriva.ui.viewmodel;

import com.deriva.app.usecase.EmBridgeSettingsPort;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EmBrowserExtensionViewModelTest {
    private static final String ID = "cnipocakflhegoediialcdngmopbieip";
    private static final String TOKEN = "0123456789abcdef".repeat(4);

    private static final class MemoryPort implements EmBridgeSettingsPort {
        Optional<Values> stored = Optional.empty();
        boolean environment;

        public Optional<Values> load() { return stored; }
        public void save(Values values) { stored = Optional.of(values); }
        public void clear() { stored = Optional.empty(); }
        public boolean overriddenByEnvironment() { return environment; }
        public String extensionFolder() { return "C:\\Deriva\\extension"; }
    }

    @Test
    void startsUnconfiguredWithTheDefaultPort() {
        var model = new EmBrowserExtensionViewModel(new MemoryPort(), () -> TOKEN);
        var page = model.load();
        assertEquals(EmBrowserExtensionViewModel.NOT_CONFIGURED, page.status());
        assertEquals("47821", page.portText());
        assertFalse(page.canSave());
        assertFalse(page.canClear());
        assertEquals("C:\\Deriva\\extension", page.extensionFolder());
    }

    @Test
    void savesAValidIdTokenAndPortAndExplainsWhenItApplies() {
        MemoryPort port = new MemoryPort();
        var model = new EmBrowserExtensionViewModel(port, () -> TOKEN);
        model.load();
        model.update("  " + ID.toUpperCase() + " ", "47821");
        model.generateToken();

        var outcome = model.save();

        assertFalse(outcome.error());
        assertEquals(EmBrowserExtensionViewModel.SAVED, outcome.message());
        assertEquals(new EmBridgeSettingsPort.Values(ID, TOKEN, 47821), port.stored.orElseThrow());
        assertTrue(outcome.page().status().startsWith("Configurada: extensión " + ID));
        assertTrue(outcome.page().canClear());
    }

    @Test
    void rejectsAnInvalidIdPortOrMissingTokenWithoutSaving() {
        MemoryPort port = new MemoryPort();
        var model = new EmBrowserExtensionViewModel(port, () -> TOKEN);
        model.load();

        model.update("no-es-un-id", "47821");
        assertEquals(EmBrowserExtensionViewModel.ID_ERROR, model.save().message());
        model.update(ID, "80");
        assertEquals(EmBrowserExtensionViewModel.PORT_ERROR, model.save().message());
        model.update(ID, "47821");
        assertEquals(EmBrowserExtensionViewModel.TOKEN_ERROR, model.save().message());
        assertTrue(port.stored.isEmpty());
    }

    @Test
    void clearingRemovesTheSettingsAndReloadsTheDefaults() {
        MemoryPort port = new MemoryPort();
        port.stored = Optional.of(new EmBridgeSettingsPort.Values(ID, TOKEN, 47900));
        var model = new EmBrowserExtensionViewModel(port, () -> TOKEN);
        assertEquals("47900", model.load().portText());

        var outcome = model.clear();

        assertEquals(EmBrowserExtensionViewModel.CLEARED, outcome.message());
        assertTrue(port.stored.isEmpty());
        assertEquals("", outcome.page().token());
        assertEquals("47821", outcome.page().portText());
    }

    @Test
    void warnsWhenStartUiOverridesTheSavedSettings() {
        MemoryPort port = new MemoryPort();
        port.environment = true;
        var page = new EmBrowserExtensionViewModel(port, () -> TOKEN).load();
        assertEquals(EmBrowserExtensionViewModel.ENVIRONMENT_NOTE, page.environmentNote());
        port.environment = false;
        assertNull(new EmBrowserExtensionViewModel(port, () -> TOKEN).load().environmentNote());
    }
}
