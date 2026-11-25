package com.deriva.bridge.diagnostics;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.*;

class EmBridgeReportLogTest {
    @TempDir Path tempDirectory;

    @Test
    void writesTimestampSequenceStateAndHostPresenceOnly() throws Exception {
        Path log = tempDirectory.resolve("reports.log");
        Clock clock = Clock.fixed(Instant.parse("2026-11-22T10:00:00Z"), ZoneOffset.UTC);

        try (EmBridgeReportLog writer = new EmBridgeReportLog(log, clock)) {
            writer.append(1, "NORMAL", true);
            writer.append(2, "UNKNOWN", false);
        }

        String content = Files.readString(log);
        assertTrue(content.contains("# start 2026-11-22T10:00:00Z"));
        assertTrue(content.contains("timestamp\tseq\tstate\thostPresent"));
        assertTrue(content.contains("2026-11-22T10:00:00Z\t1\tNORMAL\tyes"));
        assertTrue(content.contains("2026-11-22T10:00:00Z\t2\tUNKNOWN\tno"));
        assertFalse(content.contains("docs.google.com"));
    }
}
