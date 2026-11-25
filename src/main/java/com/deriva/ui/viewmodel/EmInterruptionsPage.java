package com.deriva.ui.viewmodel;

import java.util.List;

public record EmInterruptionsPage(List<EmInterruptionRow> rows, int totalEpisodes, int openEpisodes) {
    public EmInterruptionsPage {
        rows = List.copyOf(rows);
    }
}
