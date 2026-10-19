package com.codecrafthub.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Turns exceptions into the same JSON error shape everywhere:
 * {"success": false, "error": "message"}.
 * This keeps the controller free of try/catch blocks.
 */
@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(CourseNotFoundException.class)
    public ResponseEntity<Map<String, Object>> notFound(CourseNotFoundException e) {
        return error(e.getMessage(), HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(InvalidCourseException.class)
    public ResponseEntity<Map<String, Object>> invalid(InvalidCourseException e) {
        return error(e.getMessage(), HttpStatus.BAD_REQUEST);
    }

    /** The request body is not valid JSON, or a field has the wrong type. */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> unreadable(HttpMessageNotReadableException e) {
        return error("Request body must be valid JSON with text values for name, description, target_date and status",
                HttpStatus.BAD_REQUEST);
    }

    /** For example GET /api/courses/abc, where the id is not a number. */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Map<String, Object>> badPathValue(MethodArgumentTypeMismatchException e) {
        return error("Course ID must be a whole number", HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<Map<String, Object>> badMethod(HttpRequestMethodNotSupportedException e) {
        return error("Method not allowed: " + e.getMethod(), HttpStatus.METHOD_NOT_ALLOWED);
    }

    /** POST/PUT sent without a JSON Content-Type header. */
    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<Map<String, Object>> badContentType(HttpMediaTypeNotSupportedException e) {
        return error("Content-Type must be application/json", HttpStatus.UNSUPPORTED_MEDIA_TYPE);
    }

    /** A URL that does not exist, for example /api/course (typo). */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<Map<String, Object>> noSuchUrl(NoResourceFoundException e) {
        return error("Endpoint not found: /" + e.getResourcePath(), HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(StorageException.class)
    public ResponseEntity<Map<String, Object>> storage(StorageException e) {
        return error(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
    }

    /** Safety net for anything unexpected. */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> unexpected(Exception e) {
        return error("Unexpected server error: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
    }

    private ResponseEntity<Map<String, Object>> error(String message, HttpStatus status) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("success", false);
        body.put("error", message);
        return ResponseEntity.status(status).body(body);
    }
}
