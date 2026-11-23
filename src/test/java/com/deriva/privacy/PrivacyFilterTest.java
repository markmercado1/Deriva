package com.deriva.privacy;

import com.deriva.sensor.ActivitySample;
import com.deriva.sensor.FakeIdleDetector;
import com.deriva.sensor.FakeSensor;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class PrivacyFilterTest {
    private static final Instant T = Instant.parse("2026-11-22T10:00:00Z");
    private static final long OWN_PID = 42;

    private final PrivacyFilter filter = new PrivacyFilter(OWN_PID);

    private static ActivitySample sample(String process, long pid, boolean available) {
        return available
                ? new ActivitySample(T, process, pid, 100, false, true)
                : new ActivitySample(T, null, 0, 0, false, false);
    }

    @Test
    void unavailableNeverCarriesIdentity() {
        Observation observation = filter.filter(sample(null, 0, false), Duration.ZERO);
        assertEquals(ObservationKind.UNAVAILABLE, observation.kind());
        assertNull(observation.processName());
        assertNull(observation.siteHost());
    }

    @Test
    void ownProcessIsExcluded() {
        Observation observation = filter.filter(sample("java.exe", OWN_PID, true), Duration.ZERO);
        assertEquals(ObservationKind.EXCLUDED, observation.kind());
        assertNull(observation.processName());
    }

    @Test
    void idleBeyondThresholdWinsOverProcess() {
        assertEquals(ObservationKind.IDLE,
                filter.filter(sample("notepad.exe", 7, true), Duration.ofSeconds(121)).kind());
        assertEquals(ObservationKind.OBSERVED,
                filter.filter(sample("notepad.exe", 7, true), Duration.ofSeconds(120)).kind());
    }

    @Test
    void windowsLockSurfaceIsUnavailableNotObserved() {
        for (String process : List.of("LockApp.exe", "LOGONUI.EXE")) {
            Observation observation = filter.filter(sample(process, 7, true), Duration.ZERO);
            assertEquals(ObservationKind.UNAVAILABLE, observation.kind(), process);
            assertNull(observation.processName(), process);
            assertNull(observation.siteHost(), process);
        }
        assertEquals(ObservationKind.UNAVAILABLE,
                filter.filter(sample("LockApp.exe", 7, true), Duration.ofSeconds(300)).kind());
    }

    @Test
    void browsersStayExcludedUntilD4AuthorizesSites() {
        for (String browser : List.of("chrome.exe", "MSEDGE.EXE", "brave.exe", "firefox.exe",
                "opera.exe", "VIVALDI.EXE")) {
            Observation observation = filter.filter(sample(browser, 7, true), Duration.ZERO);
            assertEquals(ObservationKind.EXCLUDED, observation.kind(), browser);
            assertNull(observation.processName(), browser);
        }
    }

    @Test
    void observedProcessIsNormalized() {
        Observation observation = filter.filter(sample("NotePad.EXE", 7, true), Duration.ZERO);
        assertEquals(ObservationKind.OBSERVED, observation.kind());
        assertEquals("notepad.exe", observation.processName());
        assertNull(observation.siteHost());
    }

    @Test
    void chromeIsObservedOnlyWithFreshNormalHost() {
        PrivacyFilter browserFilter = new PrivacyFilter(OWN_PID, Duration.ofSeconds(120),
                () -> Optional.of("chess.com"));
        Observation observation = browserFilter.filter(sample("chrome.exe", 7, true), Duration.ZERO);

        assertEquals(ObservationKind.OBSERVED, observation.kind());
        assertEquals("chrome.exe", observation.processName());
        assertEquals("chess.com", observation.siteHost());
    }

    @Test
    void chromeWithoutFreshHostIsExcluded() {
        PrivacyFilter browserFilter = new PrivacyFilter(OWN_PID, Duration.ofSeconds(120), Optional::empty);
        Observation observation = browserFilter.filter(sample("chrome.exe", 7, true), Duration.ZERO);

        assertEquals(ObservationKind.EXCLUDED, observation.kind());
        assertNull(observation.siteHost());
    }

    @Test
    void braveIsObservedOnlyWithFreshNormalHost() {
        PrivacyFilter browserFilter = new PrivacyFilter(OWN_PID, Duration.ofSeconds(120),
                () -> Optional.of("chess.com"));
        Observation observation = browserFilter.filter(sample("BRAVE.EXE", 7, true), Duration.ZERO);

        assertEquals(ObservationKind.OBSERVED, observation.kind());
        assertEquals("brave.exe", observation.processName());
        assertEquals("chess.com", observation.siteHost());
    }

    @Test
    void restrictedBrowserSetKeepsBraveExcludedWithFreshHost() {
        PrivacyFilter chromeOnly = new PrivacyFilter(OWN_PID, Duration.ofSeconds(120),
                () -> Optional.of("example.com"), Set.of("chrome.exe"));

        assertEquals(ObservationKind.EXCLUDED,
                chromeOnly.filter(sample("brave.exe", 7, true), Duration.ZERO).kind());
        assertEquals(ObservationKind.OBSERVED,
                chromeOnly.filter(sample("chrome.exe", 7, true), Duration.ZERO).kind());
    }

    @Test
    void braveWithoutFreshHostIsExcluded() {
        PrivacyFilter browserFilter = new PrivacyFilter(OWN_PID, Duration.ofSeconds(120), Optional::empty);
        Observation observation = browserFilter.filter(sample("brave.exe", 7, true), Duration.ZERO);

        assertEquals(ObservationKind.EXCLUDED, observation.kind());
        assertNull(observation.siteHost());
    }

    @Test
    void otherBrowsersStayExcludedEvenWithHost() {
        PrivacyFilter browserFilter = new PrivacyFilter(OWN_PID, Duration.ofSeconds(120),
                () -> Optional.of("chess.com"));
        Observation observation = browserFilter.filter(sample("msedge.exe", 7, true), Duration.ZERO);

        assertEquals(ObservationKind.EXCLUDED, observation.kind());
        assertNull(observation.siteHost());
    }

    @Test
    void idleChromeDoesNotCarryHost() {
        PrivacyFilter browserFilter = new PrivacyFilter(OWN_PID, Duration.ofSeconds(120),
                () -> Optional.of("chess.com"));
        Observation observation = browserFilter.filter(sample("chrome.exe", 7, true),
                Duration.ofSeconds(121));

        assertEquals(ObservationKind.IDLE, observation.kind());
        assertNull(observation.siteHost());
    }

    @Test
    void browserHostIsNotAttachedToNonBrowser() {
        PrivacyFilter browserFilter = new PrivacyFilter(OWN_PID, Duration.ofSeconds(120),
                () -> Optional.of("chess.com"));
        Observation observation = browserFilter.filter(sample("notepad.exe", 7, true), Duration.ZERO);

        assertEquals(ObservationKind.OBSERVED, observation.kind());
        assertNull(observation.siteHost());
    }

    @Test
    void deterministicSequenceFiltersBeforeTracking() {
        FakeSensor sensor = new FakeSensor(
                sample("chrome.exe", 7, true),
                sample("notepad.exe", 8, true),
                sample("notepad.exe", 8, true),
                sample(null, 0, false),
                sample("java.exe", OWN_PID, true));
        FakeIdleDetector idle = new FakeIdleDetector(
                Duration.ZERO, Duration.ofSeconds(5), Duration.ofSeconds(121), Duration.ZERO, Duration.ZERO);

        List<Observation> observations = new ArrayList<>();
        while (sensor.remaining() > 0) observations.add(filter.filter(sensor.sample(), idle.idleTime()));

        assertEquals(List.of(ObservationKind.EXCLUDED, ObservationKind.OBSERVED, ObservationKind.IDLE,
                        ObservationKind.UNAVAILABLE, ObservationKind.EXCLUDED),
                observations.stream().map(Observation::kind).toList());
        assertEquals(Arrays.asList(null, "notepad.exe", null, null, null),
                observations.stream().map(Observation::processName).toList());
    }

    @Test
    void nonObservedObservationCannotCarryIdentity() {
        assertThrows(IllegalArgumentException.class,
                () -> new Observation(T, ObservationKind.EXCLUDED, "chrome.exe", null));
        assertThrows(IllegalArgumentException.class,
                () -> new Observation(T, ObservationKind.IDLE, null, "example.com"));
        assertThrows(IllegalArgumentException.class,
                () -> new Observation(T, ObservationKind.OBSERVED, null, null));
    }
}
