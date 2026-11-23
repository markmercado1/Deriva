package com.deriva.ui.viewmodel;

import java.time.LocalDate;
import java.util.List;

/** Display state for one day of persisted usage. */
public record UsagePage(
        LocalDate date,
        String zoneText,
        String totalText,
        String coverageText,
        String statusText,
        List<UsageRow> apps,
        List<UsageRow> sites,
        boolean hasSegments) {

    public UsagePage {
        apps = List.copyOf(apps);
        sites = List.copyOf(sites);
    }
}
