package com.deriva.ui.viewmodel;

import java.time.LocalDate;
import java.util.List;

/** Display state for one day of persisted usage. */
public record EmUsagePage(
        LocalDate date,
        String zoneText,
        String totalText,
        String coverageText,
        String statusText,
        List<EmUsageRow> apps,
        List<EmUsageRow> sites,
        boolean hasSegments) {

    public EmUsagePage {
        apps = List.copyOf(apps);
        sites = List.copyOf(sites);
    }
}
