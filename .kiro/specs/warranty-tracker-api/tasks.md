# Implementation Plan: Warranty Tracker REST API

## Overview

The Warranty Tracker REST API is a Spring Boot 3 application that provides REST endpoints for managing product categories and tracking warranty expiration dates. This implementation plan breaks down the design into 20 discrete coding tasks organized into 6 phases, with each task building on previous steps. The plan ensures incremental validation of core functionality through unit tests and checkpoints.

---

## Notes

| ID | Title | Phase | Complexity | Status | Files |
|----|-------|-------|------------|--------|-------|
| 1 | Configure OpenAPI/Swagger UI with Springdoc | 1 | Low | Not Started | build.gradle, application.properties, OpenApiConfig.java |
| 2 | Set Up Gradle Project Structure and Dependencies | 1 | Medium | Not Started | build.gradle, settings.gradle, gradle.properties |
| 3 | Configure Spring Boot Application and H2 Database | 1 | Medium | Not Started | application.properties, WarrantyTrackerApiApplication.java |
| 4 | Create Category and Product JPA Entities | 2 | Medium | Not Started | Category.java, Product.java |
| 5 | Implement Category and Product Repository Interfaces | 2 | Low | Not Started | CategoryRepository.java, ProductRepository.java |
| 6 | Create Request/Response DTOs for API Contracts | 2 | Medium | Not Started | CreateProductRequest.java, UpdateProductRequest.java, CategoryResponse.java, ProductResponse.java |
| 7 | Implement WarrantyCalculator for Status Determination | 3 | Low | Not Started | WarrantyStatus.java, WarrantyCalculator.java |
| 8 | Implement CategoryService with Business Logic | 3 | Low | Not Started | CategoryService.java |
| 9 | Implement ProductService with Validation and Calculations | 3 | High | Not Started | ProductService.java |
| 10 | Create Custom Exception Classes and GlobalExceptionHandler | 3 | Medium | Not Started | CategoryNotFoundException.java, ProductNotFoundException.java, ValidationException.java, GlobalExceptionHandler.java |
| 11 | Implement DataLoader for Sample Data Initialization | 3 | Low | Not Started | DataLoader.java |
| 12 | Create CategoryController REST Endpoints | 4 | Medium | Not Started | CategoryController.java |
| 13 | Create ProductController REST Endpoints (CRUD) | 4 | High | Not Started | ProductController.java |
| 14 | Implement Expiring-Soon Query Endpoint | 4 | Medium | Not Started | ProductController.java (expiring-soon endpoint) |
| 15 | Write Unit Tests for WarrantyCalculator | 5 | Low | Not Started | WarrantyCalculatorTest.java |
| 16 | Write Unit Tests for CategoryService | 5 | Medium | Not Started | CategoryServiceTest.java |
| 17 | Write Unit Tests for ProductService | 5 | High | Not Started | ProductServiceTest.java |
| 18 | Write Unit Tests for CategoryController | 5 | Medium | Not Started | CategoryControllerTest.java |
| 19 | Write Unit Tests for ProductController | 5 | High | Not Started | ProductControllerTest.java |
| 20 | Final Checkpoint and Manual API Testing | 6 | Medium | Not Started | All components verified |

---
## Tasks
## Phase 1: Project Setup & API Documentation

- [x] 1. Configure OpenAPI/Swagger UI with Springdoc

**Description**: Set up OpenAPI/Swagger UI documentation endpoint using Springdoc OpenAPI library. Configure automatic API documentation generation that displays all endpoints, request/response schemas, and HTTP status codes.

**Complexity**: Low

**Dependencies**: 
- Gradle build system configured
- Spring Boot 3 base project structure in place

**Files to Create/Modify**:
- `build.gradle` (add Springdoc dependency)
- `application.properties` (add OpenAPI configuration)
- `src/main/java/com/warranty/config/OpenApiConfig.java` (new file)

**Acceptance Criteria**:
1. WHEN Springdoc library is added to build.gradle, THE build completes successfully
2. WHEN application.properties includes OpenAPI configuration, THE following endpoints are accessible:
   - `/v3/api-docs` (returns OpenAPI JSON specification)
   - `/swagger-ui.html` (serves interactive Swagger UI)
3. WHEN visiting `/swagger-ui.html`, THE UI displays with proper styling and structure
4. WHEN OpenApiConfig class is created with @Configuration annotation, THE API title is "Warranty Tracker REST API" and version is "1.0.0"
5. WHEN checking application logs on startup, NO Springdoc-related errors appear
6. THE configuration sets `springdoc.swagger-ui.operations-sorter=method` to sort endpoints by HTTP method
7. THE configuration sets `springdoc.swagger-ui.tags-sorter=alpha` to sort tags alphabetically

---

- [x] 2. Set Up Gradle Project Structure and Dependencies

**Description**: Configure Gradle build system with all required dependencies for Spring Boot 3, Spring Data JPA, H2 database, Lombok, validation, and testing frameworks. Set up project structure following Maven conventions.

**Complexity**: Medium

**Dependencies**: 
- Gradle CLI installed and available
- Java 17+ JDK configured

**Files to Create/Modify**:
- `build.gradle` (create/update with all dependencies)
- `settings.gradle` (create if not exists)
- `gradle.properties` (create with Gradle configuration)

**Acceptance Criteria**:
1. WHEN `build.gradle` is created, THE following dependencies are specified:
   - Spring Boot 3.x (spring-boot-starter-web, spring-boot-starter-data-jpa)
   - Spring Boot Starter Validation (spring-boot-starter-validation)
   - H2 Database (com.h2database:h2)
   - Springdoc OpenAPI UI (springdoc-openapi-starter-webmvc-ui)
   - Lombok (org.projectlombok:lombok)
   - JUnit 5 (spring-boot-starter-test, junit-jupiter)
   - Mockito (org.mockito:mockito-core, org.mockito:mockito-junit-jupiter)
2. WHEN `gradle build` is executed, THE build completes without errors
3. WHEN `gradle dependencies` is run, ALL specified dependencies are downloaded and resolved correctly
4. WHEN `settings.gradle` is created, THE rootProject.name is set to "warranty-tracker-api"
5. WHEN `gradle.properties` is created, THE Gradle version is specified (7.x or higher)
6. WHEN checking the project structure, THE following directories exist:
   - `src/main/java/com/warranty/`
   - `src/main/resources/`
   - `src/test/java/com/warranty/`
   - `src/test/resources/`

---

- [x] 3. Configure Spring Boot Application and H2 Database

**Description**: Create application configuration files and main Spring Boot application class. Configure H2 in-memory database with Hibernate DDL auto-generation and optional H2 console for development.

**Complexity**: Medium

**Dependencies**: 
- Gradle project structure set up (Task 2)
- Dependencies resolved

**Files to Create/Modify**:
- `src/main/resources/application.properties` (create/update)
- `src/main/java/com/warranty/WarrantyTrackerApiApplication.java` (create)

**Acceptance Criteria**:
1. WHEN `application.properties` is configured, THE following properties are set:
   - `spring.datasource.url=jdbc:h2:mem:warranty_db` (in-memory H2 database)
   - `spring.datasource.driverClassName=org.h2.Driver`
   - `spring.datasource.username=sa`
   - `spring.datasource.password=` (empty password)
2. WHEN H2 console is enabled, THE following properties are set:
   - `spring.h2.console.enabled=true`
   - `spring.h2.console.path=/h2-console`
3. WHEN Hibernate is configured, THE following properties are set:
   - `spring.jpa.database-platform=org.hibernate.dialect.H2Dialect`
   - `spring.jpa.hibernate.ddl-auto=create-drop` (recreate schema on startup)
4. WHEN `WarrantyTrackerApiApplication.java` is created with `@SpringBootApplication` annotation, THE main method calls `SpringApplication.run()`
5. WHEN `gradle bootRun` is executed, THE application starts successfully and logs show:
   - "H2 Console available at 'http://localhost:8080/h2-console'" (or similar)
   - "Hibernate creating schema from entities" message
   - No errors or warnings related to database configuration

