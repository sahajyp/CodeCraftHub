# CodeCraftHub

CodeCraftHub is a simple personalized learning platform where developers track the courses they want to learn. It has two parts in one folder:

- **Backend**: a REST API built with Java and Spring Boot. Courses are stored in a plain JSON file (`courses.json`), so there is no database to install.
- **Dashboard**: a single HTML file (`index.html`, vanilla HTML, CSS and JavaScript) that lets you add, edit and remove courses through the API.

## Features

- Full CRUD for courses: create, list, read one, update, delete
- JSON file storage that is created automatically on first use
- Each course has a name, description, target completion date and status (`Not Started`, `In Progress` or `Completed`)
- Input validation with clear error messages (missing fields, bad status, impossible dates, unreadable JSON, file errors)
- Partial updates: send only the fields you want to change
- Bonus endpoints: course statistics and search
- CORS enabled so the dashboard can call the API from another port (for example Live Server)
- Responsive dashboard with loading spinners, success and error messages, an edit dialog and a remove confirmation
- Unit tests for the storage and validation logic

## Project structure

```
CodeCraftHub/
├── pom.xml                         # Maven build and dependencies
├── index.html                      # Course dashboard (open with Live Server)
├── README.md
├── courses.json                    # Data file, created automatically (git-ignored)
└── src/
    ├── main/
    │   ├── java/com/codecrafthub/
    │   │   ├── CodeCraftHubApplication.java    # Main class
    │   │   ├── controller/CourseController.java # REST endpoints
    │   │   ├── model/Course.java                # Course data model
    │   │   ├── service/CourseService.java       # Business logic and JSON file storage
    │   │   ├── exception/                       # Error types and the JSON error handler
    │   │   └── config/CorsConfig.java           # Lets the dashboard call the API
    │   └── resources/application.properties     # Port, data file, CORS settings
    └── test/java/com/codecrafthub/service/CourseServiceTest.java
```

## Installation

You need:

| Software | Version |
| --- | --- |
| Java Development Kit (JDK) | 17 or newer (17 and 21 are the LTS versions) |
| Apache Maven | 3.6.3 or newer (3.8+ recommended) |

Check them with:

```bash
java -version
mvn -version
```

Then get the project:

```bash
git clone https://github.com/YOUR-USERNAME/codecrafthub.git
cd codecrafthub
```

## Running the application

Build the project (this also runs the tests):

```bash
mvn clean install
```

Start the API:

```bash
mvn spring-boot:run
```

You should see:

```
CodeCraftHub API is starting...
Data will be stored in: courses.json
API will be available at: http://localhost:8080
```

Or run the packaged JAR instead:

```bash
mvn clean package
java -jar target/codecrafthub-1.0.0.jar
```

The API is now at `http://localhost:8080`. `courses.json` is created in the folder you started the app from.

### Running the dashboard

1. Keep the backend running.
2. Open `index.html` with **Live Server** (right-click the file in VS Code and choose "Open with Live Server"). You can also double-click the file to open it in a browser.
3. The dashboard talks to `http://localhost:8080/api/courses`. To point it at a different backend, open it with `?api=`, for example `index.html?api=http://localhost:5000`.

In a cloud IDE or lab environment, use the port the environment shows for Live Server, as described in the lab instructions.

## API documentation

Base URL: `http://localhost:8080/api/courses`

A course in JSON:

```json
{
  "id": 1,
  "name": "Python Basics",
  "description": "Learn Python fundamentals",
  "target_date": "2025-12-31",
  "status": "Not Started",
  "created_at": "2025-11-04 10:30:00"
}
```

`id` and `created_at` are set by the server. `target_date` must be a real date in `YYYY-MM-DD` format. `status` must be exactly `Not Started`, `In Progress` or `Completed`.

Successful responses look like `{"success": true, ...}`. Errors look like `{"success": false, "error": "message"}`.

| Method | Path | Purpose | Success |
| --- | --- | --- | --- |
| POST | `/api/courses` | Add a course | 201 |
| GET | `/api/courses` | List all courses | 200 |
| GET | `/api/courses/{id}` | Get one course | 200 |
| PUT | `/api/courses/{id}` | Update a course | 200 |
| DELETE | `/api/courses/{id}` | Delete a course | 200 |
| GET | `/api/courses/stats` | Totals and counts by status | 200 |
| GET | `/api/courses/search?q=text` | Search name and description | 200 |

### 1. Add a course

```bash
curl -X POST http://localhost:8080/api/courses \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Python Basics",
    "description": "Learn Python fundamentals",
    "target_date": "2025-12-31",
    "status": "Not Started"
  }'
```

Response (201):

```json
{
  "success": true,
  "message": "Course added successfully",
  "course": {
    "id": 1,
    "name": "Python Basics",
    "description": "Learn Python fundamentals",
    "target_date": "2025-12-31",
    "status": "Not Started",
    "created_at": "2025-11-04 10:30:00"
  }
}
```

### 2. Get all courses

```bash
curl http://localhost:8080/api/courses
```

