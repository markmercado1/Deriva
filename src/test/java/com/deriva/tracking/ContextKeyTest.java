package com.deriva.tracking;

import com.deriva.tracking.model.ContextKey;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ContextKeyTest {
    @Test
    void normalizesProcessAndHost() {
        ContextKey key = new ContextKey(" Chrome.EXE ", "WWW.Example.COM");
        assertEquals("chrome.exe", key.processName());
        assertEquals("example.com", key.siteHost());
        assertEquals(new ContextKey("chrome.exe", "example.com"), key);
    }

    @Test
    void keepsSubdomainsAndProcessesDistinct() {
        assertNotEquals(new ContextKey("chrome.exe", "docs.google.com"),
                new ContextKey("chrome.exe", "mail.google.com"));
        assertNotEquals(new ContextKey("chrome.exe", "mail.google.com"),
                new ContextKey("msedge.exe", "mail.google.com"));
    }

    @Test
    void allowsApplicationsWithoutSite() {
        ContextKey key = new ContextKey("notepad.exe", null);
        assertNull(key.siteHost());
    }

    @Test
    void rejectsInvalidIdentity() {
        assertThrows(IllegalArgumentException.class, () -> new ContextKey(null, null));
        assertThrows(IllegalArgumentException.class, () -> new ContextKey("  ", null));
        assertThrows(IllegalArgumentException.class, () -> new ContextKey("C:\\Chrome\\chrome.exe", null));
        assertThrows(IllegalArgumentException.class, () -> new ContextKey("chrome.exe", "https://example.com/path"));
        assertThrows(IllegalArgumentException.class, () -> new ContextKey("chrome.exe", "a..b"));
        assertThrows(IllegalArgumentException.class, () -> new ContextKey("chrome.exe", "-bad.com"));
        assertThrows(IllegalArgumentException.class, () -> new ContextKey("chrome.exe", "  "));
    }
}