---

## Phase 2: Domain Model & Persistence

- [x] 4. Create Category and Product JPA Entities

**Description**: Implement JPA entity classes for Category and Product with proper annotations, validation constraints, and relationships. Configure the one-to-many relationship between Category and Product.

**Complexity**: Medium

**Dependencies**: 
- Spring Boot application configured (Task 3)
- H2 database configured

**Files to Create/Modify**:
- `src/main/java/com/warranty/entity/Category.java` (create)
- `src/main/java/com/warranty/entity/Product.java` (create)

**Acceptance Criteria**:
1. WHEN `Category.java` entity is created, IT MUST include:
   - `@Entity` and `@Table(name = "category")` annotations
   - `id` field: `@Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;`
   - `name` field: `@Column(unique = true, nullable = false) private String name;`
   - `products` field: `@OneToMany(mappedBy = "category") private Set<Product> products;`
   - Lombok annotations: `@Data`, `@NoArgsConstructor`, `@AllArgsConstructor`
   - Constructor: public Category(String name)
2. WHEN `Product.java` entity is created, IT MUST include:
   - `@Entity` and `@Table(name = "product")` annotations
   - `id` field: `@Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;`
   - `name` field: `@Column(nullable = false) private String name;`
   - `purchaseDate` field: `@Column(nullable = false) private LocalDate purchaseDate;`
   - `warrantyMonths` field: `@Column(nullable = false) private Integer warrantyMonths;`
   - `category` field: `@ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "category_id", nullable = false) private Category category;`
   - Lombok annotations: `@Data`, `@NoArgsConstructor`, `@AllArgsConstructor`
3. WHEN the application starts with these entities, THE H2 schema includes:
   - `category` table with columns: id (BIGINT PRIMARY KEY AUTO_INCREMENT), name (VARCHAR(255) NOT NULL UNIQUE)
   - `product` table with columns: id (BIGINT PRIMARY KEY AUTO_INCREMENT), name (VARCHAR(255) NOT NULL), purchase_date (DATE NOT NULL), warranty_months (INTEGER NOT NULL), category_id (BIGINT NOT NULL, FOREIGN KEY)
4. WHEN Hibernate DDL is executed, NO errors appear in logs related to entity mapping or schema generation
5. WHEN viewing the H2 console, BOTH tables exist and can be queried

---

- [x] 5. Implement Category and Product Repository Interfaces

**Description**: Create Spring Data JPA repository interfaces for Category and Product entities. Define custom query methods needed for expiring-soon filtering and product lookups.

**Complexity**: Low

**Dependencies**: 
- JPA entities created (Task 4)
- Spring Data JPA dependency available

**Files to Create/Modify**:
- `src/main/java/com/warranty/repository/CategoryRepository.java` (create)
- `src/main/java/com/warranty/repository/ProductRepository.java` (create)

**Acceptance Criteria**:
1. WHEN `CategoryRepository.java` is created, IT MUST:
   - Extend `JpaRepository<Category, Long>`
   - Be annotated with `@Repository`
   - Include method: `Optional<Category> findByName(String name);` (for lookup by name)
   - Have access to inherited CRUD methods: findAll(), findById(), save(), delete()
2. WHEN `ProductRepository.java` is created, IT MUST:
   - Extend `JpaRepository<Product, Long>`
   - Be annotated with `@Repository`
   - Include custom query method: `List<Product> findProductsExpiringWithin(LocalDate endDate);` (for filtering by warranty end date)
   - Have access to inherited CRUD methods: findAll(), findById(), save(), delete()
3. WHEN `ProductRepository.findAll()` is called after saving products, IT returns all saved Product entities in database order
4. WHEN `CategoryRepository.findById(1L)` is called with valid ID, IT returns an Optional containing the Category
5. WHEN `CategoryRepository.findById(999L)` is called with invalid ID, IT returns an empty Optional
6. WHEN `ProductRepository.save()` is called with a new Product, THE Product receives an auto-generated ID from the database

---

- [x] 6. Create Request/Response DTOs for API Contracts

**Description**: Implement Data Transfer Objects (DTOs) for API requests and responses. These DTOs decouple the API contract from JPA entity structure and enable calculated fields like warrantyEndDate and warrantyStatus.

**Complexity**: Medium

**Dependencies**: 
- JPA entities created (Task 4)
- Lombok dependency available

**Files to Create/Modify**:
- `src/main/java/com/warranty/dto/request/CreateProductRequest.java` (create)
- `src/main/java/com/warranty/dto/request/UpdateProductRequest.java` (create)
- `src/main/java/com/warranty/dto/response/CategoryResponse.java` (create)
- `src/main/java/com/warranty/dto/response/ProductResponse.java` (create)
- `src/main/java/com/warranty/dto/response/ErrorResponse.java` (create)
- `src/main/java/com/warranty/dto/internal/WarrantyStatus.java` (create - will be moved later)

**Acceptance Criteria**:
1. WHEN `CreateProductRequest.java` is created, IT MUST include:
   - `@Data`, `@NoArgsConstructor`, `@AllArgsConstructor` Lombok annotations
   - `name` field: `@NotBlank(message = "Product name cannot be empty") private String name;`
   - `categoryId` field: `@NotNull(message = "Category ID is required") private Long categoryId;`
   - `purchaseDate` field: `@NotNull(message = "Purchase date is required") private LocalDate purchaseDate;`
   - `warrantyMonths` field: `@NotNull(message = "Warranty months is required") @Positive(message = "Warranty months must be greater than 0") private Integer warrantyMonths;`
2. WHEN `UpdateProductRequest.java` is created, IT MUST have identical fields and validation as CreateProductRequest
3. WHEN `CategoryResponse.java` is created, IT MUST include:
   - `@Data`, `@NoArgsConstructor`, `@AllArgsConstructor` Lombok annotations
   - `id` field: `private Long id;`
   - `name` field: `private String name;`
4. WHEN `ProductResponse.java` is created, IT MUST include:
   - `@Data`, `@NoArgsConstructor`, `@AllArgsConstructor` Lombok annotations
   - `id`, `name`, `categoryId`, `purchaseDate`, `warrantyMonths` fields (matching request fields)
   - `warrantyEndDate` field: `private LocalDate warrantyEndDate;` (calculated field)
   - `warrantyStatus` field: `private String warrantyStatus;` (calculated field, e.g., "ACTIVE")
   - `category` field: `private CategoryResponse category;` (nested CategoryResponse)
5. WHEN `ErrorResponse.java` is created, IT MUST include:
   - `@Data`, `@NoArgsConstructor`, `@AllArgsConstructor` Lombok annotations
   - `timestamp` field: `private LocalDateTime timestamp;`
   - `status` field: `private int status;`
   - `error` field: `private String error;`
   - `message` field: `private String message;`
   - `path` field: `private String path;`
   - `fieldErrors` field: `private List<FieldError> fieldErrors;` (inner class with field and message)
6. WHEN `WarrantyStatus.java` is created as an enum, IT MUST include:
   - Three enum constants: `ACTIVE`, `EXPIRING_SOON`, `EXPIRED`

---

## Phase 3: Business Logic & Services

- [x] 7. Implement WarrantyCalculator for Status Determination

**Description**: Create WarrantyCalculator utility class that calculates warranty status based on warranty end date and current date. This is a pure, stateless component that implements the warranty status business rules.

**Complexity**: Low

**Dependencies**: 
- WarrantyStatus enum created (Task 6)

**Files to Create/Modify**:
- `src/main/java/com/warranty/service/WarrantyCalculator.java` (create)

**Acceptance Criteria**:
1. WHEN `WarrantyCalculator.java` is created, IT MUST include:
   - Public static method: `public static WarrantyStatus calculateStatus(LocalDate warrantyEndDate, LocalDate today)`
   - Private constant: `private static final int EXPIRING_SOON_DAYS = 30;`