```json
{ "success": true, "count": 1, "courses": [ { "id": 1, "name": "Python Basics", "...": "..." } ] }
```

### 3. Get one course

```bash
curl http://localhost:8080/api/courses/1
```

```json
{ "success": true, "course": { "id": 1, "name": "Python Basics", "...": "..." } }
```

### 4. Update a course

All fields are optional. Only the fields you send change.

```bash
curl -X PUT http://localhost:8080/api/courses/1 \
  -H "Content-Type: application/json" \
  -d '{"status": "In Progress"}'
```

```json
{ "success": true, "message": "Course updated successfully", "course": { "id": 1, "status": "In Progress", "...": "..." } }
```

### 5. Delete a course

```bash
curl -X DELETE http://localhost:8080/api/courses/1
```

```json
{ "success": true, "message": "Course deleted successfully", "deleted_course": { "id": 1, "...": "..." } }
```

### 6. Statistics (bonus)

```bash
curl http://localhost:8080/api/courses/stats
```

```json
{ "success": true, "total": 3, "by_status": { "Not Started": 1, "In Progress": 1, "Completed": 1 } }
```

### 7. Search (bonus)

```bash
curl "http://localhost:8080/api/courses/search?q=python"
```

### Error responses

| Situation | Status | Example message |
| --- | --- | --- |
| Missing required field | 400 | `Missing required field: description` |
| Invalid status | 400 | `Status must be one of: Not Started, In Progress, Completed` |
| Bad or impossible date | 400 | `target_date must be a real date in YYYY-MM-DD format` |
| Body is not valid JSON | 400 | `Request body must be valid JSON ...` |
| Course ID is not a number | 400 | `Course ID must be a whole number` |
| Course not found | 404 | `Course with ID 99 not found` |
| Missing `Content-Type: application/json` | 415 | `Content-Type must be application/json` |
| `courses.json` cannot be read or written | 500 | `Could not read courses.json. ...` |

**Windows PowerShell:** `curl` is an alias for another command there. Use `curl.exe` instead, and write JSON bodies with escaped quotes or use a here-string:

```powershell
curl.exe -X POST http://localhost:8080/api/courses -H "Content-Type: application/json" -d '{\"name\":\"Python Basics\",\"description\":\"Learn Python fundamentals\",\"target_date\":\"2025-12-31\",\"status\":\"Not Started\"}'
```

## Testing

Run the unit tests:

```bash
mvn test
```

They cover id and timestamp generation, validation of every field, partial updates, delete, search, statistics, automatic file creation, persistence between restarts, the snake_case JSON format, and file problems (a corrupt `courses.json` is reported and never overwritten). Each test uses a temporary folder, so your real `courses.json` is untouched.

To test the running API by hand, run the curl commands above in a second terminal and check that:

- every endpoint returns the response shown
- the data persists in `courses.json`
- the error cases return the status codes in the table above

## Configuration

Settings live in `src/main/resources/application.properties` and can also be passed on the command line:

| Property | Default | Meaning |
| --- | --- | --- |
| `server.port` | `8080` | Port of the API |
| `codecrafthub.data-file` | `courses.json` | Where courses are stored |
| `codecrafthub.cors.allowed-origins` | `*` | Origins allowed to call the API from a browser |

Example: `mvn spring-boot:run -Dspring-boot.run.arguments=--server.port=9090`

If you change the port, open the dashboard with `?api=http://localhost:9090`.

## Troubleshooting

**"JAVA_HOME not set" or `mvn` cannot find Java.** Set the `JAVA_HOME` environment variable to your JDK folder and restart the terminal. Run `java -version` to confirm it is JDK 17 or newer.

**"Port 8080 was already in use".** Stop the other program using the port, or start CodeCraftHub on another port with `--server.port=9090` (see Configuration).

**`mvn: command not found`.** Install Maven and add its `bin` folder to your `PATH`.

**Build fails with "release version 17 not supported".** Your JDK is older than 17. Install JDK 17 or 21.

**The dashboard says "Cannot reach the API".** The backend is not running, or it is on a different port. Start it with `mvn spring-boot:run`, and check that `http://localhost:8080/api/courses` opens in your browser.

**The browser console shows a CORS error.** Restart the backend after changing `application.properties`. Check that `codecrafthub.cors.allowed-origins` allows the address the dashboard is served from.

**"Could not read courses.json".** The file contains invalid JSON. Fix it, or delete it and the app creates a new empty one. The app never overwrites a file it cannot read, so your data is safe until you decide.

**Changes to a course do not show up.** Make sure you are looking at the `courses.json` in the folder you started the app from.

## Publish to GitHub

```bash
git init
git add .
git commit -m "Complete CodeCraftHub learning platform"
git branch -M main
git remote add origin https://github.com/YOUR-USERNAME/codecrafthub.git
git push -u origin main
```

When Git asks for a password, paste a GitHub personal access token, not your account password.
If you keep the dashboard in its own repository (the lab suggests `codecrafthub-course-dashboard`), copy `index.html` into that folder and use the same commands.
