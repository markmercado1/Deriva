package com.deriva.privacy;

import java.util.Optional;

/** Supplies the host only while the authorized browser profile has fresh NORMAL state. */
@FunctionalInterface
public interface BrowserStateProvider {
    Optional<String> freshNormalHost();
}