2. WHEN `calculateStatus()` is called with `warrantyEndDate=2024-01-15` and `today=2024-06-15`:
   - THE method returns `WarrantyStatus.EXPIRED` (because today > warrantyEndDate)
3. WHEN `calculateStatus()` is called with `warrantyEndDate=2024-07-10` and `today=2024-06-15`:
   - THE method returns `WarrantyStatus.EXPIRING_SOON` (because today <= warrantyEndDate AND warrantyEndDate <= today + 30 days)
4. WHEN `calculateStatus()` is called with `warrantyEndDate=2025-06-15` and `today=2024-06-15`:
   - THE method returns `WarrantyStatus.ACTIVE` (because warrantyEndDate > today + 30 days)
5. WHEN `calculateStatus()` is called with boundary case `warrantyEndDate=today+30` and `today=today`:
   - THE method returns `WarrantyStatus.EXPIRING_SOON` (inclusive upper boundary)
6. WHEN `calculateStatus()` is called with boundary case `warrantyEndDate=today+31` and `today=today`:
   - THE method returns `WarrantyStatus.ACTIVE` (exclusive boundary at 31 days)

---

- [x] 8. Implement CategoryService with Business Logic

**Description**: Create CategoryService class that encapsulates category-related business logic. Handle retrieval of all categories and retrieval by ID with appropriate error handling.

**Complexity**: Low

**Dependencies**: 
- CategoryRepository created (Task 5)
- CategoryResponse DTO created (Task 6)

**Files to Create/Modify**:
- `src/main/java/com/warranty/service/CategoryService.java` (create)

**Acceptance Criteria**:
1. WHEN `CategoryService.java` is created, IT MUST:
   - Be annotated with `@Service`
   - Have dependency injection of `CategoryRepository` via constructor
   - Include Lombok `@RequiredArgsConstructor` annotation
2. WHEN `getAllCategories()` method is called, IT MUST:
   - Call `categoryRepository.findAll()`
   - Convert each Category entity to CategoryResponse DTO
   - Return `List<CategoryResponse>` (empty list if no categories exist)
3. WHEN `getCategoryById(Long id)` method is called with valid ID, IT MUST:
   - Call `categoryRepository.findById(id)`
   - Throw `CategoryNotFoundException` with message "Category with ID {id} not found" if not found
   - Return `CategoryResponse` if found
4. WHEN `saveSampleCategories()` method is called, IT MUST:
   - Create Category entities for: "Kitchen Appliances", "Bedroom", "Beauty", "Electronics", "Home Appliances"
   - Call `categoryRepository.saveAll()` with all categories
5. WHEN multiple calls to `getAllCategories()` are made, THE same Category entities are returned in the same order

---

- [-] 9. Implement ProductService with Validation and Calculations

**Description**: Create ProductService class that encapsulates all product-related business logic. Handle CRUD operations, warranty calculations, validation, and expiring-soon filtering with comprehensive error handling.

**Complexity**: High

**Dependencies**: 
- ProductRepository created (Task 5)
- CategoryService created (Task 8)
- WarrantyCalculator created (Task 7)
- DTOs created (Task 6)
- Custom exceptions (will be created in Task 10)

**Files to Create/Modify**:
- `src/main/java/com/warranty/service/ProductService.java` (create)

**Acceptance Criteria**:
1. WHEN `ProductService.java` is created, IT MUST:
   - Be annotated with `@Service`
   - Have dependency injection of `ProductRepository` and `CategoryService` via constructor
   - Include Lombok `@RequiredArgsConstructor` annotation
2. WHEN `createProduct(CreateProductRequest request)` is called with valid data, IT MUST:
   - Validate: name is not blank (throw ValidationException if empty)
   - Validate: categoryId references existing category (call `categoryService.getCategoryById()`, catch CategoryNotFoundException)
   - Validate: purchaseDate is not in future (throw ValidationException if future)
   - Validate: warrantyMonths is > 0 (throw ValidationException if <= 0)
   - Calculate: `warrantyEndDate = purchaseDate.plusMonths(warrantyMonths)`
   - Create Product entity and call `productRepository.save()`
   - Return ProductResponse with calculated warrantyEndDate and warrantyStatus
3. WHEN `createProduct()` is called with future purchaseDate, IT MUST throw `ValidationException` with message "Purchase date cannot be in the future"
4. WHEN `createProduct()` is called with warrantyMonths <= 0, IT MUST throw `ValidationException` with message "Warranty months must be greater than 0"
5. WHEN `createProduct()` is called with non-existent categoryId, IT MUST throw `CategoryNotFoundException` with message "Category with ID {id} not found"
6. WHEN `getAllProducts()` is called, IT MUST:
   - Call `productRepository.findAll()`
   - Convert each Product entity to ProductResponse DTO
   - Calculate warrantyStatus using WarrantyCalculator for each product
   - Return `List<ProductResponse>` (empty list if no products)
7. WHEN `getProductById(Long id)` is called with valid ID, IT MUST:
   - Call `productRepository.findById(id)`
   - Throw `ProductNotFoundException` with message "Product with ID {id} not found" if not found
   - Return ProductResponse with calculated values
8. WHEN `getProductById(Long id)` is called with invalid ID, IT MUST throw `ProductNotFoundException`
9. WHEN `updateProduct(Long id, UpdateProductRequest request)` is called with valid data, IT MUST:
   - Retrieve existing Product (throw `ProductNotFoundException` if not found)
   - Apply same validation rules as createProduct
   - Update: name, categoryId, purchaseDate, warrantyMonths
   - Recalculate warrantyEndDate
   - Call `productRepository.save()` to persist updates
   - Return updated ProductResponse
10. WHEN `deleteProduct(Long id)` is called with valid ID, IT MUST:
    - Retrieve existing Product (throw `ProductNotFoundException` if not found)
    - Call `productRepository.deleteById(id)`
    - Return void (no return value)
11. WHEN `getProductsExpiringWithin(Integer days)` is called with positive days value, IT MUST:
    - Use default days = 30 if days is null
    - Throw `ValidationException` with message "Days parameter must be non-negative" if days < 0
    - Calculate end date range: `today <= warrantyEndDate <= today + days`
    - Retrieve products using custom repository method
    - Convert to ProductResponse DTOs with calculated warranties
    - Return sorted by warrantyEndDate ascending

---

- [ ] 10. Create Custom Exception Classes and GlobalExceptionHandler

**Description**: Implement custom exception classes for business errors (CategoryNotFoundException, ProductNotFoundException, ValidationException) and create a global exception handler that converts exceptions to standardized ErrorResponse objects with appropriate HTTP status codes.

**Complexity**: Medium

**Dependencies**: 
- ErrorResponse DTO created (Task 6)
- Spring Boot web framework available

**Files to Create/Modify**:
- `src/main/java/com/warranty/exception/CategoryNotFoundException.java` (create)
- `src/main/java/com/warranty/exception/ProductNotFoundException.java` (create)
- `src/main/java/com/warranty/exception/ValidationException.java` (create)
- `src/main/java/com/warranty/exception/GlobalExceptionHandler.java` (create)

**Acceptance Criteria**:
1. WHEN `CategoryNotFoundException.java` is created, IT MUST:
   - Extend `RuntimeException`
   - Include constructor: `public CategoryNotFoundException(String message)`
   - Include constructor: `public CategoryNotFoundException(Long categoryId)`
2. WHEN `ProductNotFoundException.java` is created, IT MUST:
   - Extend `RuntimeException`
   - Include constructor: `public ProductNotFoundException(String message)`
   - Include constructor: `public ProductNotFoundException(Long productId)`
3. WHEN `ValidationException.java` is created, IT MUST:
   - Extend `RuntimeException`
   - Include constructor: `public ValidationException(String message)`
4. WHEN `GlobalExceptionHandler.java` is created, IT MUST:
   - Be annotated with `@RestControllerAdvice`
   - Be annotated with `@Slf4j` (Lombok for logging)
