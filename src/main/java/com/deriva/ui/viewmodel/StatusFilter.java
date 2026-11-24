package com.deriva.ui.viewmodel;

import com.deriva.coach.model.DriftStatus;

public enum StatusFilter {
    ALL("Todas las estabilidades"),
    OPEN("En curso"),
    STABLE("Estables"),
    UNCONFIRMED("No comprobadas");

    private final String label;

    StatusFilter(String label) {
        this.label = label;
    }

    public boolean matches(DriftStatus status) {
        return this == ALL || name().equals(status.name());
    }

    @Override
    public String toString() {
        return label;
    }
}
