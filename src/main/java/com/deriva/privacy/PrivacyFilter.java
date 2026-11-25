package com.deriva.privacy;

import com.deriva.sensor.ActivitySample;

import java.time.Duration;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Applies privacy rules before any tracking context is built or persisted.
 *
 * <p>Chrome and Brave may be observed only while the authorized profile supplies a
 * fresh NORMAL host. Every other browser remains excluded. Idle observations are
 * evaluated before browser identity, so an idle browser never carries a host. The
 * Windows lock/logon surface is unavailable, never an observed application.</p>
 */
public final class PrivacyFilter {
    private static final Duration DEFAULT_IDLE_THRESHOLD = Duration.ofSeconds(120);
    private static final Set<String> AUTHORIZED_BROWSERS = Set.of("chrome.exe", "brave.exe");
    private static final Set<String> BROWSER_PROCESSES =
            Set.of("chrome.exe", "msedge.exe", "brave.exe", "firefox.exe", "opera.exe", "vivaldi.exe");
    private static final Set<String> SESSION_LOCK_PROCESSES =
            Set.of("lockapp.exe", "logonui.exe");

    private final long ownPid;
    private final Duration idleThreshold;
    private final BrowserStateProvider browserStateProvider;
    private final Set<String> authorizedBrowsers;

    public PrivacyFilter(long ownPid) {
        this(ownPid, DEFAULT_IDLE_THRESHOLD, Optional::empty);
    }

    public PrivacyFilter(long ownPid, Duration idleThreshold) {
        this(ownPid, idleThreshold, Optional::empty);
    }

    public PrivacyFilter(long ownPid, Duration idleThreshold, BrowserStateProvider browserStateProvider) {
        this(ownPid, idleThreshold, browserStateProvider, AUTHORIZED_BROWSERS);
    }

    public PrivacyFilter(long ownPid, Duration idleThreshold, BrowserStateProvider browserStateProvider,
                         Set<String> authorizedBrowsers) {
        if (ownPid <= 0) throw new IllegalArgumentException("ownPid");
        if (idleThreshold == null || idleThreshold.isNegative()) throw new IllegalArgumentException("idleThreshold");
        this.ownPid = ownPid;
        this.idleThreshold = idleThreshold;
        this.browserStateProvider = Objects.requireNonNull(browserStateProvider, "browserStateProvider");
        this.authorizedBrowsers = Set.copyOf(Objects.requireNonNull(authorizedBrowsers, "authorizedBrowsers"));
    }

    /** True for every process name in the shared browser list, including excluded ones. */
    public static boolean isBrowserProcess(String processName) {
        if (processName == null) return false;
        return BROWSER_PROCESSES.contains(processName.toLowerCase(Locale.ROOT));
    }

    public Observation filter(ActivitySample sample, Duration idleTime) {
        Objects.requireNonNull(sample, "sample");
        Objects.requireNonNull(idleTime, "idleTime");
        if (idleTime.isNegative()) throw new IllegalArgumentException("idleTime");

        if (!sample.available())
            return new Observation(sample.timestamp(), ObservationKind.UNAVAILABLE, null, null);
        if (sample.pid() == ownPid)
            return new Observation(sample.timestamp(), ObservationKind.EXCLUDED, null, null);

        String processName = sample.processName().toLowerCase(Locale.ROOT);
        if (SESSION_LOCK_PROCESSES.contains(processName))
            return new Observation(sample.timestamp(), ObservationKind.UNAVAILABLE, null, null);
        if (idleTime.compareTo(idleThreshold) > 0)
            return new Observation(sample.timestamp(), ObservationKind.IDLE, null, null);

        if (authorizedBrowsers.contains(processName)) {
            Optional<String> host = Objects.requireNonNull(
                    browserStateProvider.freshNormalHost(), "freshNormalHost");
            if (host.isPresent() && !host.get().isBlank())
                return new Observation(sample.timestamp(), ObservationKind.OBSERVED, processName, host.get());
            return new Observation(sample.timestamp(), ObservationKind.EXCLUDED, null, null);
        }
        if (BROWSER_PROCESSES.contains(processName))
            return new Observation(sample.timestamp(), ObservationKind.EXCLUDED, null, null);

        return new Observation(sample.timestamp(), ObservationKind.OBSERVED, processName, null);
    }
}
