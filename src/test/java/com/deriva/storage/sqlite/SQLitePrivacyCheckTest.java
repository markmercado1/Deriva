package com.deriva.storage.sqlite;

import com.deriva.storage.StorageException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.*;

class SQLitePrivacyCheckTest {
    @TempDir Path tempDirectory;

    @Test
    void acceptsTheV1SegmentSchema() {
        Path database = tempDirectory.resolve("ok.db");
        new SQLiteSegmentRepository(database);
        assertDoesNotThrow(() -> SQLitePrivacyCheck.verify(database));
    }

    @Test
    void rejectsAForbiddenTitleColumn() throws Exception {
        Path database = tempDirectory.resolve("bad.db");
        try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + database);
             Statement statement = connection.createStatement()) {
            statement.execute("CREATE TABLE segment (site_host TEXT, window_title TEXT)");
        }
        assertThrows(StorageException.class, () -> SQLitePrivacyCheck.verify(database));
    }

    @Test
    void rejectsUrlLikeHosts() throws Exception {
        Path database = tempDirectory.resolve("url.db");
        new SQLiteSegmentRepository(database);
        try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + database);
             Statement statement = connection.createStatement()) {
            statement.execute("""
                    INSERT INTO segment(kind, process_name, site_host, start_ts, end_ts)
                    VALUES ('OBSERVED', 'chrome.exe', 'https://evil.example/path', 0, 1)
                    """);
        }
        assertThrows(StorageException.class, () -> SQLitePrivacyCheck.verify(database));
    }
}
