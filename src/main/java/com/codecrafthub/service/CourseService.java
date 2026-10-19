package com.codecrafthub.service;

import com.codecrafthub.exception.CourseNotFoundException;
import com.codecrafthub.exception.InvalidCourseException;
import com.codecrafthub.exception.StorageException;
import com.codecrafthub.model.Course;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Business logic and file storage.
 *
 * Every request loads courses.json, changes the list in memory and, if
 * something changed, writes the whole list back. The methods are
 * synchronized so two requests cannot overwrite each other's changes.
 */
@Service
public class CourseService {

    public static final List<String> VALID_STATUSES = List.of("Not Started", "In Progress", "Completed");

    private static final DateTimeFormatter TIMESTAMP = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    // "uuuu-MM-dd" with STRICT resolving rejects impossible dates such as 2025-02-30
    private static final DateTimeFormatter DATE_ONLY =
            DateTimeFormatter.ofPattern("uuuu-MM-dd").withResolverStyle(ResolverStyle.STRICT);

    private final ObjectMapper objectMapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
    private final Path dataFile;

    public CourseService(@Value("${codecrafthub.data-file:courses.json}") String dataFile) {
        this.dataFile = Path.of(dataFile);
    }

    // ------------------------------------------------------------------ reads

    public synchronized List<Course> findAll() {
        return load();
    }

    public synchronized Course findById(int id) {
        return load().stream()
                .filter(c -> c.getId() == id)
                .findFirst()
                .orElseThrow(() -> new CourseNotFoundException(id));
    }

    /** Courses whose name or description contains the text (ignoring upper/lower case). */
    public synchronized List<Course> search(String text) {
        String needle = text == null ? "" : text.trim().toLowerCase(Locale.ROOT);
        List<Course> matches = new ArrayList<>();
        for (Course c : load()) {
            if (c.getName().toLowerCase(Locale.ROOT).contains(needle)
                    || c.getDescription().toLowerCase(Locale.ROOT).contains(needle)) {
                matches.add(c);
            }
        }
        return matches;
    }

    /** Total number of courses and how many there are in each status. */
    public synchronized Map<String, Object> stats() {
        List<Course> courses = load();
        Map<String, Integer> byStatus = new LinkedHashMap<>();
        VALID_STATUSES.forEach(s -> byStatus.put(s, 0));
        courses.forEach(c -> byStatus.merge(c.getStatus(), 1, Integer::sum));

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("total", courses.size());
        result.put("by_status", byStatus);
        return result;
    }

    // ----------------------------------------------------------------- writes

    public synchronized Course create(Course input) {
        validateForCreate(input);
        List<Course> courses = load();

        Course course = new Course(
                nextId(courses),
                input.getName().trim(),
                input.getDescription().trim(),
                input.getTargetDate().trim(),
                input.getStatus(),
                LocalDateTime.now().format(TIMESTAMP));

        courses.add(course);
        save(courses);
        return course;
    }

    /** Partial update: only the fields present in the request are changed. */
    public synchronized Course update(int id, Course input) {
        List<Course> courses = load();
        Course course = courses.stream()
                .filter(c -> c.getId() == id)
                .findFirst()
                .orElseThrow(() -> new CourseNotFoundException(id));

        if (input.getName() != null) {
            course.setName(requireText(input.getName(), "name"));
        }
        if (input.getDescription() != null) {
            course.setDescription(requireText(input.getDescription(), "description"));
        }
        if (input.getTargetDate() != null) {
            course.setTargetDate(requireDate(input.getTargetDate()));
        }
        if (input.getStatus() != null) {
            course.setStatus(requireStatus(input.getStatus()));
        }

        save(courses);
        return course;
    }

    public synchronized Course delete(int id) {
        List<Course> courses = load();
        Course course = courses.stream()
                .filter(c -> c.getId() == id)
                .findFirst()
                .orElseThrow(() -> new CourseNotFoundException(id));

        courses.remove(course);
        save(courses);
        return course;
    }

    // ------------------------------------------------------------- validation

    private void validateForCreate(Course c) {
        // Missing-field messages come first so the user learns what is absent
        if (isBlank(c.getName()))        throw new InvalidCourseException("Missing required field: name");
        if (isBlank(c.getDescription())) throw new InvalidCourseException("Missing required field: description");
        if (isBlank(c.getTargetDate()))  throw new InvalidCourseException("Missing required field: target_date");
        if (isBlank(c.getStatus()))      throw new InvalidCourseException("Missing required field: status");
        requireDate(c.getTargetDate());
        requireStatus(c.getStatus());
    }

    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    private static String requireText(String value, String field) {
        if (isBlank(value)) throw new InvalidCourseException("Field '" + field + "' cannot be empty");
        return value.trim();
    }

    private static String requireDate(String value) {
        String text = requireText(value, "target_date");
        try {
            LocalDate.parse(text, DATE_ONLY);
        } catch (DateTimeParseException e) {
            throw new InvalidCourseException("target_date must be a real date in YYYY-MM-DD format");
        }
        return text;
    }

    private static String requireStatus(String value) {
        if (!VALID_STATUSES.contains(value)) {
            throw new InvalidCourseException("Status must be one of: " + String.join(", ", VALID_STATUSES));
        }
        return value;
    }

    // ----------------------------------------------------------- file storage

    private int nextId(List<Course> courses) {
        return courses.stream().mapToInt(Course::getId).max().orElse(0) + 1;
    }

    /** Reads courses.json. Creates an empty file first if it does not exist yet. */
    private List<Course> load() {
        try {
            if (Files.notExists(dataFile)) {
                save(new ArrayList<>());
                return new ArrayList<>();
            }
            if (Files.size(dataFile) == 0) {
                return new ArrayList<>(); // an empty file counts as "no courses"
            }
            return new ArrayList<>(objectMapper.readValue(dataFile.toFile(), new TypeReference<List<Course>>() {}));
        } catch (IOException e) {
            // Do NOT silently return an empty list here: the next save would erase the user's data.
            throw new StorageException("Could not read " + dataFile + ". Check that it is valid JSON "
                    + "(a list of courses) and that the app may read it.", e);
        }
    }

    /**
     * Writes courses.json. The data goes to a temporary file first and is then
     * moved over the real file, so a crash mid-write cannot leave half a file behind.
     */
    private void save(List<Course> courses) {
        try {
            Path absolute = dataFile.toAbsolutePath();
            if (absolute.getParent() != null) {
                Files.createDirectories(absolute.getParent());
            }
            Path temp = Files.createTempFile(absolute.getParent(), "courses", ".tmp");
            try {
                objectMapper.writeValue(temp.toFile(), courses);
                Files.move(temp, absolute, StandardCopyOption.REPLACE_EXISTING);
            } finally {
                Files.deleteIfExists(temp);
            }
        } catch (IOException e) {
            throw new StorageException("Could not write " + dataFile + ". Check the folder permissions.", e);
        }
    }
}
