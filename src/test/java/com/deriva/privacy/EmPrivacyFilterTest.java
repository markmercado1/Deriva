package com.deriva.privacy;

import com.deriva.sensor.EmActivitySample;
import com.deriva.sensor.EmFakeIdleDetector;
import com.deriva.sensor.EmFakeSensor;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class EmPrivacyFilterTest {
    private static final Instant T = Instant.parse("2026-11-22T10:00:00Z");
    private static final long OWN_PID = 42;

    private final EmPrivacyFilter filter = new EmPrivacyFilter(OWN_PID);

    private static EmActivitySample sample(String process, long pid, boolean available) {
        return available
                ? new EmActivitySample(T, process, pid, 100, false, true)
                : new EmActivitySample(T, null, 0, 0, false, false);
    }

    @Test
    void unavailableNeverCarriesIdentity() {
        EmObservation observation = filter.filter(sample(null, 0, false), Duration.ZERO);
        assertEquals(EmObservationKind.UNAVAILABLE, observation.kind());
        assertNull(observation.processName());
        assertNull(observation.siteHost());
    }

    @Test
    void ownProcessIsExcluded() {
        EmObservation observation = filter.filter(sample("java.exe", OWN_PID, true), Duration.ZERO);
        assertEquals(EmObservationKind.EXCLUDED, observation.kind());
        assertNull(observation.processName());
    }

    @Test
    void idleBeyondThresholdWinsOverProcess() {
        assertEquals(EmObservationKind.IDLE,
                filter.filter(sample("notepad.exe", 7, true), Duration.ofSeconds(121)).kind());
        assertEquals(EmObservationKind.OBSERVED,
                filter.filter(sample("notepad.exe", 7, true), Duration.ofSeconds(120)).kind());
    }

    @Test
    void customIdleThresholdUsesStrictlyGreaterComparison() {
        EmPrivacyFilter custom = new EmPrivacyFilter(OWN_PID, Duration.ofSeconds(300));
        assertEquals(EmObservationKind.OBSERVED,
                custom.filter(sample("notepad.exe", 7, true), Duration.ofSeconds(300)).kind());
        assertEquals(EmObservationKind.IDLE,
                custom.filter(sample("notepad.exe", 7, true), Duration.ofSeconds(301)).kind());
    }

    @Test
    void windowsLockSurfaceIsUnavailableNotObserved() {
        for (String process : List.of("LockApp.exe", "LOGONUI.EXE")) {
            EmObservation observation = filter.filter(sample(process, 7, true), Duration.ZERO);
            assertEquals(EmObservationKind.UNAVAILABLE, observation.kind(), process);
            assertNull(observation.processName(), process);
            assertNull(observation.siteHost(), process);
        }
        assertEquals(EmObservationKind.UNAVAILABLE,
                filter.filter(sample("LockApp.exe", 7, true), Duration.ofSeconds(300)).kind());
    }

    @Test
    void browsersStayExcludedUntilD4AuthorizesSites() {
        for (String browser : List.of("chrome.exe", "MSEDGE.EXE", "brave.exe", "firefox.exe",
                "opera.exe", "VIVALDI.EXE")) {
            EmObservation observation = filter.filter(sample(browser, 7, true), Duration.ZERO);
            assertEquals(EmObservationKind.EXCLUDED, observation.kind(), browser);
            assertNull(observation.processName(), browser);
        }
    }

    @Test
    void observedProcessIsNormalized() {
        EmObservation observation = filter.filter(sample("NotePad.EXE", 7, true), Duration.ZERO);
        assertEquals(EmObservationKind.OBSERVED, observation.kind());
        assertEquals("notepad.exe", observation.processName());
        assertNull(observation.siteHost());
    }

    @Test
    void chromeIsObservedOnlyWithFreshNormalHost() {
        EmPrivacyFilter browserFilter = new EmPrivacyFilter(OWN_PID, Duration.ofSeconds(120),
                () -> Optional.of("chess.com"));
        EmObservation observation = browserFilter.filter(sample("chrome.exe", 7, true), Duration.ZERO);

        assertEquals(EmObservationKind.OBSERVED, observation.kind());
        assertEquals("chrome.exe", observation.processName());
        assertEquals("chess.com", observation.siteHost());
    }

    @Test
    void chromeWithoutFreshHostIsExcluded() {
        EmPrivacyFilter browserFilter = new EmPrivacyFilter(OWN_PID, Duration.ofSeconds(120), Optional::empty);
        EmObservation observation = browserFilter.filter(sample("chrome.exe", 7, true), Duration.ZERO);

        assertEquals(EmObservationKind.EXCLUDED, observation.kind());
        assertNull(observation.siteHost());
    }

    @Test
    void braveIsObservedOnlyWithFreshNormalHost() {
        EmPrivacyFilter browserFilter = new EmPrivacyFilter(OWN_PID, Duration.ofSeconds(120),
                () -> Optional.of("chess.com"));
        EmObservation observation = browserFilter.filter(sample("BRAVE.EXE", 7, true), Duration.ZERO);

        assertEquals(EmObservationKind.OBSERVED, observation.kind());
        assertEquals("brave.exe", observation.processName());
        assertEquals("chess.com", observation.siteHost());
    }

    @Test
    void restrictedBrowserSetKeepsBraveExcludedWithFreshHost() {
        EmPrivacyFilter chromeOnly = new EmPrivacyFilter(OWN_PID, Duration.ofSeconds(120),
                () -> Optional.of("example.com"), Set.of("chrome.exe"));

        assertEquals(EmObservationKind.EXCLUDED,
                chromeOnly.filter(sample("brave.exe", 7, true), Duration.ZERO).kind());
        assertEquals(EmObservationKind.OBSERVED,
                chromeOnly.filter(sample("chrome.exe", 7, true), Duration.ZERO).kind());
    }

    @Test
    void braveWithoutFreshHostIsExcluded() {
        EmPrivacyFilter browserFilter = new EmPrivacyFilter(OWN_PID, Duration.ofSeconds(120), Optional::empty);
        EmObservation observation = browserFilter.filter(sample("brave.exe", 7, true), Duration.ZERO);

        assertEquals(EmObservationKind.EXCLUDED, observation.kind());
        assertNull(observation.siteHost());
    }

    @Test
    void otherBrowsersStayExcludedEvenWithHost() {
        EmPrivacyFilter browserFilter = new EmPrivacyFilter(OWN_PID, Duration.ofSeconds(120),
                () -> Optional.of("chess.com"));
        EmObservation observation = browserFilter.filter(sample("msedge.exe", 7, true), Duration.ZERO);

        assertEquals(EmObservationKind.EXCLUDED, observation.kind());
        assertNull(observation.siteHost());
    }

    @Test
    void idleChromeDoesNotCarryHost() {
        EmPrivacyFilter browserFilter = new EmPrivacyFilter(OWN_PID, Duration.ofSeconds(120),
                () -> Optional.of("chess.com"));
        EmObservation observation = browserFilter.filter(sample("chrome.exe", 7, true),
                Duration.ofSeconds(121));

        assertEquals(EmObservationKind.IDLE, observation.kind());
        assertNull(observation.siteHost());
    }

    @Test
    void browserHostIsNotAttachedToNonBrowser() {
        EmPrivacyFilter browserFilter = new EmPrivacyFilter(OWN_PID, Duration.ofSeconds(120),
                () -> Optional.of("chess.com"));
        EmObservation observation = browserFilter.filter(sample("notepad.exe", 7, true), Duration.ZERO);

        assertEquals(EmObservationKind.OBSERVED, observation.kind());
        assertNull(observation.siteHost());
    }

    @Test
    void deterministicSequenceFiltersBeforeTracking() {
        EmFakeSensor sensor = new EmFakeSensor(
                sample("chrome.exe", 7, true),
                sample("notepad.exe", 8, true),
                sample("notepad.exe", 8, true),
                sample(null, 0, false),
                sample("java.exe", OWN_PID, true));
        EmFakeIdleDetector idle = new EmFakeIdleDetector(
                Duration.ZERO, Duration.ofSeconds(5), Duration.ofSeconds(121), Duration.ZERO, Duration.ZERO);

        List<EmObservation> observations = new ArrayList<>();
        while (sensor.remaining() > 0) observations.add(filter.filter(sensor.sample(), idle.idleTime()));

        assertEquals(List.of(EmObservationKind.EXCLUDED, EmObservationKind.OBSERVED, EmObservationKind.IDLE,
                        EmObservationKind.UNAVAILABLE, EmObservationKind.EXCLUDED),
                observations.stream().map(EmObservation::kind).toList());
        assertEquals(Arrays.asList(null, "notepad.exe", null, null, null),
                observations.stream().map(EmObservation::processName).toList());
    }

    @Test
    void nonObservedObservationCannotCarryIdentity() {
        assertThrows(IllegalArgumentException.class,
                () -> new EmObservation(T, EmObservationKind.EXCLUDED, "chrome.exe", null));
        assertThrows(IllegalArgumentException.class,
                () -> new EmObservation(T, EmObservationKind.IDLE, null, "example.com"));
        assertThrows(IllegalArgumentException.class,
                () -> new EmObservation(T, EmObservationKind.OBSERVED, null, null));
    }
}
