package com.deriva.storage;

/** Explicit storage failure; never hide it behind an empty result. */
public final class StorageException extends RuntimeException {
    public StorageException(String message, Throwable cause) {
        super(message, cause);
    }

    public StorageException(String message) {
        super(message);
    }
}
