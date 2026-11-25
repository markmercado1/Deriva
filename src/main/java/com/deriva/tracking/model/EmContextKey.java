package com.deriva.tracking.model;

import java.util.Locale;

/**
 * Identity of an observed context: normalized process plus optional normalized site host.
 *
 * <p>Visible labels never define identity. The host is lower-cased and one leading
 * {@code www.} is removed, but remaining subdomains stay distinct (D3).</p>
 */
public record EmContextKey(String processName, String siteHost) {
    public EmContextKey {
        if (processName == null || processName.isBlank()) throw new IllegalArgumentException("processName");
        processName = processName.trim().toLowerCase(Locale.ROOT);
        if (processName.indexOf('/') >= 0 || processName.indexOf('\\') >= 0)
            throw new IllegalArgumentException("processName must be a file name");
        if (processName.chars().anyMatch(Character::isISOControl))
            throw new IllegalArgumentException("processName");

        if (siteHost != null) {
            siteHost = siteHost.trim().toLowerCase(Locale.ROOT);
            if (siteHost.startsWith("www.")) siteHost = siteHost.substring(4);
            if (siteHost.isEmpty() || siteHost.length() > 253) throw new IllegalArgumentException("siteHost");
            for (String label : siteHost.split("\\.", -1)) {
                if (!label.matches("[a-z0-9](?:[a-z0-9-]{0,61}[a-z0-9])?"))
                    throw new IllegalArgumentException("siteHost");
            }
        }
    }
}
