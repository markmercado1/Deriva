package com.deriva.storage;

/** Explicit storage failure; never hide it behind an empty result. */
public final class EmStorageException extends RuntimeException {
    public EmStorageException(String message, Throwable cause) {
        super(message, cause);
    }

    public EmStorageException(String message) {
        super(message);
    }
}
