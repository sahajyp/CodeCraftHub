package com.codecrafthub.exception;

/** Thrown when no course has the requested id (HTTP 404). */
public class CourseNotFoundException extends RuntimeException {
    public CourseNotFoundException(int id) {
        super("Course with ID " + id + " not found");
    }
}
