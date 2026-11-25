package com.deriva.goals;

import com.deriva.goals.model.Goal;
import com.deriva.privacy.Observation;
import com.deriva.privacy.ObservationKind;
import com.deriva.tracking.model.ContextKey;

import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Pure rule that keeps a passive goal context observed while there is no input.
 *
 * <p>It receives the already sanitized observations and the passive contexts injected
 * by the application. It knows nothing about SQL, threads, JavaFX or the clock. An
 * idle observation is promoted only when the same foreground sample would be observed
 * without the idle boundary, its context matches an active passive goal exactly, and
 * the idle time has not exceeded the configured maximum. The promotion is bound to the
 * context that was in front when the idle period started; changing windows cuts it
 * until real activity resumes.</p>
 */
public final class PassiveActivity {
    /** Initial product limit measured from the last input. */
    public static final Duration DEFAULT_MAX = Duration.ofMinutes(10);
    private static final Duration MAX_GAP = Duration.ofSeconds(3);

    private final Duration max;
    private Set<ContextKey> contexts;
    private ContextKey promoted;
    private boolean suppressed;
    private Instant lastTimestamp;

    public PassiveActivity(Duration max, Collection<Goal> goals) {
        Objects.requireNonNull(max, "max");
        if (max.isZero() || max.isNegative()) throw new IllegalArgumentException("max");
        Objects.requireNonNull(goals, "goals");
        this.max = max;
        this.contexts = contextsOf(goals);
    }

    /**
     * Applies a new goal list without restarting the capture session. An ongoing
     * promotion survives when its context is still passive; removing or disabling
     * that context cuts it. Editing an unrelated goal therefore changes nothing.
     */
    public void updateGoals(Collection<Goal> goals) {
        Objects.requireNonNull(goals, "goals");
        this.contexts = contextsOf(goals);
        if (promoted != null && !contexts.contains(promoted)) cut();
    }

    public boolean hasContexts() {
        return !contexts.isEmpty();
    }

    private static Set<ContextKey> contextsOf(Collection<Goal> goals) {
        return goals.stream()
                .filter(Goal::active)
                .filter(Goal::passive)
                .map(Goal::context)
                .collect(Collectors.toUnmodifiableSet());
    }

    /**
     * Returns the possible promotion of an idle observation. Every non-idle observation,
     * an exceeded maximum, a non-observed foreground or a non-passive context is
     * returned unchanged, so without the passive flag the previous behaviour is kept.
     */
    public Observation apply(Observation filtered, Observation foreground, Duration idleTime) {
        Objects.requireNonNull(filtered, "filtered");
        Objects.requireNonNull(foreground, "foreground");
        Objects.requireNonNull(idleTime, "idleTime");
        if (idleTime.isNegative()) throw new IllegalArgumentException("idleTime");
        boolean gap = lastTimestamp != null
                && Duration.between(lastTimestamp, filtered.timestamp()).compareTo(MAX_GAP) > 0;
        lastTimestamp = filtered.timestamp();
        if (gap && filtered.kind() == ObservationKind.IDLE) {
            cut();
            return filtered;
        }
        if (filtered.kind() != ObservationKind.IDLE) {
            reset();
            return filtered;
        }
        if (idleTime.compareTo(max) > 0 || foreground.kind() != ObservationKind.OBSERVED) {
            cut();
            return filtered;
        }
        ContextKey context = new ContextKey(foreground.processName(), foreground.siteHost());
        if (!contexts.contains(context)) {
            cut();
            return filtered;
        }
        if (promoted == null) {
            if (suppressed) return filtered;
            promoted = context;
            return foreground;
        }
        if (promoted.equals(context)) return foreground;
        cut();
        return filtered;
    }

    private void cut() {
        promoted = null;
        suppressed = true;
    }

    private void reset() {
        promoted = null;
        suppressed = false;
    }
}
