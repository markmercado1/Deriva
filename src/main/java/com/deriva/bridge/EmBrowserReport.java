package com.deriva.bridge;

import java.util.Locale;

public record EmBrowserReport(State state, String host, long sequence) {
    public enum State { NORMAL, INTERNAL, UNKNOWN }

    public EmBrowserReport {
        if (state == null || sequence < 1) throw new IllegalArgumentException("Invalid report");
        if (state != State.NORMAL) {
            if (host != null) throw new IllegalArgumentException("Unexpected host");
        } else {
            if (host == null) throw new IllegalArgumentException("Missing host");
            host = host.toLowerCase(Locale.ROOT);
            if (host.startsWith("www.")) host = host.substring(4);
            if (host.isEmpty() || host.length() > 253) throw new IllegalArgumentException("Invalid host");
            for (String label : host.split("\\.", -1)) {
                if (!label.matches("[a-z0-9](?:[a-z0-9-]{0,61}[a-z0-9])?"))
                    throw new IllegalArgumentException("Invalid host");
            }
        }
    }
}