5. WHEN `GlobalExceptionHandler` handles `CategoryNotFoundException`, IT MUST:
   - Log the exception at WARN level
   - Return HTTP 404 status code
   - Return ErrorResponse with status=404, error="Not Found", message containing category ID
6. WHEN `GlobalExceptionHandler` handles `ProductNotFoundException`, IT MUST:
   - Log the exception at WARN level
   - Return HTTP 404 status code
   - Return ErrorResponse with status=404, error="Not Found", message containing product ID
7. WHEN `GlobalExceptionHandler` handles `ValidationException`, IT MUST:
   - Log the exception at WARN level
   - Return HTTP 400 status code
   - Return ErrorResponse with status=400, error="Validation Error", message with validation details
8. WHEN `GlobalExceptionHandler` handles `MethodArgumentNotValidException` (validation constraint violations), IT MUST:
   - Log the exception at WARN level
   - Return HTTP 400 status code
   - Return ErrorResponse with fieldErrors array containing field names and constraint violation messages
9. WHEN `GlobalExceptionHandler` handles generic `Exception`, IT MUST:
   - Log the exception at ERROR level with full stack trace
   - Return HTTP 500 status code
   - Return ErrorResponse with status=500, error="Internal Server Error", message="An unexpected error occurred"
10. WHEN ErrorResponse is returned, ALL instances MUST include:
    - `timestamp`: Current LocalDateTime when error occurred
    - `status`: HTTP status code
    - `error`: Error classification
    - `message`: Human-readable description
    - `path`: Request path that caused the error

---

- [ ] 11. Implement DataLoader for Sample Data Initialization

**Description**: Create DataLoader component that initializes the database with predefined categories and sample products representing all warranty statuses (ACTIVE, EXPIRING_SOON, EXPIRED) on application startup.

**Complexity**: Low

**Dependencies**: 
- CategoryService created (Task 8)
- ProductService created (Task 9)
- H2 database configured (Task 3)

**Files to Create/Modify**:
- `src/main/java/com/warranty/initialization/DataLoader.java` (create)

**Acceptance Criteria**:
1. WHEN `DataLoader.java` is created, IT MUST:
   - Implement `ApplicationRunner` interface
   - Be annotated with `@Component`
   - Have dependency injection of `CategoryService` and `ProductService` via constructor
   - Include Lombok `@RequiredArgsConstructor` annotation
2. WHEN `run()` method is executed on application startup, IT MUST:
   - Call CategoryService to save predefined categories: "Kitchen Appliances", "Bedroom", "Beauty", "Electronics", "Home Appliances"
   - Create and save at least 3 sample products with different warranty statuses
3. WHEN sample product with EXPIRED status is created, IT MUST:
   - Name: "Refrigerator"
   - Category: "Kitchen Appliances"
   - PurchaseDate: `LocalDate.now().minusMonths(12)`
   - WarrantyMonths: 24 (calculated end date: 12 months ago, now EXPIRED)
   - WarrantyStatus: EXPIRED
4. WHEN sample product with EXPIRING_SOON status is created, IT MUST:
   - Name: "Microwave"
   - Category: "Kitchen Appliances"
   - PurchaseDate: `LocalDate.now().minusMonths(18)`
   - WarrantyMonths: 24 (calculated end date: approximately 6 months from now, within 30 days if today is set appropriately)
   - WarrantyStatus: EXPIRING_SOON
5. WHEN sample product with ACTIVE status is created, IT MUST:
   - Name: "Laptop"
   - Category: "Electronics"
   - PurchaseDate: `LocalDate.now().minusMonths(6)`
   - WarrantyMonths: 36 (calculated end date: 30 months from now, beyond 30-day window)
   - WarrantyStatus: ACTIVE
6. WHEN application starts with DataLoader enabled, THE following messages appear in logs:
   - "Loading sample categories..." (or similar)
   - "Loading sample products..." (or similar)
   - No errors or exceptions from DataLoader

---

## Phase 4: REST API Controllers

- [ ] 12. Create CategoryController REST Endpoints

**Description**: Implement CategoryController with REST endpoints for retrieving all categories and retrieving a specific category by ID. Handle URL routing, HTTP methods, status codes, and response serialization.

**Complexity**: Medium

**Dependencies**: 
- CategoryService created (Task 8)
- CategoryResponse DTO created (Task 6)
- GlobalExceptionHandler created (Task 10)

**Files to Create/Modify**:
- `src/main/java/com/warranty/controller/CategoryController.java` (create)

**Acceptance Criteria**:
1. WHEN `CategoryController.java` is created, IT MUST:
   - Be annotated with `@RestController`
   - Be annotated with `@RequestMapping("/api/categories")`
   - Have dependency injection of `CategoryService` via constructor
   - Include Lombok `@RequiredArgsConstructor` annotation
2. WHEN `GET /api/categories` endpoint is called, IT MUST:
   - Call `categoryService.getAllCategories()`
   - Return HTTP 200 status code
   - Return JSON array of CategoryResponse objects
   - Return empty array `[]` if no categories exist
3. WHEN `GET /api/categories` response is returned, THE response structure MUST be:
   ```json
   [
     { "id": 1, "name": "Kitchen Appliances" },
     { "id": 2, "name": "Bedroom" },
     ...
   ]
   ```
4. WHEN `GET /api/categories/{id}` endpoint is called with valid category ID, IT MUST:
   - Call `categoryService.getCategoryById(id)`
   - Return HTTP 200 status code
   - Return CategoryResponse DTO as JSON
5. WHEN `GET /api/categories/{id}` endpoint is called with non-existent category ID, IT MUST:
   - CategoryService throws `CategoryNotFoundException`
   - GlobalExceptionHandler catches exception
   - Return HTTP 404 status code
   - Return ErrorResponse with message "Category with ID {id} not found"
6. WHEN `GET /api/categories/{id}` endpoint is called with invalid (non-numeric) ID, IT MUST:
   - Spring framework validation catches invalid format
   - Return HTTP 400 status code
   - Return ErrorResponse indicating invalid format

---

- [ ] 13. Create ProductController REST Endpoints (CRUD)

**Description**: Implement ProductController with REST endpoints for CRUD operations on products: create (POST), retrieve all (GET), retrieve by ID (GET), update (PUT), and delete (DELETE). Handle request validation, response serialization, and appropriate HTTP status codes.

**Complexity**: High

**Dependencies**: 
- ProductService created (Task 9)
- CreateProductRequest and UpdateProductRequest DTOs created (Task 6)
- ProductResponse DTO created (Task 6)
- GlobalExceptionHandler created (Task 10)

**Files to Create/Modify**:
- `src/main/java/com/warranty/controller/ProductController.java` (create)

**Acceptance Criteria**:
1. WHEN `ProductController.java` is created, IT MUST:
   - Be annotated with `@RestController`
   - Be annotated with `@RequestMapping("/api/products")`
   - Have dependency injection of `ProductService` via constructor
   - Include Lombok `@RequiredArgsConstructor` annotation
2. WHEN `POST /api/products` endpoint is called with valid request body, IT MUST:
   - Accept JSON body with CreateProductRequest fields: name, categoryId, purchaseDate, warrantyMonths
   - Call `productService.createProduct(request)`
   - Return HTTP 201 status code (Created)
   - Return ProductResponse as JSON body
   - Include Location header: `/api/products/{newProductId}`
3. WHEN `POST /api/products` request body has @Valid validation errors (e.g., empty name), IT MUST:
   - Spring validation catches violation
   - Return HTTP 400 status code
   - Return ErrorResponse with fieldErrors array containing field name and violation message
4. WHEN `POST /api/products` request references non-existent categoryId, IT MUST:
   - ProductService throws `CategoryNotFoundException`
   - GlobalExceptionHandler catches exception
   - Return HTTP 404 status code
   - Return ErrorResponse with message "Category with ID {id} not found"
