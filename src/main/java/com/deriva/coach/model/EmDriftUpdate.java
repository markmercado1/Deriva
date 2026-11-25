package com.deriva.coach.model;

/** One detector output: an episode was opened, updated or closed. */
public record EmDriftUpdate(Type type, EmDriftEpisode episode) {
    public enum Type { OPENED, UPDATED, CLOSED }

    public EmDriftUpdate {
        if (type == null || episode == null) throw new IllegalArgumentException("type/episode");
        if (type == Type.OPENED && episode.status() != EmDriftStatus.OPEN)
            throw new IllegalArgumentException("OPENED without OPEN episode");
        if (type == Type.CLOSED && episode.status() == EmDriftStatus.OPEN)
            throw new IllegalArgumentException("CLOSED with OPEN episode");
    }
}
