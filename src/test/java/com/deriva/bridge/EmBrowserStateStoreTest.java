package com.deriva.bridge;

import org.junit.jupiter.api.Test;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.concurrent.atomic.AtomicLong;
import static org.junit.jupiter.api.Assertions.*;

class EmBrowserStateStoreTest {
    private final AtomicLong now = new AtomicLong();
    private final EmBrowserStateStore store = new EmBrowserStateStore(
            Clock.fixed(Instant.EPOCH, ZoneOffset.UTC), now::get);

    @Test void expiryDoesNotRenewOldHost() {
        String session = store.beginSession();
        assertTrue(store.accept(session, new EmBrowserReport(EmBrowserReport.State.NORMAL, "docs.google.com", 1)));
        now.set(2_999_999_999L); assertEquals("docs.google.com", store.snapshot().host());
        now.incrementAndGet(); assertNull(store.snapshot().host());
        assertEquals("STALE", store.snapshot().state());
    }
    @Test void restartAndOutOfOrderPacketsCannotRestorePreviousSession() {
        String old = store.beginSession();
        assertTrue(store.accept(old, new EmBrowserReport(EmBrowserReport.State.NORMAL, "example.com", 2)));
        assertFalse(store.accept(old, new EmBrowserReport(EmBrowserReport.State.NORMAL, "old.example", 1)));
        assertFalse(store.accept(old, new EmBrowserReport(EmBrowserReport.State.NORMAL, "old.example", 2)));
        String current = store.beginSession();
        assertNull(store.snapshot().host());
        assertFalse(store.accept(old, new EmBrowserReport(EmBrowserReport.State.NORMAL, "old.example", 3)));
        assertTrue(store.accept(current, new EmBrowserReport(EmBrowserReport.State.UNKNOWN, null, 1)));
        assertNull(store.snapshot().host());
    }
    @Test void unknownImmediatelyClearsHost() {
        String id = store.beginSession();
        store.accept(id, new EmBrowserReport(EmBrowserReport.State.NORMAL, "example.com", 1));
        store.accept(id, new EmBrowserReport(EmBrowserReport.State.UNKNOWN, null, 2));
        assertNull(store.snapshot().host());
        assertEquals("UNKNOWN", store.snapshot().state());
    }
    @Test void freshNormalHostOnlyWhileNormalAndFresh() {
        String session = store.beginSession();
        assertTrue(store.freshNormalHost().isEmpty());
        assertTrue(store.accept(session, new EmBrowserReport(EmBrowserReport.State.NORMAL, "chess.com", 1)));
        assertEquals("chess.com", store.freshNormalHost().orElseThrow());

        now.set(3_000_000_000L);
        assertTrue(store.freshNormalHost().isEmpty());

        now.set(0);
        assertTrue(store.accept(session, new EmBrowserReport(EmBrowserReport.State.UNKNOWN, null, 2)));
        assertTrue(store.freshNormalHost().isEmpty());
    }

    @Test void hostValidationRejectsUrlsAndPrivatePayloads() {
        for (String host : new String[]{"https://example.com/path", "a/b", "a:80", "a@b", "a..b", "-bad.com", "a\n"}) {
            assertThrows(IllegalArgumentException.class, () -> new EmBrowserReport(EmBrowserReport.State.NORMAL, host, 1));
        }
        assertThrows(IllegalArgumentException.class, () -> new EmBrowserReport(EmBrowserReport.State.UNKNOWN, "private.example", 1));
        assertEquals("example.com", new EmBrowserReport(EmBrowserReport.State.NORMAL, "WWW.Example.COM", 1).host());
    }
}
