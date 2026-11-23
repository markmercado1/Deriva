package com.deriva.coach.model;

/** One detector output: an episode was opened, updated or closed. */
public record DriftUpdate(Type type, DriftEpisode episode) {
    public enum Type { OPENED, UPDATED, CLOSED }

    public DriftUpdate {
        if (type == null || episode == null) throw new IllegalArgumentException("type/episode");
        if (type == Type.OPENED && episode.status() != DriftStatus.OPEN)
            throw new IllegalArgumentException("OPENED without OPEN episode");
        if (type == Type.CLOSED && episode.status() == DriftStatus.OPEN)
            throw new IllegalArgumentException("CLOSED with OPEN episode");
    }
}
