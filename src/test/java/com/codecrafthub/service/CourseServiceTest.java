package com.codecrafthub.service;

import com.codecrafthub.exception.CourseNotFoundException;
import com.codecrafthub.exception.InvalidCourseException;
import com.codecrafthub.exception.StorageException;
import com.codecrafthub.model.Course;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests the service and the JSON file storage with a fresh temporary
 * folder for every test, so the real courses.json is never touched.
 */
class CourseServiceTest {

    @TempDir
    Path dir;

    private CourseService newService() {
        return new CourseService(dir.resolve("courses.json").toString());
    }

    private static Course course(String name, String status) {
        return new Course(null, name, "Learn " + name, "2026-12-31", status, null);
    }

    // ------------------------------------------------------------ create

    @Test
    void createAssignsIdsStartingAtOneAndATimestamp() {
        CourseService service = newService();
        Course first = service.create(course("Python Basics", "Not Started"));
        Course second = service.create(course("Java Basics", "In Progress"));

        assertEquals(1, first.getId().intValue());
        assertEquals(2, second.getId().intValue());
        assertTrue(first.getCreatedAt().matches("\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}"));
    }

    @Test
    void createIgnoresIdAndCreatedAtSentByTheClient() {
        Course input = new Course(99, "SQL", "Learn SQL", "2026-01-01", "Completed", "1999-01-01 00:00:00");
        Course saved = newService().create(input);

        assertEquals(1, saved.getId().intValue());
        assertFalse(saved.getCreatedAt().startsWith("1999"));
    }

    @Test
    void createTrimsText() {
        Course saved = newService().create(new Course(null, "  Git  ", " Learn Git ", " 2026-05-05 ", "Not Started", null));
        assertEquals("Git", saved.getName());
        assertEquals("Learn Git", saved.getDescription());
        assertEquals("2026-05-05", saved.getTargetDate());
    }

    @Test
    void createMakesTheFileAutomaticallyAndDataSurvivesARestart() {
        Path file = dir.resolve("courses.json");
        assertFalse(Files.exists(file));

        newService().create(course("Docker", "Not Started"));

        assertTrue(Files.exists(file));
        List<Course> reloaded = newService().findAll(); // a brand-new service reads the same file
        assertEquals(1, reloaded.size());
        assertEquals("Docker", reloaded.get(0).getName());
    }

    @Test
    void fileUsesSnakeCaseFieldNames() throws IOException {
        newService().create(course("Docker", "Not Started"));
        String json = Files.readString(dir.resolve("courses.json"));

        assertTrue(json.contains("\"target_date\""));
        assertTrue(json.contains("\"created_at\""));
        assertFalse(json.contains("targetDate"));
    }

    @Test
    void createRejectsEachMissingField() {
        CourseService service = newService();
        assertEquals("Missing required field: name",
                assertThrows(InvalidCourseException.class,
                        () -> service.create(new Course(null, null, "d", "2026-01-01", "Completed", null))).getMessage());
        assertEquals("Missing required field: description",
                assertThrows(InvalidCourseException.class,
                        () -> service.create(new Course(null, "n", "  ", "2026-01-01", "Completed", null))).getMessage());
        assertEquals("Missing required field: target_date",
                assertThrows(InvalidCourseException.class,
                        () -> service.create(new Course(null, "n", "d", null, "Completed", null))).getMessage());
        assertEquals("Missing required field: status",
                assertThrows(InvalidCourseException.class,
                        () -> service.create(new Course(null, "n", "d", "2026-01-01", "", null))).getMessage());
        assertTrue(service.findAll().isEmpty()); // nothing was saved
    }

    @Test
    void createRejectsInvalidStatusIncludingWrongCase() {
        CourseService service = newService();
        InvalidCourseException e = assertThrows(InvalidCourseException.class,
                () -> service.create(course("X", "in progress")));
        assertEquals("Status must be one of: Not Started, In Progress, Completed", e.getMessage());
        assertThrows(InvalidCourseException.class, () -> service.create(course("X", "Done")));
    }

    @Test
    void createRejectsBadDates() {
        CourseService service = newService();
        for (String bad : new String[] {"31/12/2025", "2025-13-01", "2025-02-30", "tomorrow", "2025-1-5"}) {
            assertThrows(InvalidCourseException.class,
                    () -> service.create(new Course(null, "n", "d", bad, "Completed", null)), bad);
        }
    }

    // -------------------------------------------------------------- read

