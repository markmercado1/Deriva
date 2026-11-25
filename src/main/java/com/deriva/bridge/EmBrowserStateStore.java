package com.deriva.bridge;

import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.function.LongSupplier;

/** Diagnostic reports only. No report authorizes capture without native foreground correlation. */
public final class EmBrowserStateStore {
    public record Snapshot(String state, String host, long sequence, long accepted, Instant receivedAt) {}
    private final Clock clock;
    private final LongSupplier nanoTime;
    private String session;
    private EmBrowserReport report;
    private long receivedNanos;
    private Instant receivedAt;
    private long accepted;

    public EmBrowserStateStore(Clock clock, LongSupplier nanoTime) {
        this.clock = clock;
        this.nanoTime = nanoTime;
    }

    public synchronized String beginSession() {
        session = UUID.randomUUID().toString();
        report = null;
        receivedAt = null;
        return session;
    }

    public synchronized boolean accept(String sessionId, EmBrowserReport next) {
        if (session == null || !session.equals(sessionId)
                || (report != null && next.sequence() <= report.sequence())) return false;
        report = next;
        receivedNanos = nanoTime.getAsLong();
        receivedAt = clock.instant();
        accepted++;
        return true;
    }

    public synchronized Snapshot snapshot() {
        long age = nanoTime.getAsLong() - receivedNanos;
        boolean fresh = report != null && age >= 0 && age < 3_000_000_000L;
        return new Snapshot(fresh ? report.state().name() : "STALE",
                fresh ? report.host() : null, report == null ? 0 : report.sequence(), accepted, receivedAt);
    }

    /** Host for one authorized browser/profile, only while the report is fresh and NORMAL. */
    public synchronized Optional<String> freshNormalHost() {
        Snapshot current = snapshot();
        if (!"NORMAL".equals(current.state()) || current.host() == null || current.host().isBlank())
            return Optional.empty();
        return Optional.of(current.host());
    }
}
