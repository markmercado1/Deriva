package com.deriva.app.usecase;

import java.util.Optional;

/**
 * Where the browser-extension settings live. The application layer only sees these
 * plain values; the file format belongs to the adapter.
 */
public interface EmBridgeSettingsPort {
    /** Extension id (32 letters a–p), local token (64 hex characters) and loopback port. */
    record Values(String extensionId, String token, int port) {}

    Optional<Values> load();

    void save(Values values);

    void clear();

    /** True when the environment (Start-Ui.ps1 -ExtensionId) overrides the saved values. */
    boolean overriddenByEnvironment();

    /** Folder that holds the unpacked extension to load in Chrome or Brave. */
    String extensionFolder();
}
