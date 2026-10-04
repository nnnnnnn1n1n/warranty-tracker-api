# warranty-tracker-api

A personal warranty tracker REST API for managing products and tracking warranty expiration dates.

## Prerequisites

Before running the Warranty Tracker API, ensure you have the following installed:

- **Git** — For cloning the repository
- **Java 21** — The project requires JDK 21 or later
- **Gradle** — The project uses Gradle wrapper, so a separate Gradle installation is optional (the wrapper is included)
- **Docker & Docker Compose** — Required for running PostgreSQL locally (optional for development with in-memory database)

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

3. **Choose your database option and run the application** — See Database Options below.

### Database Options

The application supports two database configurations for local development:

#### Option 1: H2 In-Memory Database (Default)

The application uses an H2 in-memory database by default. No additional setup is required.

**Run the application:**

```bash
./gradlew bootRun
```

On Windows:

```bash
gradlew.bat bootRun
```

**Characteristics:**
- Data persists during the application session and is reset when the application restarts
- Quick testing and development without external dependencies
- H2 Console available at http://localhost:8080/h2-console

#### Option 2: PostgreSQL (Persistent Database)

The project provides Docker Compose configuration for running PostgreSQL locally. This profile is recommended for development that requires persistent data across application restarts.

**Prerequisites:**
- Docker and Docker Compose must be installed

**Step 1: Start PostgreSQL**

```bash
docker-compose up -d
```

The container will start in the background. PostgreSQL will be available at:
- **Host:** localhost
- **Port:** 5432
- **Database:** `warranty_tracker`
- **Username:** `postgres`
- **Password:** `postgres`

**Step 2: Verify PostgreSQL is Running**

```bash
docker-compose ps
```

You should see the `warranty-tracker-postgres` container with status "Up". To verify the database connection directly:

```bash
docker-compose exec postgres pg_isready -U postgres
```

**Step 3: Run the application with PostgreSQL profile**

```bash
./gradlew bootRun --args='--spring.profiles.active=postgresql'
```

On Windows:

```bash
gradlew.bat bootRun --args='--spring.profiles.active=postgresql'
```

**Managing PostgreSQL:**

Stop PostgreSQL (data is preserved):

```bash
docker-compose down
```

Remove PostgreSQL container and all data:

```bash
docker-compose down -v
```

### Wait for Application Startup

After running the application with either option, you should see:

```
Tomcat started on port(s): 8080 (http)
```

## Available URLs

Once the application is running, you can access:

### H2 Console (H2 Database Only)
- **URL:** http://localhost:8080/h2-console
- **Purpose:** Web interface to explore and query the H2 in-memory database
- **Connection Details:** Use the following (from `application.properties`):
  - JDBC URL: `jdbc:h2:mem:warranty_db`
  - Username: `sa`
  - Password: (leave empty)
- **Note:** Only available when using the H2 in-memory database

### Swagger UI
- **URL:** http://localhost:8080/swagger-ui/index.html
- **Purpose:** Interactive API documentation and testing interface
- **Usage:** Explore the available REST endpoints and call them directly from the browser

## Running Tests

The project includes unit tests for services, repositories, and controllers.

### Run All Tests

```bash
./gradlew test
```

On Windows:

```bash
gradlew.bat test
```

### Run Specific Test Class

```bash
./gradlew test --tests WarrantyCalculatorTest
```

On Windows:

```bash
gradlew.bat test --tests WarrantyCalculatorTest
```

### Run Integration Tests

Integration tests verify end-to-end behavior of API endpoints with a real database context. These tests use @SpringBootTest with an H2 in-memory database.

**Run all integration tests:**

```bash
./gradlew test --tests "*IntegrationTest"
```

On Windows:

```bash
gradlew.bat test --tests "*IntegrationTest"
```

**Run a specific integration test class:**

```bash
./gradlew test --tests PaginationIntegrationTest
```

On Windows:

```bash
gradlew.bat test --tests PaginationIntegrationTest
```

**Run a specific test method within an integration test class:**

```bash
./gradlew test --tests PaginationIntegrationTest.testPagination_FirstPage
```

On Windows:

```bash
gradlew.bat test --tests PaginationIntegrationTest.testPagination_FirstPage
```

**Available Integration Tests:**

- `PaginationIntegrationTest` — Tests pagination of GET /api/products endpoint (27 tests)
  - Basic pagination: first page, second page, last page
  - Default parameters and metadata validation
  - Edge cases: single product per page, offset beyond results, large limits
  - Pagination with filters: category, status, and combined filters
  - Sorting integration with pagination
  - Sequential pagination consistency
  - Response structure validation
  - Partial page handling

### Run Tests with Detailed Output

```bash
./gradlew test --info
```

On Windows:

```bash
gradlew.bat test --info
```

### View Test Results

After running tests, test results are available in:

```
build/reports/tests/test/index.html
```

Open this file in a browser to view a detailed test report with pass/fail status for each test.