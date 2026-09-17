# warranty-tracker-api

A personal warranty tracker REST API for managing products and tracking warranty expiration dates.

## Prerequisites

Before running the Warranty Tracker API, ensure you have the following installed:

- **Git** — For cloning the repository
- **Java 21** — The project requires JDK 21 or later
- **Gradle** — The project uses Gradle wrapper, so a separate Gradle installation is optional (the wrapper is included)

## How to Run

### Local Development (Spring Boot)

1. **Clone the repository:**
   ```bash
   git clone <repository-url>
   ```

2. **Navigate to the project directory:**
   ```bash
   cd warranty-tracker-api
   ```

3. **Run the application using Gradle wrapper:**
   ```bash
   ./gradlew bootRun
   ```
   On Windows:
   ```bash
   gradlew.bat bootRun
   ```

4. **Wait for the application to start** — You should see:
   ```
   Tomcat started on port(s): 8080 (http)
   ```

The application uses an H2 in-memory database for the current local setup. Data persists during the application session and is reset when the application restarts.

## Available URLs

Once the application is running, you can access:

### H2 Console
- **URL:** http://localhost:8080/h2-console
- **Purpose:** Web interface to explore and query the H2 in-memory database
- **Connection Details:** Use the following (from `application.properties`):
  - JDBC URL: `jdbc:h2:mem:warranty_db`
  - Username: `sa`
  - Password: (leave empty)

### Swagger UI
- **URL:** http://localhost:8080/swagger-ui/index.html
- **Purpose:** Interactive API documentation and testing interface
- **Usage:** Explore the available REST endpoints and call them directly from the browser
