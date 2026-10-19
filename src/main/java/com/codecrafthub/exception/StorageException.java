package com.codecrafthub.exception;

/** Thrown when courses.json cannot be read or written (HTTP 500). */
public class StorageException extends RuntimeException {
    public StorageException(String message, Throwable cause) {
        super(message, cause);
    }
}
