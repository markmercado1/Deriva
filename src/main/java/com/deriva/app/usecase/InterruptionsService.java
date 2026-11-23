package com.deriva.app.usecase;

import com.deriva.coach.model.DriftEvent;
import com.deriva.storage.port.DriftRepository;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/** Read and annotate drift episodes; depends only on the repository port. */
public final class InterruptionsService {
    private final DriftRepository repository;
    private final Clock clock;

    public InterruptionsService(DriftRepository repository, Clock clock) {
        this.repository = Objects.requireNonNull(repository, "repository");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    /** Returns episodes overlapping the inclusive local-date range, without duplicates. */
    public List<DriftEvent> forRange(LocalDate fromDay, LocalDate toDay, ZoneId zone) {
        Objects.requireNonNull(fromDay, "fromDay");
        Objects.requireNonNull(toDay, "toDay");
        Objects.requireNonNull(zone, "zone");
        if (fromDay.isAfter(toDay)) throw new IllegalArgumentException("fromDay after toDay");
        Instant from = fromDay.atStartOfDay(zone).toInstant();
        Instant to = toDay.plusDays(1).atStartOfDay(zone).toInstant();
        return repository.between(from, to).stream()
                .sorted(Comparator.comparing(DriftEvent::startTs).reversed())
                .toList();
    }

    public List<DriftEvent> forDay(LocalDate day, ZoneId zone) {
        return forRange(day, day, zone);
    }

    public void annotate(long id, String reason, boolean justified) {
        repository.annotate(id, reason, justified, clock.instant());
    }
}
