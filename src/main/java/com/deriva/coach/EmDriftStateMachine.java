package com.deriva.coach;

import com.deriva.analysis.EmChange;
import com.deriva.analysis.EmChangeCounter;
import com.deriva.coach.model.EmCloseReason;
import com.deriva.coach.model.EmDetectionSettings;
import com.deriva.coach.model.EmDriftEpisode;
import com.deriva.coach.model.EmDriftStatus;
import com.deriva.coach.model.EmDriftUpdate;
import com.deriva.privacy.EmObservationKind;
import com.deriva.tracking.model.EmContextKey;
import com.deriva.tracking.model.EmStableTick;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Pure D1/D2 state machine. It opens one episode when the configured number of
 * confirmed changes fits inside the configured window, updates that same episode
 * while it remains open, and closes it as STABLE after the configured stability
 * time in the current context or as UNCONFIRMED on a boundary.
 */
public final class EmDriftStateMachine implements EmDriftDetector {
    private final EmDetectionSettings settings;
    private final EmChangeCounter changeCounter = new EmChangeCounter();
    private final Deque<EmChange> window = new ArrayDeque<>();
    private OpenEpisode open;
    private long nextId = 1;

    public EmDriftStateMachine() {
        this(EmDetectionSettings.defaults());
    }

    public EmDriftStateMachine(EmDetectionSettings settings) {
        this.settings = Objects.requireNonNull(settings, "settings");
    }

    @Override
    public List<EmDriftUpdate> onTick(EmStableTick tick) {
        Objects.requireNonNull(tick, "tick");
        List<EmDriftUpdate> updates = new ArrayList<>();

        if (tick.kind() != EmObservationKind.OBSERVED) {
            if (open != null) {
                updates.add(new EmDriftUpdate(EmDriftUpdate.Type.CLOSED,
                        open.unconfirmed(tick.timestamp(), closeReason(tick.kind()))));
                open = null;
            }
            window.clear();
            changeCounter.onTick(tick);
            return List.copyOf(updates);
        }

        if (open != null) open.lastObservedTs = tick.timestamp();
        Optional<EmChange> change = changeCounter.onTick(tick);

        if (open != null) {
            if (change.isPresent()) {
                open.switchCount++;
                open.contexts.add(change.get().context());
                updates.add(new EmDriftUpdate(EmDriftUpdate.Type.UPDATED, open.openSnapshot()));
            }
            if (!tick.pendingChange()) {
                Duration stable = Duration.between(tick.contextSince(), tick.timestamp());
                if (stable.compareTo(settings.stability()) >= 0) {
                    updates.add(new EmDriftUpdate(EmDriftUpdate.Type.CLOSED,
                            open.stable(tick.timestamp(), tick.contextSince())));
                    open = null;
                    window.clear();
                }
            }
            return List.copyOf(updates);
        }

        if (change.isPresent()) {
            window.addLast(change.get());
            Instant cutoff = tick.timestamp().minus(settings.window());
            while (!window.isEmpty() && !window.getFirst().timestamp().isAfter(cutoff))
                window.removeFirst();

            if (window.size() >= settings.switches()) {
                open = OpenEpisode.from(nextId++, window, tick);
                window.clear();
                updates.add(new EmDriftUpdate(EmDriftUpdate.Type.OPENED, open.openSnapshot()));
            }
        }
        return List.copyOf(updates);
    }

    /** Closes an open episode on process exit; a later restart must not reuse it. */
    public List<EmDriftUpdate> close(Instant at, EmCloseReason reason) {
        Objects.requireNonNull(at, "at");
        Objects.requireNonNull(reason, "reason");
        if (open == null) {
            window.clear();
            return List.of();
        }
        Instant closedAt = open.lastObservedTs.isAfter(at) ? open.lastObservedTs : at;
        EmDriftUpdate update = new EmDriftUpdate(EmDriftUpdate.Type.CLOSED, open.unconfirmed(closedAt, reason));
        open = null;
        window.clear();
        return List.of(update);
    }

    private static EmCloseReason closeReason(EmObservationKind kind) {
        return switch (kind) {
            case IDLE -> EmCloseReason.IDLE;
            case EXCLUDED -> EmCloseReason.EXCLUSION;
            case UNAVAILABLE -> EmCloseReason.UNAVAILABLE;
            case OBSERVED -> throw new IllegalArgumentException("OBSERVED is not a boundary");
        };
    }

    private static final class OpenEpisode {
        private final long id;
        private final Instant startTs;
        private final Instant detectedAt;
        private final int switchCountAtOpen;
        private final EmContextKey firstChangeContext;
        private final Set<EmContextKey> contexts = new LinkedHashSet<>();

        private Instant lastObservedTs;
        private int switchCount;

        private OpenEpisode(long id, Instant startTs, Instant detectedAt, Instant lastObservedTs,
                            int switchCountAtOpen, EmContextKey firstChangeContext) {
            this.id = id;
            this.startTs = startTs;
            this.detectedAt = detectedAt;
            this.lastObservedTs = lastObservedTs;
            this.switchCountAtOpen = switchCountAtOpen;
            this.switchCount = switchCountAtOpen;
            this.firstChangeContext = firstChangeContext;
        }

        static OpenEpisode from(long id, Deque<EmChange> changes, EmStableTick tick) {
            EmChange first = changes.getFirst();
            OpenEpisode episode = new OpenEpisode(id, first.timestamp(), tick.timestamp(),
                    tick.timestamp(), changes.size(), first.context());
            for (EmChange change : changes) episode.contexts.add(change.context());
            return episode;
        }

        EmDriftEpisode openSnapshot() {
            return new EmDriftEpisode(id, startTs, detectedAt, lastObservedTs, null, null,
                    EmDriftStatus.OPEN, null, switchCount, firstChangeContext, null, contexts);
        }

        EmDriftEpisode stable(Instant closedAt, Instant stabilityStartTs) {
            long time = Duration.between(startTs, stabilityStartTs).toMillis();
            return new EmDriftEpisode(id, startTs, detectedAt, closedAt, stabilityStartTs, closedAt,
                    EmDriftStatus.STABLE, null, switchCount, firstChangeContext, time, contexts);
        }

        EmDriftEpisode unconfirmed(Instant closedAt, EmCloseReason reason) {
            return new EmDriftEpisode(id, startTs, detectedAt, lastObservedTs, null, closedAt,
                    EmDriftStatus.UNCONFIRMED, reason, switchCount, firstChangeContext, null, contexts);
        }
    }
}
