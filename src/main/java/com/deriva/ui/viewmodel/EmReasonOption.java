package com.deriva.ui.viewmodel;

public record EmReasonOption(String code, String label) {
    @Override
    public String toString() {
        return label;
    }
}