5. WHEN `POST /api/products` request has invalid data (e.g., future purchaseDate), IT MUST:
   - ProductService throws `ValidationException`
   - GlobalExceptionHandler catches exception
   - Return HTTP 400 status code
   - Return ErrorResponse with validation error message
6. WHEN `GET /api/products` endpoint is called, IT MUST:
   - Call `productService.getAllProducts()`
   - Return HTTP 200 status code
   - Return JSON array of ProductResponse objects
   - Return empty array `[]` if no products exist
7. WHEN `GET /api/products` response is returned, EACH ProductResponse MUST include:
   - id, name, categoryId, purchaseDate, warrantyMonths, warrantyEndDate, warrantyStatus, category (CategoryResponse)
8. WHEN `GET /api/products/{id}` endpoint is called with valid product ID, IT MUST:
   - Call `productService.getProductById(id)`
   - Return HTTP 200 status code
   - Return ProductResponse with all fields including calculated warrantyStatus
9. WHEN `GET /api/products/{id}` endpoint is called with non-existent product ID, IT MUST:
   - ProductService throws `ProductNotFoundException`
   - GlobalExceptionHandler catches exception
   - Return HTTP 404 status code
   - Return ErrorResponse with message "Product with ID {id} not found"
10. WHEN `PUT /api/products/{id}` endpoint is called with valid ID and request body, IT MUST:
    - Accept JSON body with UpdateProductRequest fields (identical to CreateProductRequest)
    - Call `productService.updateProduct(id, request)`
    - Return HTTP 200 status code
    - Return updated ProductResponse with recalculated warranty values
11. WHEN `PUT /api/products/{id}` endpoint is called with non-existent product ID, IT MUST:
    - ProductService throws `ProductNotFoundException`
    - GlobalExceptionHandler catches exception
    - Return HTTP 404 status code
    - Return ErrorResponse with message "Product with ID {id} not found"
12. WHEN `PUT /api/products/{id}` request has invalid data, IT MUST:
    - ProductService throws `ValidationException`
    - Return HTTP 400 status code
    - Return ErrorResponse with validation error details
13. WHEN `DELETE /api/products/{id}` endpoint is called with valid product ID, IT MUST:
    - Call `productService.deleteProduct(id)`
    - Return HTTP 204 status code (No Content)
    - Return empty response body
14. WHEN `DELETE /api/products/{id}` endpoint is called with non-existent product ID, IT MUST:
    - ProductService throws `ProductNotFoundException`
    - Return HTTP 404 status code
    - Return ErrorResponse with message "Product with ID {id} not found"
15. WHEN subsequent `GET /api/products/{id}` is called for a deleted product, IT MUST:
    - Return HTTP 404 status code (product no longer exists)

---

- [ ] 14. Implement Expiring-Soon Query Endpoint

**Description**: Implement the expiring-soon endpoint that filters products by warranty expiration timeline. Accept optional "days" query parameter (default 30) and return products expiring within specified range, sorted by warranty end date.

**Complexity**: Medium

**Dependencies**: 
- ProductController created (Task 13)
- ProductService created (Task 9)
- ProductResponse DTO created (Task 6)

**Files to Create/Modify**:
- `src/main/java/com/warranty/controller/ProductController.java` (modify to add endpoint)

**Acceptance Criteria**:
1. WHEN `GET /api/products/expiring-soon` endpoint is called without query parameters, IT MUST:
   - Use default days = 30
   - Call `productService.getProductsExpiringWithin(30)`
   - Return HTTP 200 status code
   - Return JSON array of ProductResponse objects expiring within 30 days
2. WHEN `GET /api/products/expiring-soon?days=60` endpoint is called with custom days parameter, IT MUST:
   - Parse days parameter as integer (60)
   - Call `productService.getProductsExpiringWithin(60)`
   - Return HTTP 200 status code
   - Return products expiring within 60 days from today
3. WHEN `GET /api/products/expiring-soon?days=0` endpoint is called with days=0, IT MUST:
   - Return only products expiring today (warrantyEndDate == today)
   - Return HTTP 200 status code
4. WHEN `GET /api/products/expiring-soon?days=-5` endpoint is called with negative days, IT MUST:
   - ProductService throws `ValidationException` with message "Days parameter must be non-negative"
   - Return HTTP 400 status code
   - Return ErrorResponse with validation error message
5. WHEN expiring-soon endpoint returns results, EACH ProductResponse MUST include:
   - id, name, categoryId, purchaseDate, warrantyMonths, warrantyEndDate, warrantyStatus, category
6. WHEN expiring-soon endpoint is called and no products expire within the range, IT MUST:
   - Return HTTP 200 status code
   - Return empty array `[]`
7. WHEN expiring-soon endpoint returns multiple products, THEY MUST be sorted by warrantyEndDate in ascending order (earliest expiration first)
8. WHEN `GET /api/products/expiring-soon` is called BEFORE the generic `GET /api/products` in route matching, THE specific expiring-soon route MUST be matched (not treated as {id} parameter)

---

## Phase 5: Unit Tests

- [ ] 15. Write Unit Tests for WarrantyCalculator

**Description**: Implement comprehensive unit tests for WarrantyCalculator class. Test all three warranty status conditions and boundary cases to ensure accurate status determination.

**Complexity**: Low

**Dependencies**: 
- WarrantyCalculator implemented (Task 7)
- WarrantyStatus enum created (Task 6)
- JUnit 5 test framework available
- AssertJ library available (for fluent assertions)

**Files to Create/Modify**:
- `src/test/java/com/warranty/service/WarrantyCalculatorTest.java` (create)

**Acceptance Criteria**:
1. WHEN `WarrantyCalculatorTest.java` is created, IT MUST:
   - Be annotated with `@DisplayName("WarrantyCalculator Tests")`
   - Include test methods with descriptive names following pattern: `test_<condition>_returns_<status>`
2. WHEN test for EXPIRED status is executed, IT MUST:
   - Test case: warrantyEndDate = 2024-01-15, today = 2024-06-15
   - Assert: `calculateStatus()` returns `WarrantyStatus.EXPIRED`
   - Test message: "When warranty end date is in the past, status should be EXPIRED"
3. WHEN test for EXPIRING_SOON status is executed, IT MUST:
   - Test case: warrantyEndDate = today + 15 days, today = today
   - Assert: `calculateStatus()` returns `WarrantyStatus.EXPIRING_SOON`
   - Test message: "When warranty expires within 30 days, status should be EXPIRING_SOON"
4. WHEN test for ACTIVE status is executed, IT MUST:
   - Test case: warrantyEndDate = today + 60 days, today = today
   - Assert: `calculateStatus()` returns `WarrantyStatus.ACTIVE`
   - Test message: "When warranty expires beyond 30 days, status should be ACTIVE"
5. WHEN boundary test for EXPIRING_SOON (inclusive upper boundary) is executed, IT MUST:
   - Test case: warrantyEndDate = today + 30 days (exactly), today = today
   - Assert: `calculateStatus()` returns `WarrantyStatus.EXPIRING_SOON`
   - Test message: "Boundary case: warranty expiring exactly 30 days from now should be EXPIRING_SOON"
6. WHEN boundary test for ACTIVE (exclusive upper boundary) is executed, IT MUST:
   - Test case: warrantyEndDate = today + 31 days, today = today
   - Assert: `calculateStatus()` returns `WarrantyStatus.ACTIVE`
   - Test message: "Boundary case: warranty expiring 31 days from now should be ACTIVE"
7. WHEN boundary test for EXPIRING_SOON (inclusive lower boundary) is executed, IT MUST:
   - Test case: warrantyEndDate = today (exactly), today = today
   - Assert: `calculateStatus()` returns `WarrantyStatus.EXPIRING_SOON`
   - Test message: "Boundary case: warranty expiring today should be EXPIRING_SOON"
8. WHEN all tests pass, THE test class MUST show 100% code coverage for WarrantyCalculator

---

- [ ] 16. Write Unit Tests for CategoryService

