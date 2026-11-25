package com.deriva.tracking;

import com.deriva.tracking.model.EmContextKey;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class EmContextKeyTest {
    @Test
    void normalizesProcessAndHost() {
        EmContextKey key = new EmContextKey(" Chrome.EXE ", "WWW.Example.COM");
        assertEquals("chrome.exe", key.processName());
        assertEquals("example.com", key.siteHost());
        assertEquals(new EmContextKey("chrome.exe", "example.com"), key);
    }

    @Test
    void keepsSubdomainsAndProcessesDistinct() {
        assertNotEquals(new EmContextKey("chrome.exe", "docs.google.com"),
                new EmContextKey("chrome.exe", "mail.google.com"));
        assertNotEquals(new EmContextKey("chrome.exe", "mail.google.com"),
                new EmContextKey("msedge.exe", "mail.google.com"));
    }

    @Test
    void allowsApplicationsWithoutSite() {
        EmContextKey key = new EmContextKey("notepad.exe", null);
        assertNull(key.siteHost());
    }

    @Test
    void rejectsInvalidIdentity() {
        assertThrows(IllegalArgumentException.class, () -> new EmContextKey(null, null));
        assertThrows(IllegalArgumentException.class, () -> new EmContextKey("  ", null));
        assertThrows(IllegalArgumentException.class, () -> new EmContextKey("C:\\Chrome\\chrome.exe", null));
        assertThrows(IllegalArgumentException.class, () -> new EmContextKey("chrome.exe", "https://example.com/path"));
        assertThrows(IllegalArgumentException.class, () -> new EmContextKey("chrome.exe", "a..b"));
        assertThrows(IllegalArgumentException.class, () -> new EmContextKey("chrome.exe", "-bad.com"));
        assertThrows(IllegalArgumentException.class, () -> new EmContextKey("chrome.exe", "  "));
    }
}
