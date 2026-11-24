package com.deriva.ui.viewmodel;

import com.deriva.tracking.model.ContextKey;

import java.time.Duration;
import java.util.Map;

/** Shared display names and duration formatting for the read-only views. */
public final class DisplayFormats {
    private static final Map<String, String> APP_NAMES = Map.of(
            "code.exe", "VS Code",
            "winword.exe", "Word",
            "windowsterminal.exe", "Terminal",
            "explorer.exe", "Explorador",
            "chrome.exe", "Chrome",
            "brave.exe", "Brave",
            "chess.exe", "Ajedrez");
    private static final Map<String, String> SITE_NAMES = Map.of(
            "instagram.com", "Instagram",
            "web.whatsapp.com", "WhatsApp Web",
            "docs.google.com", "Google Docs",
            "youtube.com", "YouTube",
            "mail.google.com", "Gmail",
            "chess.com", "Chess.com");

    private DisplayFormats() {}

    public static String context(ContextKey context) {
        if (context.siteHost() != null) return site(context.siteHost());
        return app(context.processName());
    }

    public static String app(String processName) {
        String known = APP_NAMES.get(processName);
        if (known != null) return known;
        return processName.endsWith(".exe")
                ? processName.substring(0, processName.length() - 4)
                : processName;
    }

    public static String site(String host) {
        return SITE_NAMES.getOrDefault(host, host);
    }

    public static String duration(Long millis) {
        if (millis == null) return "—";
        long seconds = Math.max(0, Duration.ofMillis(millis).toSeconds());
        if (seconds < 60) return seconds + " s";
        long minutes = seconds / 60;
        long remaining = seconds % 60;
        if (minutes < 60) return remaining == 0 ? minutes + " min" : minutes + " min " + remaining + " s";
        long hours = minutes / 60;
        long restMinutes = minutes % 60;
        return restMinutes == 0 ? hours + " h" : hours + " h " + restMinutes + " min";
    }
}
