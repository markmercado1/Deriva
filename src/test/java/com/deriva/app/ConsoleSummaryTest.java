package com.deriva.app;

import com.deriva.privacy.ObservationKind;
import com.deriva.tracking.model.ContextKey;
import com.deriva.tracking.model.Segment;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ConsoleSummaryTest {
    private static final Instant BASE = Instant.parse("2026-11-22T10:00:00Z");

    private static Instant at(int second) {
        return BASE.plusSeconds(second);
    }

    private static Segment observed(String process, String host, int start, int end) {
        return new Segment(0, ObservationKind.OBSERVED, new ContextKey(process, host),
                null, null, at(start), at(end));
    }

    private static Segment boundary(ObservationKind kind, int start, int end) {
        return new Segment(0, kind, null, null, null, at(start), at(end));
    }

    @Test
    void separatesObservedAppSiteIdleExcludedUnavailableAndUncovered() {
        List<Segment> segments = List.of(
                observed("notepad.exe", null, 0, 10),
                observed("chrome.exe", "chess.com", 10, 30),
                boundary(ObservationKind.IDLE, 30, 35),
                boundary(ObservationKind.EXCLUDED, 35, 40),
                boundary(ObservationKind.UNAVAILABLE, 40, 45));

        ConsoleSummary.Summary summary = ConsoleSummary.of(segments, at(0), at(50));
        assertEquals(Duration.ofSeconds(10), summary.observedByApp().get("notepad.exe"));
        assertEquals(Duration.ofSeconds(20), summary.observedByApp().get("chrome.exe"));
        assertEquals(Duration.ofSeconds(20), summary.observedBySite().get("chess.com"));
        assertEquals(Duration.ofSeconds(30), summary.observedTotal());
        assertEquals(Duration.ofSeconds(5), summary.idle());
        assertEquals(Duration.ofSeconds(5), summary.excluded());
        assertEquals(Duration.ofSeconds(5), summary.unavailable());
        assertEquals(Duration.ofSeconds(5), summary.uncovered());
    }

    @Test
    void clipsSegmentsToTheRequestedWindow() {
        ConsoleSummary.Summary summary = ConsoleSummary.of(
                List.of(observed("chrome.exe", "example.com", 0, 100)), at(10), at(20));
        assertEquals(Duration.ofSeconds(10), summary.observedByApp().get("chrome.exe"));
        assertEquals(Duration.ofSeconds(10), summary.observedBySite().get("example.com"));
        assertEquals(Duration.ZERO, summary.uncovered());
    }

    @Test
    void uncoveredStartsAtFirstSegmentNotMidnight() {
        ConsoleSummary.Summary summary = ConsoleSummary.of(
                List.of(boundary(ObservationKind.EXCLUDED, 10, 20)), at(0), at(30));
        assertEquals(Duration.ofSeconds(10), summary.uncovered());
    }

    @Test
    void emptyWindowIsAllUncovered() {
        ConsoleSummary.Summary summary = ConsoleSummary.of(List.of(), at(0), at(10));
        assertTrue(summary.observedByApp().isEmpty());
        assertEquals(Duration.ofSeconds(10), summary.uncovered());
    }

    @Test
    void formatContainsSeparateCategories() {
        ConsoleSummary.Summary summary = ConsoleSummary.of(List.of(
                observed("chrome.exe", "chess.com", 0, 10),
                boundary(ObservationKind.IDLE, 10, 15)), at(0), at(20));
        String text = ConsoleSummary.format(summary);
        assertTrue(text.contains("Observado por app"));
        assertTrue(text.contains("Sitios"));
        assertTrue(text.contains("IDLE"));
        assertTrue(text.contains("EXCLUDED"));
        assertTrue(text.contains("Sin datos (UNAVAILABLE)"));
        assertTrue(text.contains("Sin registro"));
    }
}