**Description**: Implement unit tests for CategoryService business logic. Mock CategoryRepository and test retrieval, error handling, and data transformation.

**Complexity**: Medium

**Dependencies**: 
- CategoryService implemented (Task 8)
- CategoryRepository interface created (Task 5)
- CategoryResponse DTO created (Task 6)
- JUnit 5, Mockito available
- AssertJ available

**Files to Create/Modify**:
- `src/test/java/com/warranty/service/CategoryServiceTest.java` (create)

**Acceptance Criteria**:
1. WHEN `CategoryServiceTest.java` is created, IT MUST:
   - Use `@ExtendWith(MockitoExtension.class)` annotation
   - Mock `CategoryRepository` using `@Mock` annotation
   - Use constructor injection to inject mocked repository into service
   - Include test methods with descriptive names
2. WHEN `getAllCategories()` test is executed with categories in repository, IT MUST:
   - Mock `categoryRepository.findAll()` to return list of Category entities
   - Assert: Result is not null
   - Assert: Result size matches expected count
   - Assert: Each CategoryResponse has correct id and name
   - Test message: "Should return all categories when categories exist"
3. WHEN `getAllCategories()` test is executed with empty repository, IT MUST:
   - Mock `categoryRepository.findAll()` to return empty list
   - Assert: Result is empty list (not null)
   - Test message: "Should return empty list when no categories exist"
4. WHEN `getCategoryById(Long id)` test is executed with valid category ID, IT MUST:
   - Mock `categoryRepository.findById(1L)` to return Optional with Category
   - Assert: Result is CategoryResponse with correct id and name
   - Test message: "Should return CategoryResponse when category exists"
5. WHEN `getCategoryById(Long id)` test is executed with invalid category ID, IT MUST:
   - Mock `categoryRepository.findById(999L)` to return empty Optional
   - Assert: `CategoryNotFoundException` is thrown with message containing ID
   - Test message: "Should throw CategoryNotFoundException when category does not exist"
6. WHEN `saveSampleCategories()` test is executed, IT MUST:
   - Verify `categoryRepository.saveAll()` is called
   - Verify all 5 predefined categories are saved
   - Test message: "Should save all 5 predefined categories"
7. WHEN tests complete, Mockito MUST verify:
   - Repository methods are called correct number of times
   - No unnecessary calls to repository

---

- [ ] 17. Write Unit Tests for ProductService

**Description**: Implement comprehensive unit tests for ProductService business logic. Test all CRUD operations, validation rules, warranty calculations, and error scenarios using mocked repositories and services.

**Complexity**: High

**Dependencies**: 
- ProductService implemented (Task 9)
- ProductRepository interface created (Task 5)
- CategoryService mocked (created in Task 16)
- WarrantyCalculator implemented (Task 7)
- DTOs created (Task 6)
- JUnit 5, Mockito available
- AssertJ available

**Files to Create/Modify**:
- `src/test/java/com/warranty/service/ProductServiceTest.java` (create)

**Acceptance Criteria**:
1. WHEN `ProductServiceTest.java` is created, IT MUST:
   - Use `@ExtendWith(MockitoExtension.class)` annotation
   - Mock `ProductRepository` and `CategoryService` using `@Mock` annotation
   - Use constructor injection to inject mocked dependencies
   - Include test methods with descriptive names following pattern: `test_<operation>_<scenario>`
2. WHEN `createProduct()` test is executed with valid data, IT MUST:
   - Mock `categoryService.getCategoryById()` to return valid CategoryResponse
   - Mock `productRepository.save()` to return saved Product with generated ID
   - Assert: Result is ProductResponse with correct fields
   - Assert: warrantyEndDate equals purchaseDate + warrantyMonths
   - Assert: warrantyStatus is calculated (ACTIVE, EXPIRING_SOON, or EXPIRED)
   - Test message: "Should create product with valid data"
3. WHEN `createProduct()` test is executed with empty name, IT MUST:
   - Assert: `ValidationException` is thrown with message "Product name cannot be empty"
   - Test message: "Should throw ValidationException when name is empty"
4. WHEN `createProduct()` test is executed with non-existent categoryId, IT MUST:
   - Mock `categoryService.getCategoryById()` to throw `CategoryNotFoundException`
   - Assert: `CategoryNotFoundException` is thrown
   - Test message: "Should throw CategoryNotFoundException when category does not exist"
5. WHEN `createProduct()` test is executed with future purchaseDate, IT MUST:
   - Assert: `ValidationException` is thrown with message "Purchase date cannot be in the future"
   - Test message: "Should throw ValidationException when purchase date is in future"
6. WHEN `createProduct()` test is executed with warrantyMonths <= 0, IT MUST:
   - Test case 1: warrantyMonths = 0
   - Test case 2: warrantyMonths = -5
   - Assert: `ValidationException` is thrown with message "Warranty months must be greater than 0"
   - Test message: "Should throw ValidationException when warranty months is not positive"
7. WHEN `getAllProducts()` test is executed, IT MUST:
   - Mock `productRepository.findAll()` to return list of Product entities
   - Assert: Result is list of ProductResponse with calculated warranty values
   - Test message: "Should return all products"
8. WHEN `getProductById(Long id)` test is executed with valid ID, IT MUST:
   - Mock `productRepository.findById()` to return Optional with Product
   - Assert: Result is ProductResponse with correct values
   - Test message: "Should return product when ID exists"
9. WHEN `getProductById(Long id)` test is executed with invalid ID, IT MUST:
   - Mock `productRepository.findById()` to return empty Optional
   - Assert: `ProductNotFoundException` is thrown with message containing ID
   - Test message: "Should throw ProductNotFoundException when product does not exist"
10. WHEN `updateProduct(Long id, UpdateProductRequest)` test is executed with valid data, IT MUST:
    - Mock existing Product retrieval
    - Mock `categoryService.getCategoryById()` to return valid category
    - Mock `productRepository.save()` to return updated Product
    - Assert: Result has updated name, categoryId, purchaseDate, warrantyMonths
    - Assert: warrantyEndDate is recalculated
    - Test message: "Should update product with valid data"
11. WHEN `updateProduct()` test is executed with non-existent product ID, IT MUST:
    - Mock `productRepository.findById()` to return empty Optional
    - Assert: `ProductNotFoundException` is thrown
    - Test message: "Should throw ProductNotFoundException when updating non-existent product"
12. WHEN `deleteProduct(Long id)` test is executed with valid ID, IT MUST:
    - Mock `productRepository.findById()` to return Optional with Product
    - Mock `productRepository.deleteById()`
    - Verify `productRepository.deleteById(id)` is called
    - Test message: "Should delete product when ID exists"
13. WHEN `deleteProduct(Long id)` test is executed with invalid ID, IT MUST:
    - Mock `productRepository.findById()` to return empty Optional
    - Assert: `ProductNotFoundException` is thrown
    - Test message: "Should throw ProductNotFoundException when deleting non-existent product"
14. WHEN `getProductsExpiringWithin(Integer days)` test is executed, IT MUST:
    - Mock repository query to return filtered products
    - Assert: Result contains only products expiring within specified days
    - Assert: Results are sorted by warrantyEndDate
    - Test message: "Should return products expiring within specified days"
15. WHEN `getProductsExpiringWithin(null)` test is executed, IT MUST:
    - Use default days = 30
    - Test message: "Should use default 30 days when days parameter is null"
16. WHEN `getProductsExpiringWithin(-5)` test is executed, IT MUST:
    - Assert: `ValidationException` is thrown with message "Days parameter must be non-negative"
    - Test message: "Should throw ValidationException when days is negative"

---

- [ ] 18. Write Unit Tests for CategoryController

**Description**: Implement **unit tests** for CategoryController REST endpoints. Test HTTP request/response handling, status codes, and JSON serialization using MockMvc with **mocked service dependencies**.

**Complexity**: Medium

