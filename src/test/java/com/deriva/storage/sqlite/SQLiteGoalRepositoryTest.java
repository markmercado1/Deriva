package com.deriva.storage.sqlite;

import com.deriva.goals.model.Goal;
import com.deriva.storage.StorageException;
import com.deriva.tracking.model.ContextKey;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class SQLiteGoalRepositoryTest {
    private static final Instant T0 = Instant.parse("2026-11-24T09:00:00Z");
    private static final Clock CLOCK = Clock.fixed(T0, ZoneOffset.UTC);
    private static final ContextKey CODING = new ContextKey("code.exe", null);
    private static final ContextKey CHESS = new ContextKey("chrome.exe", "chess.com");

    @TempDir Path tempDirectory;

    private Path database() {
        return tempDirectory.resolve("goals.db");
    }

    private SQLiteGoalRepository repository() {
        return new SQLiteGoalRepository(database(), CLOCK);
    }

    @Test
    void createsFindsAndReopensGoals() {
        Goal created = repository().create(Goal.draft(CODING, 90, null, false));
        assertTrue(created.id() > 0);
        assertEquals(T0, created.createdAt());
        assertEquals(T0, created.updatedAt());
        assertTrue(created.active());

        Goal stored = repository().find(created.id()).orElseThrow();
        assertEquals(CODING, stored.context());
        assertEquals(90, stored.dailyMinutes());
        assertNull(stored.dailyEntries());
        assertFalse(stored.passive());

        assertEquals(stored, repository().find(created.id()).orElseThrow());
    }

    @Test
    void storesSiteGoalWithBothLimitsAndPassiveFlag() {
        Goal created = repository().create(Goal.draft(CHESS, 45, 8, true));
        Goal stored = repository().find(created.id()).orElseThrow();
        assertEquals(CHESS, stored.context());
        assertEquals(45, stored.dailyMinutes());
        assertEquals(8, stored.dailyEntries());
        assertTrue(stored.passive());
    }

    @Test
    void updatesLimitsAndTouchesModifiedTime() {
        Goal created = repository().create(Goal.draft(CODING, 90, 5, false));
        Clock later = Clock.fixed(T0.plusSeconds(60), ZoneOffset.UTC);
        SQLiteGoalRepository laterRepository = new SQLiteGoalRepository(database(), later);

        laterRepository.update(new Goal(created.id(), CODING, 60, null, true, true,
                created.createdAt(), created.updatedAt()));

        Goal stored = laterRepository.find(created.id()).orElseThrow();
        assertEquals(60, stored.dailyMinutes());
        assertNull(stored.dailyEntries());
        assertTrue(stored.passive());
        assertEquals(T0, stored.createdAt());
        assertEquals(T0.plusSeconds(60), stored.updatedAt());
    }

    @Test
    void listsOnlyActiveGoalsOrderedByContext() {
        SQLiteGoalRepository repository = repository();
        repository.create(Goal.draft(CODING, 30, null, false));
        repository.create(Goal.draft(CHESS, null, 4, false));
        repository.create(new Goal(0, new ContextKey("word.exe", null), 10, null, false, false, null, null));

        List<Goal> active = repository.listActive();
        assertEquals(List.of(CHESS, CODING), active.stream().map(Goal::context).toList());

        List<Goal> all = repository.listAll();
        assertEquals(3, all.size());
        assertEquals(List.of(CHESS, CODING, new ContextKey("word.exe", null)),
                all.stream().map(Goal::context).toList());
        assertTrue(all.getLast().active() == false);
    }

    @Test
    void enforcesOneGoalPerContext() {
        SQLiteGoalRepository repository = repository();
        repository.create(Goal.draft(CODING, 30, null, false));
        assertThrows(StorageException.class, () -> repository.create(Goal.draft(CODING, 60, null, false)));

        repository.create(Goal.draft(CHESS, 30, null, false));
        assertThrows(StorageException.class, () -> repository.create(Goal.draft(CHESS, null, 3, false)));

        repository.create(Goal.draft(new ContextKey("chrome.exe", null), 30, null, false));
        repository.create(Goal.draft(new ContextKey("chrome.exe", "youtube.com"), 30, null, false));
        assertEquals(4, repository.listActive().size());
    }

    @Test
    void processNameIsCaseInsensitiveInTheDatabase() throws Exception {
        repository().create(Goal.draft(CODING, 30, null, false));
        try (Connection connection = SQLiteSchema.open(database());
             PreparedStatement statement = connection.prepareStatement("""
                     INSERT INTO goals(process_name, site_host, daily_minutes, created_at, updated_at)
                     VALUES ('CODE.EXE', NULL, 10, 1, 1)
                     """)) {
            assertThrows(SQLException.class, statement::executeUpdate);
        }
    }

    @Test
    void databaseRejectsGoalWithoutLimit() throws Exception {
        repository();
        assertThrows(IllegalArgumentException.class, () -> Goal.draft(CODING, null, null, false));
        try (Connection connection = SQLiteSchema.open(database());
             PreparedStatement statement = connection.prepareStatement("""
                     INSERT INTO goals(process_name, created_at, updated_at) VALUES ('code.exe', 1, 1)
                     """)) {
            assertThrows(SQLException.class, statement::executeUpdate);
        }
    }

    @Test
    void databaseRejectsUrlLikeHostsAndPaths() throws Exception {
        repository();
        for (String host : List.of("example.com/path", "example.com?q=1", "example.com#x",
                "example.com:443", "example .com")) {
            assertThrows(SQLException.class, () -> insertRaw(host, "code.exe"));
        }
        assertThrows(SQLException.class, () -> insertRaw(null, "C:\\code.exe"));
    }

    @Test
    void schemaHasNoUrlOrTitleColumns() throws Exception {
        repository();
        try (Connection connection = SQLiteSchema.open(database());
             Statement statement = connection.createStatement()) {
            for (String table : List.of("goals", "goal_alerts")) {
                Set<String> columns = new HashSet<>();
                try (ResultSet result = statement.executeQuery("PRAGMA table_info(" + table + ")")) {
                    while (result.next()) columns.add(result.getString("name").toLowerCase(Locale.ROOT));
                }
                for (String forbidden : List.of("url", "uri", "title", "window_title", "query", "path", "content"))
                    assertFalse(columns.contains(forbidden), table + "." + forbidden);
            }
        }
    }

    @Test
    void deletesGoalAndReportsUnknownIds() {
        SQLiteGoalRepository repository = repository();
        Goal goal = repository.create(Goal.draft(CODING, 30, null, false));

        assertTrue(repository.delete(goal.id()));
        assertTrue(repository.find(goal.id()).isEmpty());
        assertFalse(repository.delete(goal.id()));
        assertThrows(StorageException.class, () -> repository.update(new Goal(999, CODING, 10, null,
                false, true, T0, T0)));
    }

    private void insertRaw(String host, String process) throws SQLException {
        try (Connection connection = SQLiteSchema.open(database());
             PreparedStatement statement = connection.prepareStatement("""
                     INSERT INTO goals(process_name, site_host, daily_minutes, created_at, updated_at)
                     VALUES (?, ?, 10, 1, 1)
                     """)) {
            statement.setString(1, process);
            statement.setString(2, host);
            statement.executeUpdate();
        }
    }
}
