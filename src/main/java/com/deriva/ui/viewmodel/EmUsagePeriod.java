package com.deriva.ui.viewmodel;

import java.time.LocalDate;
import java.time.DayOfWeek;
import java.time.temporal.TemporalAdjusters;

public enum EmUsagePeriod {
    DAY("Día"), WEEK("Semana"), MONTH("Mes"), YEAR("Año"), CUSTOM("Personalizado");
    private final String label;
    EmUsagePeriod(String label) { this.label = label; }
    @Override public String toString() { return label; }
    public LocalDate start(LocalDate anchor) {
        return switch (this) {
            case WEEK -> anchor.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
            case MONTH -> anchor.withDayOfMonth(1);
            case YEAR -> anchor.withDayOfYear(1);
            default -> anchor;
        };
    }
    public LocalDate end(LocalDate anchor) {
        LocalDate start = start(anchor);
        return switch (this) {
            case WEEK -> start.plusDays(6);
            case MONTH -> start.plusMonths(1).minusDays(1);
            case YEAR -> start.plusYears(1).minusDays(1);
            default -> anchor;
        };
    }
}