**Dependencies**: 
- CategoryController implemented (Task 12)
- CategoryService interface available (Task 8)
- JUnit 5, MockMvc, Spring Boot Test available

**Files to Create/Modify**:
- `src/test/java/com/warranty/controller/CategoryControllerTest.java` (create)

**Acceptance Criteria**:
1. WHEN `CategoryControllerTest.java` is created, IT MUST:
   - Use `@WebMvcTest(CategoryController.class)` annotation ONLY (NOT @SpringBootTest)
   - Use `@MockBean` to mock `CategoryService` (NOT real service)
   - Inject `MockMvc` for performing HTTP requests
   - NO database integration or real service dependencies
   - Include test methods with descriptive names
2. WHEN `GET /api/categories` endpoint test is executed, IT MUST:
   - Mock `categoryService.getAllCategories()` to return test data
   - Perform HTTP GET request using MockMvc
   - Assert: Response status is 200 OK
   - Assert: Response Content-Type is application/json
   - Assert: Response body is JSON array
   - Assert: Each object has id and name fields
   - Verify: `categoryService.getAllCategories()` was called once
   - Test message: "Should return all categories with 200 OK"
3. WHEN `GET /api/categories/{id}` endpoint test is executed with valid category ID, IT MUST:
   - Mock `categoryService.getCategoryById(1L)` to return CategoryResponse
   - Perform HTTP GET request with valid ID
   - Assert: Response status is 200 OK
   - Assert: Response body is CategoryResponse JSON
   - Assert: Response contains correct id and name
   - Verify: `categoryService.getCategoryById(1L)` was called once
   - Test message: "Should return category by ID with 200 OK"
4. WHEN `GET /api/categories/{id}` endpoint test is executed with non-existent category ID, IT MUST:
   - Mock `categoryService.getCategoryById(999L)` to throw `CategoryNotFoundException`
   - Perform HTTP GET request with invalid ID
   - Assert: Response status is 404 Not Found
   - Assert: Response body is ErrorResponse JSON
   - Assert: Error message contains "not found"
   - Test message: "Should return 404 when category not found"
5. WHEN `GET /api/categories/{id}` endpoint test is executed with invalid (non-numeric) ID, IT MUST:
   - Perform HTTP GET request with non-numeric ID (e.g., "abc")
   - Assert: Response status is 400 Bad Request
   - Assert: Response body is ErrorResponse JSON
   - Test message: "Should return 400 when category ID format is invalid"
6. WHEN response JSON is parsed, EACH category MUST have:
   - `id` field (number)
   - `name` field (string)
   - No additional fields exposed

---

- [ ] 19. Write Unit Tests for ProductController

**Description**: Implement **unit tests** for ProductController REST endpoints. Test all CRUD operations, validation error handling, status codes, and JSON serialization using MockMvc with **mocked service dependencies**.

**Complexity**: High

**Dependencies**: 
- ProductController implemented (Tasks 13-14)
- ProductService interface available (Task 9)
- JUnit 5, MockMvc, Spring Boot Test available

**Files to Create/Modify**:
- `src/test/java/com/warranty/controller/ProductControllerTest.java` (create)

**Acceptance Criteria**:
1. WHEN `ProductControllerTest.java` is created, IT MUST:
   - Use `@WebMvcTest(ProductController.class)` annotation ONLY (NOT @SpringBootTest)
   - Use `@MockBean` to mock `ProductService` (NOT real service)
   - Inject `MockMvc` for performing HTTP requests
   - NO database integration or real service dependencies
   - Include test methods with descriptive names
2. WHEN `POST /api/products` endpoint test is executed with valid request body, IT MUST:
   - Mock `productService.createProduct(request)` to return ProductResponse with ID
   - Perform HTTP POST request with JSON body (name, categoryId, purchaseDate, warrantyMonths)
   - Assert: Response status is 201 Created
   - Assert: Location header contains `/api/products/{newId}`
   - Assert: Response body is ProductResponse JSON
   - Assert: Response includes calculated warrantyEndDate
   - Assert: Response includes calculated warrantyStatus
   - Verify: `productService.createProduct()` was called once with correct parameters
   - Test message: "Should create product and return 201 Created"
3. WHEN `POST /api/products` endpoint test is executed with empty name, IT MUST:
   - Perform HTTP POST request with empty name field
   - Assert: Response status is 400 Bad Request
   - Assert: Response body is ErrorResponse with fieldErrors
   - Assert: fieldErrors contains "name" field with validation message
   - Verify: `productService.createProduct()` was NOT called (validation happens before service)
   - Test message: "Should reject empty product name with 400 Bad Request"
4. WHEN `POST /api/products` endpoint test is executed with invalid categoryId, IT MUST:
   - Mock `productService.createProduct()` to throw `CategoryNotFoundException`
   - Perform HTTP POST request with non-existent categoryId
   - Assert: Response status is 404 Not Found
   - Assert: Response body is ErrorResponse with message "Category with ID {id} not found"
   - Test message: "Should return 404 when category not found"
5. WHEN `POST /api/products` endpoint test is executed with future purchaseDate, IT MUST:
   - Mock `productService.createProduct()` to throw `ValidationException`
   - Perform HTTP POST request with future date
   - Assert: Response status is 400 Bad Request
   - Assert: Response body is ErrorResponse with message "Purchase date cannot be in the future"
   - Test message: "Should reject future purchase date with 400 Bad Request"
6. WHEN `POST /api/products` endpoint test is executed with warrantyMonths <= 0, IT MUST:
   - Mock `productService.createProduct()` to throw `ValidationException` (or handle validation at controller level)
   - Perform HTTP POST request with warrantyMonths = 0 (and -5)
   - Assert: Response status is 400 Bad Request
   - Assert: Response body is ErrorResponse with validation message
   - Test message: "Should reject non-positive warranty months with 400 Bad Request"
7. WHEN `GET /api/products` endpoint test is executed, IT MUST:
   - Mock `productService.getAllProducts()` to return list of ProductResponse
   - Perform HTTP GET request
   - Assert: Response status is 200 OK
   - Assert: Response body is JSON array of ProductResponse objects
   - Assert: Each product has id, name, categoryId, purchaseDate, warrantyMonths, warrantyEndDate, warrantyStatus, category
   - Verify: `productService.getAllProducts()` was called once
   - Test message: "Should return all products with 200 OK"
8. WHEN `GET /api/products/{id}` endpoint test is executed with valid product ID, IT MUST:
   - Mock `productService.getProductById(1L)` to return ProductResponse
   - Perform HTTP GET request with valid ID
   - Assert: Response status is 200 OK
   - Assert: Response body is ProductResponse JSON
   - Assert: Response includes calculated warranty fields
   - Verify: `productService.getProductById(1L)` was called once
   - Test message: "Should return product by ID with 200 OK"
9. WHEN `GET /api/products/{id}` endpoint test is executed with non-existent product ID, IT MUST:
   - Mock `productService.getProductById(999L)` to throw `ProductNotFoundException`
   - Perform HTTP GET request with invalid ID
   - Assert: Response status is 404 Not Found
   - Assert: Response body is ErrorResponse with message "Product with ID {id} not found"
   - Test message: "Should return 404 when product not found"
10. WHEN `PUT /api/products/{id}` endpoint test is executed with valid ID and request body, IT MUST:
    - Mock `productService.updateProduct(1L, request)` to return updated ProductResponse
    - Perform HTTP PUT request with valid product ID and updated data
    - Assert: Response status is 200 OK
    - Assert: Response body is updated ProductResponse
    - Assert: Updated fields match request data
    - Assert: warrantyEndDate is recalculated
    - Verify: `productService.updateProduct()` was called once with correct parameters
    - Test message: "Should update product and return 200 OK"
11. WHEN `PUT /api/products/{id}` endpoint test is executed with non-existent product ID, IT MUST:
    - Mock `productService.updateProduct(999L, request)` to throw `ProductNotFoundException`
    - Perform HTTP PUT request with invalid ID
    - Assert: Response status is 404 Not Found
    - Assert: Response body is ErrorResponse
    - Test message: "Should return 404 when updating non-existent product"
