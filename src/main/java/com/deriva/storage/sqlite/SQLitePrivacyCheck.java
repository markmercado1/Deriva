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

/**
 * Defense in depth: activity identity and goal identity must never contain URL,
 * path, query or title fields, nor URL-like text.
 *
 * <p>The user justification of a goal alert is free text written by the user, local
 * and bounded to 500 characters. It is intentionally not pattern-blocked: it never
 * comes from browsing activity, and rejecting it would create false positives (see
 * A49). Only its column name is audited.</p>
 */
public final class SQLitePrivacyCheck {
    private static final List<String> FORBIDDEN_COLUMNS =
            List.of("url", "uri", "title", "window_title", "query", "path", "content");
    private static final List<String> IDENTITY_TABLES = List.of("segment", "goals", "goal_alerts");

    private SQLitePrivacyCheck() {}

    public static void verify(Path databasePath) {
        try (Connection connection = SQLiteSchema.open(databasePath);
             Statement statement = connection.createStatement()) {
            for (String table : IDENTITY_TABLES) {
                Set<String> columns = columns(statement, table);
                for (String forbidden : FORBIDDEN_COLUMNS) {
                    if (columns.contains(forbidden))
                        throw new StorageException("Forbidden " + table + " column: " + forbidden);
                }
            }

            if (tableExists(statement, "segment") && hasRows(statement, """
                    SELECT COUNT(*) FROM segment
                    WHERE site_host LIKE '%/%' OR site_host LIKE '%?%'
                       OR site_host LIKE '%#%' OR site_host LIKE '%:%'
                    """))
                throw new StorageException("URL-like host found in segment table");

            if (tableExists(statement, "goals")
                    && columns(statement, "goals").containsAll(
                    List.of("process_name", "site_host"))) {
                if (hasRows(statement, """
                        SELECT COUNT(*) FROM goals
                        WHERE instr(process_name, '/') > 0 OR instr(process_name, '\\') > 0
                           OR instr(process_name, '?') > 0 OR instr(process_name, '#') > 0
                           OR instr(process_name, ':') > 0
                           OR instr(site_host, '/') > 0 OR instr(site_host, '?') > 0
                           OR instr(site_host, '#') > 0 OR instr(site_host, ':') > 0
                           OR instr(site_host, ' ') > 0
                        """))
                    throw new StorageException("URL-like or path-like identity found in goals table");
            }

            if (tableExists(statement, "goal_alerts")
                    && columns(statement, "goal_alerts").contains("outcome")
                    && hasRows(statement, """
                    SELECT COUNT(*) FROM goal_alerts
                    WHERE outcome IS NOT NULL AND outcome NOT IN
                        ('STABLE','NO_DRIFT','NEW_DRIFT','STILL_DRIFTING',
                         'NOT_OBSERVED','DISCARDED')
                    """))
                throw new StorageException("Unexpected free text found in goal_alerts.outcome");
        } catch (SQLException e) {
            throw new StorageException("Could not verify SQLite privacy", e);
        }
    }

    private static Set<String> columns(Statement statement, String table) throws SQLException {
        Set<String> columns = new HashSet<>();
        try (ResultSet result = statement.executeQuery("PRAGMA table_info(" + table + ")")) {
            while (result.next()) columns.add(result.getString("name").toLowerCase(Locale.ROOT));
        }
        return columns;
    }

    private static boolean tableExists(Statement statement, String table) throws SQLException {
        return !columns(statement, table).isEmpty();
    }

    private static boolean hasRows(Statement statement, String sql) throws SQLException {
        try (ResultSet result = statement.executeQuery(sql)) {
            return result.next() && result.getLong(1) > 0;
        }
    }
}
