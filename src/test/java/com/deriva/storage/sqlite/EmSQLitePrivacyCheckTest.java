package com.deriva.storage.sqlite;

import com.deriva.storage.EmStorageException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.*;

class EmSQLitePrivacyCheckTest {
    @TempDir Path tempDirectory;

    @Test
    void acceptsTheCurrentSchemaWithGoalsAndAlerts() {
        Path database = tempDirectory.resolve("ok.db");
        new EmSQLiteSegmentRepository(database);
        new EmSQLiteGoalRepository(database, java.time.Clock.systemUTC());
        new EmSQLiteGoalAlertRepository(database);
        assertDoesNotThrow(() -> EmSQLitePrivacyCheck.verify(database));
    }

    @Test
    void rejectsAForbiddenTitleColumn() throws Exception {
        Path database = tempDirectory.resolve("bad.db");
        try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + database);
             Statement statement = connection.createStatement()) {
            statement.execute("CREATE TABLE segment (site_host TEXT, window_title TEXT)");
        }
        assertThrows(EmStorageException.class, () -> EmSQLitePrivacyCheck.verify(database));
    }

    @Test
    void rejectsUrlLikeHosts() throws Exception {
        Path database = tempDirectory.resolve("url.db");
        new EmSQLiteSegmentRepository(database);
        try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + database);
             Statement statement = connection.createStatement()) {
            statement.execute("""
                    INSERT INTO segment(kind, process_name, site_host, start_ts, end_ts)
                    VALUES ('OBSERVED', 'chrome.exe', 'https://evil.example/path', 0, 1)
                    """);
        }
        assertThrows(EmStorageException.class, () -> EmSQLitePrivacyCheck.verify(database));
    }

    @Test
    void rejectsForbiddenColumnsInGoalsAndGoalAlerts() throws Exception {
        Path goals = tempDirectory.resolve("goals-column.db");
        try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + goals);
             Statement statement = connection.createStatement()) {
            statement.execute("CREATE TABLE goals (process_name TEXT, url TEXT)");
        }
        assertThrows(EmStorageException.class, () -> EmSQLitePrivacyCheck.verify(goals));

        Path alerts = tempDirectory.resolve("alerts-column.db");
        try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + alerts);
             Statement statement = connection.createStatement()) {
            statement.execute("CREATE TABLE goals (process_name TEXT, site_host TEXT)");
            statement.execute("CREATE TABLE goal_alerts (goal_id INTEGER, title TEXT)");
        }
        assertThrows(EmStorageException.class, () -> EmSQLitePrivacyCheck.verify(alerts));
    }

    @Test
    void rejectsUrlLikeOrPathLikeGoalIdentity() throws Exception {
        Path url = tempDirectory.resolve("goal-url.db");
        try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + url);
             Statement statement = connection.createStatement()) {
            statement.execute("CREATE TABLE goals (process_name TEXT, site_host TEXT)");
            statement.execute("INSERT INTO goals VALUES ('https://evil.example', NULL)");
        }
        assertThrows(EmStorageException.class, () -> EmSQLitePrivacyCheck.verify(url));

        Path path = tempDirectory.resolve("goal-path.db");
        try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + path);
             Statement statement = connection.createStatement()) {
            statement.execute("CREATE TABLE goals (process_name TEXT, site_host TEXT)");
            statement.execute("INSERT INTO goals VALUES ('C:\\tools\\code.exe', NULL)");
        }
        assertThrows(EmStorageException.class, () -> EmSQLitePrivacyCheck.verify(path));

        Path host = tempDirectory.resolve("goal-host.db");
        try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + host);
             Statement statement = connection.createStatement()) {
            statement.execute("CREATE TABLE goals (process_name TEXT, site_host TEXT)");
            statement.execute("INSERT INTO goals VALUES ('chrome.exe', 'chess.com/play?x=1')");
        }
        assertThrows(EmStorageException.class, () -> EmSQLitePrivacyCheck.verify(host));
    }

    @Test
    void rejectsFreeTextInTheOutcomeColumn() throws Exception {
        Path database = tempDirectory.resolve("outcome.db");
        try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + database);
             Statement statement = connection.createStatement()) {
            statement.execute("CREATE TABLE goals (process_name TEXT, site_host TEXT)");
            statement.execute("CREATE TABLE goal_alerts (goal_id INTEGER, outcome TEXT)");
            statement.execute("INSERT INTO goals VALUES ('code.exe', NULL)");
            statement.execute("INSERT INTO goal_alerts VALUES (1, 'https://example.com/title')");
        }
        assertThrows(EmStorageException.class, () -> EmSQLitePrivacyCheck.verify(database));
    }

    @Test
    void acceptsFreeUserJustificationTextEvenIfItLooksLikeUrlOrPath() throws Exception {
        Path database = tempDirectory.resolve("justification.db");
        try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + database);
             Statement statement = connection.createStatement()) {
            statement.execute("CREATE TABLE goals (process_name TEXT, site_host TEXT)");
            statement.execute("CREATE TABLE goal_alerts (goal_id INTEGER, justification TEXT)");
            statement.execute("INSERT INTO goals VALUES ('code.exe', NULL)");
            statement.execute("""
                    INSERT INTO goal_alerts VALUES (1, 'Pegué https://example.com/a?b y C:\\notas')
                    """);
        }
        assertDoesNotThrow(() -> EmSQLitePrivacyCheck.verify(database));
    }
}