12. WHEN `DELETE /api/products/{id}` endpoint test is executed with valid product ID, IT MUST:
    - Mock `productService.deleteProduct(1L)` to return void
    - Perform HTTP DELETE request with valid ID
    - Assert: Response status is 204 No Content
    - Assert: Response body is empty
    - Verify: `productService.deleteProduct(1L)` was called once
    - Test message: "Should delete product and return 204 No Content"
13. WHEN `DELETE /api/products/{id}` endpoint test is executed with non-existent product ID, IT MUST:
    - Mock `productService.deleteProduct(999L)` to throw `ProductNotFoundException`
    - Perform HTTP DELETE request with invalid ID
    - Assert: Response status is 404 Not Found
    - Test message: "Should return 404 when deleting non-existent product"
14. WHEN `GET /api/products/expiring-soon` endpoint test is executed without days parameter, IT MUST:
    - Mock `productService.getProductsExpiringWithin(30)` to return test products
    - Perform HTTP GET request
    - Assert: Response status is 200 OK
    - Assert: Response body contains mocked products
    - Assert: Results are sorted by warrantyEndDate ascending (as per service mock)
    - Verify: `productService.getProductsExpiringWithin(30)` was called with default value
    - Test message: "Should return products expiring within 30 days when days parameter omitted"
15. WHEN `GET /api/products/expiring-soon?days=60` endpoint test is executed with custom days, IT MUST:
    - Mock `productService.getProductsExpiringWithin(60)` to return test products
    - Perform HTTP GET request with days=60
    - Assert: Response status is 200 OK
    - Assert: Response body contains mocked products
    - Verify: `productService.getProductsExpiringWithin(60)` was called with custom parameter
    - Test message: "Should return products expiring within custom days parameter"
16. WHEN `GET /api/products/expiring-soon?days=-5` endpoint test is executed with negative days, IT MUST:
    - Mock `productService.getProductsExpiringWithin(-5)` to throw `ValidationException`
    - Perform HTTP GET request with days=-5
    - Assert: Response status is 400 Bad Request
    - Assert: Response body is ErrorResponse with message "Days parameter must be non-negative"
    - Test message: "Should return 400 when days parameter is negative"

---

## Phase 6: Integration & Verification

- [ ] 20. Final Checkpoint and Manual API Testing

**Description**: Final verification checkpoint to ensure all components are integrated, all tests pass, and the API is functional. Run complete test suites, verify database integration, and perform manual API testing through Swagger UI.

**Complexity**: Medium

**Dependencies**: 
- All 19 previous tasks completed
- All unit and integration tests implemented

**Files to Create/Modify**:
- No new files created (verification only)
- All application components integrated

**Acceptance Criteria**:
1. WHEN `gradle build` is executed, ALL of the following MUST be true:
   - Build completes successfully with no errors
   - All 16 unit tests pass (WarrantyCalculator, CategoryService, ProductService tests)
   - All 4 integration test suites pass (CategoryController, ProductController tests)
   - Total test count is 16+ (all service and controller tests)
   - Code compiles with no warnings
   - No test failures or skipped tests
2. WHEN `gradle test` is executed, THE following MUST be verified:
   - Test execution summary shows 100% pass rate
   - No failed tests or error stack traces
   - All test messages are logged
3. WHEN application is started with `gradle bootRun`, THE following MUST be verified:
   - Application starts without errors
   - Logs show successful Spring context initialization
   - DataLoader executes successfully (sample categories and products loaded)
   - H2 in-memory database is initialized
   - Tables are created by Hibernate DDL
4. WHEN accessing Swagger UI at `http://localhost:8080/swagger-ui.html`, THE following MUST be verified:
   - Swagger UI page loads successfully
   - All 7 API endpoints are documented and visible:
     - GET /api/categories (retrieve all)
     - GET /api/categories/{id} (retrieve by ID)
     - POST /api/products (create)
     - GET /api/products (retrieve all)
     - GET /api/products/{id} (retrieve by ID)
     - PUT /api/products/{id} (update)
     - DELETE /api/products/{id} (delete)
     - GET /api/products/expiring-soon (expiring soon)
   - Each endpoint shows request parameters, response schema, and status codes
5. WHEN manually testing through Swagger UI:
   - [ ] GET /api/categories returns 200 with list of 5 predefined categories
   - [ ] GET /api/categories/1 returns 200 with single category
   - [ ] GET /api/products returns 200 with sample products (ACTIVE, EXPIRING_SOON, EXPIRED)
   - [ ] GET /api/products/1 returns 200 with product including calculated warranty fields
   - [ ] POST /api/products with valid data returns 201 with new product
   - [ ] POST /api/products with invalid data returns 400 with error details
   - [ ] PUT /api/products/1 with valid data returns 200 with updated product
   - [ ] DELETE /api/products/1 returns 204 No Content
   - [ ] GET /api/products/expiring-soon returns 200 with products expiring in 30 days
   - [ ] GET /api/products/expiring-soon?days=60 returns products expiring in 60 days
6. WHEN verifying database persistence during manual testing, THE following MUST be verified:
   - [ ] Products created via POST endpoint persist and can be retrieved
   - [ ] Products updated via PUT endpoint reflect changes on subsequent GET
   - [ ] Products deleted via DELETE endpoint return 404 on subsequent GET
   - [ ] Sample data loads automatically on application startup
   - [ ] H2 console is accessible at `http://localhost:8080/h2-console` for inspection
7. WHEN verifying error handling:
   - [ ] Invalid JSON returns 400 with error response
   - [ ] Missing required fields return 400 with fieldErrors details
   - [ ] Non-existent resources return 404 with descriptive message
   - [ ] Invalid data types return 400 with type mismatch message
   - [ ] Server errors are caught and return 500 with generic error message
8. WHEN verifying warranty status calculations:
   - [ ] EXPIRED status appears in response when warrantyEndDate is past
   - [ ] EXPIRING_SOON status appears when warrantyEndDate is within 30 days
   - [ ] ACTIVE status appears when warrantyEndDate is beyond 30 days
   - [ ] Status is recalculated correctly after product updates
9. WHEN all tests pass and manual verification is complete, THE following MUST be documented:
   - [ ] All 20 tasks completed successfully
   - [ ] All tests pass with 100% success rate
   - [ ] All API endpoints are functional and tested
   - [ ] Database integration is verified
   - [ ] Error handling is working correctly
   - [ ] Warranty calculations are accurate

---

## Task Dependency Graph

```json
{
  "waves": [
    { "id": 0, "tasks": ["1", "2", "3"] },
    { "id": 1, "tasks": ["4", "5", "6", "7"] },
    { "id": 2, "tasks": ["8", "9", "10", "11"] },
    { "id": 3, "tasks": ["12", "13", "14"] },
    { "id": 4, "tasks": ["15", "16", "17", "18", "19"] },
    { "id": 5, "tasks": ["20"] }
  ]
}
```

---

## Implementation Notes

- Each task builds on previous tasks; execute in order within phases
- Tasks in the same wave (dependency graph) can be parallelized if needed
- Run `gradle test` after completing each phase to verify integration
- All DTOs must include proper Lombok annotations for clean code
- Service layer handles all validation; controller layer is thin
- WarrantyCalculator is stateless and reusable across components
- GlobalExceptionHandler centralizes error handling across all endpoints
- Unit tests use mocks; integration tests use real database and MockMvc
- All tests must pass before proceeding to next phase
- Manual API testing through Swagger UI should be performed after all tests pass

---

## Summary

This 20-task implementation plan provides a complete, incremental development path for the Warranty Tracker REST API MVP using Spring Boot 3, Spring Data JPA, and H2 in-memory database. The tasks are organized into 6 phases with clear dependencies, ensuring each step validates core functionality. All acceptance criteria are specific and testable, enabling confident progress tracking and verification at each checkpoint.

