package com.deriva.storage.sqlite;

import com.deriva.storage.StorageException;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** Defense in depth: the segment schema must never contain URL/title fields or URL-like hosts. */
public final class SQLitePrivacyCheck {
    private static final List<String> FORBIDDEN_COLUMNS =
            List.of("url", "uri", "title", "window_title", "query", "path", "content");

    private SQLitePrivacyCheck() {}

    public static void verify(Path databasePath) {
        try (Connection connection = SQLiteSchema.open(databasePath);
             Statement statement = connection.createStatement()) {
            Set<String> columns = new HashSet<>();
            try (ResultSet result = statement.executeQuery("PRAGMA table_info(segment)")) {
                while (result.next()) columns.add(result.getString("name").toLowerCase(Locale.ROOT));
            }
            for (String forbidden : FORBIDDEN_COLUMNS) {
                if (columns.contains(forbidden))
                    throw new StorageException("Forbidden segment column: " + forbidden);
            }

            try (ResultSet result = statement.executeQuery("""
                    SELECT COUNT(*) FROM segment
                    WHERE site_host LIKE '%/%' OR site_host LIKE '%?%'
                       OR site_host LIKE '%#%' OR site_host LIKE '%:%'
                    """)) {
                if (result.next() && result.getLong(1) > 0)
                    throw new StorageException("URL-like host found in segment table");
            }
        } catch (SQLException e) {
            throw new StorageException("Could not verify SQLite privacy", e);
        }
    }
}
