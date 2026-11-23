package com.deriva.bridge.diagnostics;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Clock;
import java.util.Objects;

/**
 * Temporary diagnostic log for the pilot. It records only timestamp, sequence,
 * state and whether a host was present; it never records the host itself.
 */
public final class BridgeReportLog implements AutoCloseable {
    private static final String HEADER = "timestamp\tseq\tstate\thostPresent";

    private final Clock clock;
    private final BufferedWriter writer;

    public BridgeReportLog(Path path, Clock clock) {
        Objects.requireNonNull(path, "path");
        this.clock = Objects.requireNonNull(clock, "clock");
        try {
            Path absolute = path.toAbsolutePath();
            Path parent = absolute.getParent();
            if (parent != null) Files.createDirectories(parent);
            boolean needsHeader = !Files.exists(absolute) || Files.size(absolute) == 0;
            this.writer = Files.newBufferedWriter(absolute, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND, StandardOpenOption.WRITE);
            writer.write("# start " + clock.instant());
            writer.newLine();
            if (needsHeader) {
                writer.write(HEADER);
                writer.newLine();
            }
            writer.flush();
        } catch (IOException e) {
            throw new UncheckedIOException("Could not open bridge report log", e);
        }
    }

    public synchronized void append(long sequence, String state, boolean hostPresent) {
        if (sequence < 1 || state == null || state.isBlank())
            throw new IllegalArgumentException("Invalid diagnostic report");
        try {
            writer.write(clock.instant() + "\t" + sequence + "\t" + state + "\t" + (hostPresent ? "yes" : "no"));
            writer.newLine();
            writer.flush();
        } catch (IOException e) {
            throw new UncheckedIOException("Could not write bridge report log", e);
        }
    }

    @Override
    public synchronized void close() {
        try {
            writer.close();
        } catch (IOException e) {
            throw new UncheckedIOException("Could not close bridge report log", e);
        }
    }
}