    @Test
    void findByIdReturnsTheCourseOrThrowsNotFound() {
        CourseService service = newService();
        service.create(course("A", "Not Started"));

        assertEquals("A", service.findById(1).getName());
        CourseNotFoundException e = assertThrows(CourseNotFoundException.class, () -> service.findById(7));
        assertEquals("Course with ID 7 not found", e.getMessage());
    }

    @Test
    void searchMatchesNameAndDescriptionIgnoringCase() {
        CourseService service = newService();
        service.create(new Course(null, "Python Basics", "Fundamentals", "2026-01-01", "Not Started", null));
        service.create(new Course(null, "Cooking", "Learn PYTHON-style loops", "2026-01-01", "Not Started", null));
        service.create(new Course(null, "Java", "Objects", "2026-01-01", "Not Started", null));

        assertEquals(2, service.search("python").size());
        assertEquals(1, service.search(" JAVA ").size());
        assertEquals(0, service.search("rust").size());
        assertEquals(3, service.search("").size());
    }

    @Test
    void statsCountsEveryStatusEvenWhenZero() {
        CourseService service = newService();
        service.create(course("A", "Not Started"));
        service.create(course("B", "Not Started"));
        service.create(course("C", "Completed"));

        Map<String, Object> stats = service.stats();
        assertEquals(3, ((Integer) stats.get("total")).intValue());
        @SuppressWarnings("unchecked")
        Map<String, Integer> by = (Map<String, Integer>) stats.get("by_status");
        assertEquals(2, by.get("Not Started").intValue());
        assertEquals(0, by.get("In Progress").intValue());
        assertEquals(1, by.get("Completed").intValue());
    }

    // ------------------------------------------------------------ update

    @Test
    void updateChangesOnlyTheFieldsSent() {
        CourseService service = newService();
        Course created = service.create(course("Python", "Not Started"));

        Course patch = new Course();
        patch.setStatus("In Progress");
        Course updated = service.update(1, patch);

        assertEquals("In Progress", updated.getStatus());
        assertEquals("Python", updated.getName());
        assertEquals(created.getCreatedAt(), updated.getCreatedAt());
        assertEquals("In Progress", newService().findById(1).getStatus()); // persisted
    }

    @Test
    void updateValidatesWhatItIsGiven() {
        CourseService service = newService();
        service.create(course("Python", "Not Started"));

        Course badStatus = new Course();
        badStatus.setStatus("Nope");
        assertThrows(InvalidCourseException.class, () -> service.update(1, badStatus));

        Course blankName = new Course();
        blankName.setName("   ");
        assertThrows(InvalidCourseException.class, () -> service.update(1, blankName));

        Course badDate = new Course();
        badDate.setTargetDate("2026-02-31");
        assertThrows(InvalidCourseException.class, () -> service.update(1, badDate));

        assertEquals("Not Started", service.findById(1).getStatus()); // unchanged
    }

    @Test
    void updateUnknownIdIsNotFound() {
        assertThrows(CourseNotFoundException.class, () -> newService().update(5, new Course()));
    }

    // ------------------------------------------------------------ delete

    @Test
    void deleteRemovesTheCourseAndReturnsIt() {
        CourseService service = newService();
        service.create(course("A", "Not Started"));
        service.create(course("B", "Not Started"));

        Course deleted = service.delete(1);

        assertEquals("A", deleted.getName());
        assertEquals(1, service.findAll().size());
        assertThrows(CourseNotFoundException.class, () -> service.findById(1));
        assertThrows(CourseNotFoundException.class, () -> service.delete(1));
    }

    // ---------------------------------------------------- file problems

    @Test
    void corruptFileIsReportedAndNeverOverwritten() throws IOException {
        Path file = dir.resolve("courses.json");
        Files.writeString(file, "{ this is not json");
        CourseService service = newService();

        assertThrows(StorageException.class, service::findAll);
        assertThrows(StorageException.class, () -> service.create(course("A", "Not Started")));
        assertEquals("{ this is not json", Files.readString(file)); // data was not wiped
    }

    @Test
    void emptyFileCountsAsNoCourses() throws IOException {
        Files.writeString(dir.resolve("courses.json"), "");
        CourseService service = newService();

        assertTrue(service.findAll().isEmpty());
        assertEquals(1, service.create(course("A", "Not Started")).getId().intValue());
    }

    @Test
    void unwritableLocationGivesAStorageException() throws IOException {
        Path blocker = dir.resolve("not-a-folder");
        Files.writeString(blocker, "x");                       // a FILE where a folder is needed
        CourseService service = new CourseService(blocker.resolve("courses.json").toString());

        assertThrows(StorageException.class, () -> service.create(course("A", "Not Started")));
    }
}
