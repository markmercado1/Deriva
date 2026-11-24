package com.deriva.coach;

import com.deriva.analysis.Change;
import com.deriva.analysis.ChangeCounter;
import com.deriva.coach.model.CloseReason;
import com.deriva.coach.model.DetectionSettings;
import com.deriva.coach.model.DriftEpisode;
import com.deriva.coach.model.DriftStatus;
import com.deriva.coach.model.DriftUpdate;
import com.deriva.privacy.ObservationKind;
import com.deriva.tracking.model.ContextKey;
import com.deriva.tracking.model.StableTick;

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
public final class DriftStateMachine implements DriftDetector {
    private final DetectionSettings settings;
    private final ChangeCounter changeCounter = new ChangeCounter();
    private final Deque<Change> window = new ArrayDeque<>();
    private OpenEpisode open;
    private long nextId = 1;

    public DriftStateMachine() {
        this(DetectionSettings.defaults());
    }

    public DriftStateMachine(DetectionSettings settings) {
        this.settings = Objects.requireNonNull(settings, "settings");
    }

    @Override
    public List<DriftUpdate> onTick(StableTick tick) {
        Objects.requireNonNull(tick, "tick");
        List<DriftUpdate> updates = new ArrayList<>();

        if (tick.kind() != ObservationKind.OBSERVED) {
            if (open != null) {
                updates.add(new DriftUpdate(DriftUpdate.Type.CLOSED,
                        open.unconfirmed(tick.timestamp(), closeReason(tick.kind()))));
                open = null;
            }
            window.clear();
            changeCounter.onTick(tick);
            return List.copyOf(updates);
        }

        if (open != null) open.lastObservedTs = tick.timestamp();
        Optional<Change> change = changeCounter.onTick(tick);

        if (open != null) {
            if (change.isPresent()) {
                open.switchCount++;
                open.contexts.add(change.get().context());
                updates.add(new DriftUpdate(DriftUpdate.Type.UPDATED, open.openSnapshot()));
            }
            if (!tick.pendingChange()) {
                Duration stable = Duration.between(tick.contextSince(), tick.timestamp());
                if (stable.compareTo(settings.stability()) >= 0) {
                    updates.add(new DriftUpdate(DriftUpdate.Type.CLOSED,
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
                updates.add(new DriftUpdate(DriftUpdate.Type.OPENED, open.openSnapshot()));
            }
        }
        return List.copyOf(updates);
    }

    /** Closes an open episode on process exit; a later restart must not reuse it. */
    public List<DriftUpdate> close(Instant at, CloseReason reason) {
        Objects.requireNonNull(at, "at");
        Objects.requireNonNull(reason, "reason");
        if (open == null) {
            window.clear();
            return List.of();
        }
        Instant closedAt = open.lastObservedTs.isAfter(at) ? open.lastObservedTs : at;
        DriftUpdate update = new DriftUpdate(DriftUpdate.Type.CLOSED, open.unconfirmed(closedAt, reason));
        open = null;
        window.clear();
        return List.of(update);
    }

    private static CloseReason closeReason(ObservationKind kind) {
        return switch (kind) {
            case IDLE -> CloseReason.IDLE;
            case EXCLUDED -> CloseReason.EXCLUSION;
            case UNAVAILABLE -> CloseReason.UNAVAILABLE;
            case OBSERVED -> throw new IllegalArgumentException("OBSERVED is not a boundary");
        };
    }

    private static final class OpenEpisode {
        private final long id;
        private final Instant startTs;
        private final Instant detectedAt;
        private final int switchCountAtOpen;
        private final ContextKey firstChangeContext;
        private final Set<ContextKey> contexts = new LinkedHashSet<>();

        private Instant lastObservedTs;
        private int switchCount;

        private OpenEpisode(long id, Instant startTs, Instant detectedAt, Instant lastObservedTs,
                            int switchCountAtOpen, ContextKey firstChangeContext) {
            this.id = id;
            this.startTs = startTs;
            this.detectedAt = detectedAt;
            this.lastObservedTs = lastObservedTs;
            this.switchCountAtOpen = switchCountAtOpen;
            this.switchCount = switchCountAtOpen;
            this.firstChangeContext = firstChangeContext;
        }

        static OpenEpisode from(long id, Deque<Change> changes, StableTick tick) {
            Change first = changes.getFirst();
            OpenEpisode episode = new OpenEpisode(id, first.timestamp(), tick.timestamp(),
                    tick.timestamp(), changes.size(), first.context());
            for (Change change : changes) episode.contexts.add(change.context());
            return episode;
        }

        DriftEpisode openSnapshot() {
            return new DriftEpisode(id, startTs, detectedAt, lastObservedTs, null, null,
                    DriftStatus.OPEN, null, switchCount, firstChangeContext, null, contexts);
        }

        DriftEpisode stable(Instant closedAt, Instant stabilityStartTs) {
            long time = Duration.between(startTs, stabilityStartTs).toMillis();
            return new DriftEpisode(id, startTs, detectedAt, closedAt, stabilityStartTs, closedAt,
                    DriftStatus.STABLE, null, switchCount, firstChangeContext, time, contexts);
        }

        DriftEpisode unconfirmed(Instant closedAt, CloseReason reason) {
            return new DriftEpisode(id, startTs, detectedAt, lastObservedTs, null, closedAt,
                    DriftStatus.UNCONFIRMED, reason, switchCount, firstChangeContext, null, contexts);
        }
    }
}
