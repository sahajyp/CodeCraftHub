package com.codecrafthub.controller;

import com.codecrafthub.model.Course;
import com.codecrafthub.service.CourseService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * REST endpoints. Each method only translates HTTP to a service call and
 * wraps the result in a small JSON envelope: {"success": true, ...}.
 * Errors are turned into JSON by ApiExceptionHandler.
 */
@RestController
@RequestMapping("/api/courses")
public class CourseController {

    private final CourseService courseService;

    public CourseController(CourseService courseService) {
        this.courseService = courseService;
    }

    /** GET /api/courses : all courses */
    @GetMapping
    public Map<String, Object> getAllCourses() {
        List<Course> courses = courseService.findAll();
        return body("count", courses.size(), "courses", courses);
    }

    /** GET /api/courses/stats : totals (bonus). A fixed path wins over /{id}. */
    @GetMapping("/stats")
    public Map<String, Object> getStats() {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("success", true);
        response.putAll(courseService.stats());
        return response;
    }

    /** GET /api/courses/search?q=text : search name and description (bonus) */
    @GetMapping("/search")
    public Map<String, Object> search(@RequestParam(name = "q", defaultValue = "") String q) {
        List<Course> courses = courseService.search(q);
        return body("count", courses.size(), "courses", courses);
    }

    /** GET /api/courses/{id} : one course */
    @GetMapping("/{id}")
    public Map<String, Object> getCourse(@PathVariable Integer id) {
        return body("course", courseService.findById(id));
    }

    /** POST /api/courses : add a course (201 Created) */
    @PostMapping
    public ResponseEntity<Map<String, Object>> addCourse(@RequestBody Course course) {
        Course saved = courseService.create(course);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(body("message", "Course added successfully", "course", saved));
    }

    /** PUT /api/courses/{id} : change some or all fields */
    @PutMapping("/{id}")
    public Map<String, Object> updateCourse(@PathVariable Integer id, @RequestBody Course course) {
        Course updated = courseService.update(id, course);
        return body("message", "Course updated successfully", "course", updated);
    }

    /** DELETE /api/courses/{id} : remove a course */
    @DeleteMapping("/{id}")
    public Map<String, Object> deleteCourse(@PathVariable Integer id) {
        Course deleted = courseService.delete(id);
        return body("message", "Course deleted successfully", "deleted_course", deleted);
    }

    /** Builds {"success": true, key1: value1, key2: value2, ...} keeping the order. */
    private Map<String, Object> body(Object... keysAndValues) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("success", true);
        for (int i = 0; i < keysAndValues.length; i += 2) {
            response.put((String) keysAndValues[i], keysAndValues[i + 1]);
        }
        return response;
    }
}
