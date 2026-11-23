package com.deriva.ui.viewmodel;

import java.util.List;

public record InterruptionsPage(List<InterruptionRow> rows, int totalEpisodes, int openEpisodes) {
    public InterruptionsPage {
        rows = List.copyOf(rows);
    }
}
