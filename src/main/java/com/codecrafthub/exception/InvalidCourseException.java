package com.codecrafthub.exception;

/** Thrown when a request has a missing or invalid field (HTTP 400). */
public class InvalidCourseException extends RuntimeException {
    public InvalidCourseException(String message) {
        super(message);
    }
}
