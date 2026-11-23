package com.deriva.ui.viewmodel;

public record ReasonOption(String code, String label) {
    @Override
    public String toString() {
        return label;
    }
}
