package com.deriva.storage.port;

import java.util.Map;

/** Persistence port for local key/value settings. It never interprets the values. */
public interface EmSettingsRepository {
    Map<String, String> loadAll();

    void saveAll(Map<String, String> values);
}
