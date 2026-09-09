# Warranty Tracker REST API - Design Specification

## Overview

The Warranty Tracker REST API is a Spring Boot 3 application that provides REST endpoints for managing product categories and tracking warranty expiration dates. The design follows a **3-tier layered architecture** (Controller → Service → Repository) with clear separation of concerns. The application uses Spring Data JPA for persistence with an H2 in-memory database, ensuring a lightweight, zero-configuration experience suitable for an MVP portfolio project.

### Key Design Philosophy

- **Simplicity First**: Minimal abstractions; no over-engineering for an MVP
- **Separation of Concerns**: Clear boundaries between HTTP handling, business logic, and data access
- **DTO Pattern**: DTOs maintain a contract independent of database schema
- **Calculated Fields**: Warranty status and end date computed on-demand, never persisted
- **Fail-Fast Validation**: Input validation at service layer before persistence
- **Global Error Handling**: Centralized exception handling with consistent error responses

---

## Architecture

### 3-Tier Layered Architecture

```
┌─────────────────────────────────────────┐
│         REST Controllers                │  (HTTP Layer)
│    • Handle requests/responses          │
│    • Route to services                  │
└────────────────┬────────────────────────┘
                 │
┌────────────────▼────────────────────────┐
│       Service Layer                     │  (Business Logic)
│    • Validation logic                   │
│    • Warranty calculations              │
│    • Transaction boundaries             │
└────────────────┬────────────────────────┘
                 │
┌────────────────▼────────────────────────┐
│      Data Access (Repositories)         │  (Persistence)
│    • CRUD operations                    │
│    • Custom query methods               │
└─────────────────────────────────────────┘
                 │
         ┌───────▼───────┐
         │  H2 Database  │
         └───────────────┘
```

### Package Structure

```
src/main/java/com/warranty/
├── config/                          # Application configuration
│   ├── H2Config.java               # H2 database configuration
│   └── OpenApiConfig.java          # OpenAPI/Swagger configuration
├── controller/                      # REST controllers
│   ├── CategoryController.java
│   └── ProductController.java
├── service/                         # Business logic
│   ├── CategoryService.java
│   ├── ProductService.java
│   └── WarrantyCalculator.java     # Warranty status computation
├── repository/                      # Data access
│   ├── CategoryRepository.java
│   └── ProductRepository.java
├── entity/                          # JPA entities
│   ├── Category.java
│   └── Product.java
├── dto/                             # Data transfer objects
│   ├── request/
│   │   ├── CreateProductRequest.java
│   │   └── UpdateProductRequest.java
│   ├── response/
│   │   ├── CategoryResponse.java
│   │   ├── ProductResponse.java
│   │   └── ErrorResponse.java
│   └── internal/
│       └── WarrantyStatus.java     # Enum
├── exception/                       # Custom exceptions
│   ├── CategoryNotFoundException.java
│   ├── ProductNotFoundException.java
│   ├── ValidationException.java
│   └── GlobalExceptionHandler.java
├── initialization/                  # Startup data loading
│   └── DataLoader.java             # Sample data initialization
└── WarrantyTrackerApiApplication.java
```

---

## Domain Model & JPA Entities

### Category Entity

```
Category
├── id (Long)              [Primary Key, Auto-Generated]
├── name (String)          [Unique, Not Null]
└── products (Set<Product>) [One-to-Many Relationship]

Constraints:
  • @Id @GeneratedValue
  • @Column(unique = true, nullable = false)
  • Indexed for fast lookups
```

**Rationale**: Categories are simple reference data with minimal behavior. The one-to-many relationship enables cascading (optional: ON_DELETE/ON_UPDATE). No audit fields are needed for this MVP.

### Product Entity

```
Product
├── id (Long)              [Primary Key, Auto-Generated]
├── name (String)          [Not Null, Not Empty]
├── purchaseDate (LocalDate) [Not Null, Not Future]
├── warrantyMonths (Integer) [Not Null, > 0]
├── category (Category)    [Foreign Key, Not Null, Many-to-One]
└── categoryId (Long)      [Derived from category relation]

Calculated Fields (NOT stored in database):
├── warrantyEndDate        [= purchaseDate + warrantyMonths]
└── warrantyStatus         [ACTIVE | EXPIRING_SOON | EXPIRED]

Constraints:
  • @ManyToOne(fetch = LAZY)
  • @JoinColumn(name = "category_id", nullable = false)
  • @NotNull, @NotBlank, @Positive
```

**Rationale**:
- **warrantyEndDate**: Stored in database as a derived column (`purchaseDate + warrantyMonths`) or calculated on-demand. For MVP simplicity, we calculate on-demand (no storage), eliminating sync issues.
- **warrantyStatus**: Always calculated at request time based on current date; never stored, ensuring accuracy.
- **categoryId**: Accessible via `product.getCategory().getId()` or stored as a denormalized field depending on query patterns.

### Relationship: One-to-Many (Category → Products)

```sql
Category (1)
  │
  ├── oneToMany ──────────→ Product (*)
  │   @OneToMany(mappedBy = "category")
  │   private Set<Product> products;
```

**Benefits**:
- Supports bulk queries like "all products in a category"
- Enables optional cascading delete (if a category is removed, its products are deleted)
- For MVP, cascading is disabled by default (safer); explicit product deletion is required

---

## Request and Response DTOs

### Request DTOs

#### CreateProductRequest
```
POST /api/products body:
{
  "name": "Microwave",
  "categoryId": 1,
  "purchaseDate": "2023-06-15",
  "warrantyMonths": 24
}

Fields:
  • name: String (required, non-empty)
  • categoryId: Long (required, must reference valid category)
  • purchaseDate: LocalDate (required, not in future)
  • warrantyMonths: Integer (required, > 0)

Note: NO warrantyEndDate or warrantyStatus; calculated server-side.
```

#### UpdateProductRequest
```
PUT /api/products/{id} body:
{
  "name": "Microwave Oven",
  "categoryId": 1,
  "purchaseDate": "2023-06-15",
  "warrantyMonths": 36
}

Identical to CreateProductRequest.
Note: warrantyEndDate is NOT updatable; always recalculated.
```

### Response DTOs

#### CategoryResponse
```json
{
  "id": 1,
  "name": "Kitchen Appliances"
}
```

#### ProductResponse
```json
{
  "id": 5,
  "name": "Microwave",
  "categoryId": 1,
  "purchaseDate": "2023-06-15",
  "warrantyMonths": 24,
  "warrantyEndDate": "2025-06-15",
  "warrantyStatus": "ACTIVE",
  "category": {
    "id": 1,
    "name": "Kitchen Appliances"
  }
}
```

**Fields**:
- `warrantyEndDate`: Calculated as `purchaseDate + warrantyMonths`
- `warrantyStatus`: ACTIVE | EXPIRING_SOON | EXPIRED (calculated at request time)
- `category`: Nested CategoryResponse object

#### ErrorResponse
```json
{
  "timestamp": "2024-01-15T14:30:00Z",
  "status": 400,
  "error": "Validation Error",
  "message": "Product name cannot be empty",
  "path": "/api/products",
  "fieldErrors": [
    {
      "field": "name",
      "message": "must not be empty"
    }
  ]
}
```

**Error Response Structure**:
- `timestamp`: When the error occurred
- `status`: HTTP status code
- `error`: Short error classification
- `message`: Human-readable error description
- `path`: Request path
- `fieldErrors`: Array of field-level validation errors (for 400 errors)

---

## REST API Endpoints & HTTP Status Codes

### Category Endpoints

#### 1. Retrieve All Categories
```
GET /api/categories

Response (200 OK):
[
  { "id": 1, "name": "Kitchen Appliances" },
  { "id": 2, "name": "Bedroom" },
  ...
]

Empty Response (200 OK):
[]

Status Codes:
  • 200 OK: Success
```

**Behavior**: Returns all predefined categories, or an empty array if none exist. For this MVP, categories are always present (loaded at startup).

#### 2. Retrieve Category by ID
```
GET /api/categories/{id}

Response (200 OK):
{
  "id": 1,
  "name": "Kitchen Appliances"
}

Status Codes:
  • 200 OK: Category found
  • 404 Not Found: Category not found
  • 400 Bad Request: Invalid ID format (non-numeric)
```

---

### Product Endpoints

#### 1. Create Product
```
POST /api/products
Content-Type: application/json

Request Body:
{
  "name": "Microwave",
  "categoryId": 1,
  "purchaseDate": "2023-06-15",
  "warrantyMonths": 24
}

Response (201 Created):
{
  "id": 5,
  "name": "Microwave",
  "categoryId": 1,
  "purchaseDate": "2023-06-15",
  "warrantyMonths": 24,
  "warrantyEndDate": "2025-06-15",
  "warrantyStatus": "ACTIVE",
  "category": { "id": 1, "name": "Kitchen Appliances" }
}

Status Codes:
  • 201 Created: Product created successfully
  • 400 Bad Request: Validation failure (see below)
  • 404 Not Found: Category not found

Validation Failures (400):
  • name is empty or null → "Product name cannot be empty"
  • categoryId does not exist → "Category with ID X not found"
  • purchaseDate is in future → "Purchase date cannot be in the future"
  • warrantyMonths <= 0 → "Warranty months must be greater than 0"
```

**Flow**:
1. Controller validates request structure (@Valid)
2. Service validates business rules
3. Service calculates warrantyEndDate = purchaseDate + warrantyMonths
4. Service saves Product entity
5. Service loads category data and constructs ProductResponse
6. Controller returns 201 with ProductResponse in Location header

#### 2. Retrieve All Products
```
GET /api/products

Response (200 OK):
[
  {
    "id": 1,
    "name": "Refrigerator",
    "categoryId": 1,
    "purchaseDate": "2022-01-10",
    "warrantyMonths": 12,
    "warrantyEndDate": "2023-01-10",
    "warrantyStatus": "EXPIRED",
    "category": { "id": 1, "name": "Kitchen Appliances" }
  },
  {
    "id": 2,
    "name": "Laptop",
    "categoryId": 4,
    "purchaseDate": "2024-06-15",
    "warrantyMonths": 24,
    "warrantyEndDate": "2026-06-15",
    "warrantyStatus": "ACTIVE",
    "category": { "id": 4, "name": "Electronics" }
  }
]

Empty Response (200 OK):
[]

Status Codes:
  • 200 OK: Success (even if empty)
```

**Behavior**: Returns all products with calculated warrantyEndDate and warrantyStatus. Each product includes nested category information.

#### 3. Retrieve Product by ID
```
GET /api/products/{id}

Response (200 OK):
{
  "id": 1,
  "name": "Refrigerator",
  "categoryId": 1,
  "purchaseDate": "2022-01-10",
  "warrantyMonths": 12,
  "warrantyEndDate": "2023-01-10",
  "warrantyStatus": "EXPIRED",
  "category": { "id": 1, "name": "Kitchen Appliances" }
}

Status Codes:
  • 200 OK: Product found
  • 404 Not Found: "Product with ID X not found"
  • 400 Bad Request: "Invalid product ID format"
```

#### 4. Update Product
```
PUT /api/products/{id}
Content-Type: application/json

Request Body:
{
  "name": "Refrigerator Pro",
  "categoryId": 1,
  "purchaseDate": "2022-01-10",
  "warrantyMonths": 36
}

Response (200 OK):
{
  "id": 1,
  "name": "Refrigerator Pro",
  "categoryId": 1,
  "purchaseDate": "2022-01-10",
  "warrantyMonths": 36,
  "warrantyEndDate": "2025-01-10",
  "warrantyStatus": "EXPIRING_SOON",
  "category": { "id": 1, "name": "Kitchen Appliances" }
}

Status Codes:
  • 200 OK: Product updated successfully
  • 400 Bad Request: Validation failure (same as CREATE)
  • 404 Not Found: Product or category not found
```

**Behavior**:
- Validates all input fields using same rules as CREATE
- warrantyEndDate is NOT updatable; automatically recalculated
- Returns updated product with new calculated values

#### 5. Delete Product
```
DELETE /api/products/{id}

Response (204 No Content):
(empty body)

Status Codes:
  • 204 No Content: Product deleted successfully
  • 404 Not Found: Product not found
```

**Behavior**: Deletes the product; subsequent GET requests for this ID return 404.

#### 6. Retrieve Products Expiring Soon
```
GET /api/products/expiring-soon?days=30

Query Parameters:
  • days: Integer (optional, default = 30)
    - Must be >= 0; negative values return 400
    - Represents days from today

Response (200 OK):
[
  {
    "id": 2,
    "name": "Laptop",
    "categoryId": 4,
    "purchaseDate": "2024-06-15",
    "warrantyMonths": 24,
    "warrantyEndDate": "2026-06-15",
    "warrantyStatus": "EXPIRING_SOON",
    "category": { "id": 4, "name": "Electronics" }
  }
]

Empty Response (200 OK):
[]

Status Codes:
  • 200 OK: Success
  • 400 Bad Request: "Days parameter must be non-negative"
```

**Filtering Logic**:
- Returns products where: `today <= warrantyEndDate <= today + N days`
- Excludes EXPIRED products (those where `today > warrantyEndDate`)
- Excludes ACTIVE products expiring beyond N days
- Default N = 30 days

---

## Product Validation and Error Handling

### Validation Rules

#### Input Field Validations

1. **Product Name**
   - Must not be null
   - Must not be empty or whitespace-only
   - Max length: 255 characters (database constraint)

2. **Category ID**
   - Must not be null
   - Must reference an existing Category in the database
   - Returns 404 if category not found

3. **Purchase Date**
   - Must not be null
   - Must not be a future date (today or earlier only)
   - Format: ISO 8601 (YYYY-MM-DD)

4. **Warranty Months**
   - Must not be null
   - Must be > 0 (strictly positive)
   - Typical range: 1–120 months

### Validation Flow

```
Controller (@Valid)
     ↓
[Request body validated by Bean Validation constraints]
     ↓
Service Layer
     ↓
[Business rule validation]
     ↓
Repository/Database
```

### Exception Hierarchy

```
Exception
├── CustomBusinessException (extends RuntimeException)
│   ├── ValidationException
│   │   └── Thrown when business rule validation fails
│   ├── CategoryNotFoundException
│   │   └── Thrown when referenced category doesn't exist
│   └── ProductNotFoundException
│       └── Thrown when product doesn't exist
```

### Global Exception Handler

**Component**: `GlobalExceptionHandler` (annotated with `@RestControllerAdvice`)

**Responsibilities**:
- Catches all exceptions from controllers
- Converts exceptions to standardized ErrorResponse
- Returns appropriate HTTP status codes
- Logs errors appropriately

**Handled Exceptions**:

| Exception | HTTP Status | Message |
|-----------|-------------|---------|
| ValidationException | 400 | Validation error details |
| MethodArgumentNotValidException | 400 | Field validation errors |
| CategoryNotFoundException | 404 | Category not found message |
| ProductNotFoundException | 404 | Product not found message |
| HttpMessageNotReadableException | 400 | Malformed JSON |
| IllegalArgumentException | 400 | Invalid parameter format |
| Exception (generic) | 500 | Internal server error |

**ErrorResponse Format**:
```json
{
  "timestamp": "2024-01-15T14:30:00Z",
  "status": 400,
  "error": "Validation Error",
  "message": "Product name cannot be empty",
  "path": "/api/products",
  "fieldErrors": [
    {
      "field": "name",
      "message": "must not be empty"
    }
  ]
}
```

---

## Warranty Calculation and Dynamic Status Logic

### WarrantyCalculator Component

**Purpose**: Encapsulates warranty status calculation logic; keeps it testable and reusable.

**Responsibility**: Given a warranty end date and current date, determine the warranty status.

**Public API**:
```java
public class WarrantyCalculator {
  public static WarrantyStatus calculateStatus(LocalDate warrantyEndDate, LocalDate today)
}
```

### Warranty Status Determination

**Status Rules** (immutable, non-configurable):

```
1. EXPIRED
   Condition: today > warrantyEndDate
   Meaning: Warranty has already expired

2. EXPIRING_SOON
   Condition: today <= warrantyEndDate AND warrantyEndDate <= today + 30 days
   Meaning: Warranty expires within 30 days (including today)

3. ACTIVE
   Condition: warrantyEndDate > today + 30 days
   Meaning: Warranty is valid for more than 30 days
```

**Example Timeline**:
```
Today = 2024-06-15

Warranty End Date = 2024-01-15
  → today > warrantyEndDate
  → Status: EXPIRED

Warranty End Date = 2024-07-10
  → today <= warrantyEndDate (2024-06-15 <= 2024-07-10)
  → warrantyEndDate <= today + 30 days (2024-07-10 <= 2024-07-15)
  → Status: EXPIRING_SOON

Warranty End Date = 2025-06-15
  → warrantyEndDate > today + 30 days (2025-06-15 > 2024-07-15)
  → Status: ACTIVE
```

### WarrantyStatus Enum

```java
public enum WarrantyStatus {
  ACTIVE,
  EXPIRING_SOON,
  EXPIRED
}
```

### Warranty End Date Calculation

**Formula**:
```
warrantyEndDate = purchaseDate + warrantyMonths (months)
```

**Example**:
```
purchaseDate = 2023-06-15
warrantyMonths = 24

warrantyEndDate = 2023-06-15 + 24 months = 2025-06-15
```

**Implementation**: Use `LocalDate.plusMonths(warrantyMonths)` from Java 8+ Date API.

### When Status is Calculated

- **Every Request**: Status is calculated fresh at request time, never cached or stored
- **Ensures Accuracy**: Today's date changes daily; status dynamically reflects current reality
- **No Synchronization Issues**: No need to update database when dates pass thresholds

---

## Expiring-Soon Query Behavior

### Endpoint: GET /api/products/expiring-soon?days=N

### Query Logic

```
Base Criteria:
  today <= warrantyEndDate <= today + N days

Where:
  • today = LocalDate.now()
  • N = query parameter "days" (default = 30)
  • warrantyEndDate = purchaseDate + warrantyMonths
```

**SQL Equivalent**:
```sql
SELECT * FROM Product
WHERE warrantyEndDate >= TODAY()
  AND warrantyEndDate <= DATEADD(day, ?, TODAY())
ORDER BY warrantyEndDate ASC;
```

### Parameter Validation

1. **If days parameter is missing**: Use default 30 days
2. **If days is negative**: Return 400 Bad Request with message "Days parameter must be non-negative"
3. **If days is zero**: Return products expiring today only
4. **If days is positive**: Return products expiring within N days from today

### Response Includes Calculated Status

Each product in the response includes:
- `warrantyStatus`: ACTIVE | EXPIRING_SOON | EXPIRED (calculated on-demand)
- Typically all returned products have status EXPIRING_SOON, unless edge cases occur

### Examples

**Example 1: Default behavior (30 days)**
```
GET /api/products/expiring-soon

Returns: All products expiring in next 30 days from today
```

**Example 2: Custom range (60 days)**
```
GET /api/products/expiring-soon?days=60

Returns: All products expiring in next 60 days from today
```

**Example 3: Today only**
```
GET /api/products/expiring-soon?days=0

Returns: All products expiring today (warrantyEndDate == today)
```

---

## Startup Sample Data Initialization

### DataLoader Component

**Component**: `DataLoader` (implements `ApplicationRunner` or uses `@PostConstruct`)

**Timing**: Executes when Spring application context is fully initialized (startup).

**Responsibilities**:
1. Create and persist predefined categories
2. Create and persist sample products representing all warranty statuses

### Predefined Categories

The following five categories are always loaded at startup:

```
1. Kitchen Appliances
2. Bedroom
3. Beauty
4. Electronics
5. Home Appliances
```

**Approach**: Simple INSERT-if-not-exists logic (or just load every time; H2 is reset anyway).

### Sample Products

Sample products must represent all three warranty statuses:

#### 1. ACTIVE Warranty Product
```
Name: "Laptop"
Category: Electronics
PurchaseDate: 2024-06-01
WarrantyMonths: 24
WarrantyEndDate: 2026-06-01 (> today + 30 days)
Status: ACTIVE
```

#### 2. EXPIRING_SOON Warranty Product
```
Name: "Microwave"
Category: Kitchen Appliances
PurchaseDate: 2024-01-15
WarrantyMonths: 18
WarrantyEndDate: 2025-07-15 (within next 30 days)
Status: EXPIRING_SOON
```

#### 3. EXPIRED Warranty Product
```
Name: "Refrigerator"
Category: Kitchen Appliances
PurchaseDate: 2022-01-10
WarrantyMonths: 24
WarrantyEndDate: 2024-01-10 (in the past)
Status: EXPIRED
```

**Date Calculation**:
- Use `LocalDate.now()` as reference
- ACTIVE: `now().plusMonths(36)` (36 months ahead)
- EXPIRING_SOON: `now().plusDays(15)` (expiring in 15 days)
- EXPIRED: `now().minusMonths(12)` (expired 12 months ago)

This ensures predictable demo behavior regardless of when the application starts.

### Implementation Pattern

```
1. Create CategoryRepository.saveAll([5 categories])
2. Create ProductRepository.saveAll([3 sample products])
3. Flush and commit
```

**Approach**: Keep it simple; no duplicate detection. Each application restart resets H2 and reloads sample data.

---

## H2 In-Memory Database Configuration

### Spring Boot Auto-Configuration

**Default Behavior**: Spring Boot auto-configures H2 when:
1. H2 dependency is on classpath
2. DataSource is not explicitly configured
3. JPA is on classpath

**Files**: `application.properties` or `application.yml`

### Configuration Properties

```properties
# H2 Database Configuration
spring.datasource.url=jdbc:h2:mem:warranty_db
spring.datasource.driverClassName=org.h2.Driver
spring.datasource.username=sa
spring.datasource.password=

# H2 Console (optional; for dev/testing)
spring.h2.console.enabled=true
spring.h2.console.path=/h2-console

# JPA/Hibernate Configuration
spring.jpa.database-platform=org.hibernate.dialect.H2Dialect
spring.jpa.hibernate.ddl-auto=create-drop

# Show SQL (optional; for debugging)
spring.jpa.show-sql=false
spring.jpa.properties.hibernate.format_sql=true
```

### Key Configuration Decisions

1. **in-memory database**: `jdbc:h2:mem:warranty_db`
   - Data is lost on application shutdown
   - Ideal for MVP testing
   - No file system I/O

2. **ddl-auto=create-drop**
   - Automatically creates tables from entities on startup
   - Drops tables on shutdown
   - Combined with DataLoader, ensures fresh state on each start

3. **H2 Console** (optional)
   - Accessible at `/h2-console` during development
   - Allows ad-hoc SQL queries for testing
   - **Disable in production**

### Entity DDL (Auto-Generated)

Hibernate generates the following tables from JPA entities:

```sql
CREATE TABLE category (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  name VARCHAR(255) NOT NULL UNIQUE
);

CREATE TABLE product (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  name VARCHAR(255) NOT NULL,
  purchase_date DATE NOT NULL,
  warranty_months INTEGER NOT NULL,
  category_id BIGINT NOT NULL,
  FOREIGN KEY (category_id) REFERENCES category(id)
);

CREATE INDEX idx_product_category ON product(category_id);
CREATE INDEX idx_product_warranty_end ON product(purchase_date, warranty_months);
```

### No Persistence Beyond Runtime

```
Application Starts
     ↓
H2 created in memory
     ↓
Schema created from entities
     ↓
Sample data loaded
     ↓
Application Running (data persists in memory)
     ↓
Application Stops
     ↓
H2 database destroyed (all data lost)
```

---

## OpenAPI / Swagger UI Configuration

### Springdoc OpenAPI Integration

**Library**: `springdoc-openapi-starter-webmvc-ui`

**What It Does**: Automatically generates OpenAPI 3.1 specification from Spring Boot code and annotations.

### Configuration

**File**: `application.properties` or `application.yml`

```properties
# OpenAPI/Swagger Configuration
springdoc.api-docs.path=/v3/api-docs
springdoc.swagger-ui.path=/swagger-ui.html
springdoc.swagger-ui.operations-sorter=method
springdoc.swagger-ui.tags-sorter=alpha

# Optional: Customize API title and description
springdoc.api-docs.title=Warranty Tracker REST API
springdoc.api-docs.description=API for managing product categories and tracking warranty expiration
springdoc.api-docs.version=1.0.0
```

### Automatic Documentation

Springdoc scans the application and generates:

1. **Endpoint Definitions**: All `@RestController` endpoints
2. **Request/Response Schemas**: From DTOs and entities
3. **Status Codes**: From `@ApiResponse` annotations
4. **Path/Query Parameters**: From `@PathVariable` and `@RequestParam`

### Manual Annotations (Optional but Recommended)

Add clarity with annotations:

```java
@RestController
@RequestMapping("/api/products")
public class ProductController {

  @GetMapping
  @Operation(summary = "Retrieve all products", 
             description = "Returns a list of all products with calculated warranty status")
  @ApiResponse(responseCode = "200", 
               description = "Products retrieved successfully")
  public ResponseEntity<List<ProductResponse>> getAllProducts() {
    ...
  }

  @PostMapping
  @Operation(summary = "Create a new product")
  @ApiResponse(responseCode = "201", description = "Product created successfully")
  @ApiResponse(responseCode = "400", description = "Invalid product data")
  @ApiResponse(responseCode = "404", description = "Category not found")
  public ResponseEntity<ProductResponse> createProduct(@Valid @RequestBody CreateProductRequest request) {
    ...
  }
}
```

### Accessing Documentation

- **OpenAPI JSON**: `http://localhost:8080/v3/api-docs`
- **Swagger UI**: `http://localhost:8080/swagger-ui.html`

**Swagger UI Features**:
- Interactive endpoint documentation
- "Try it out" button to send test requests
- Request/response examples
- Schema visualization

---

## Unit Testing Strategy Using JUnit and Mockito

### Testing Layers

#### 1. Service Tests
**Focus**: Verify business logic, validation, and warranty calculations.

**Approach**: Mock repositories using Mockito; inject mocks into service.

**Examples**:
- `ProductService.createProduct()` validates input and calls repository
- `WarrantyCalculator.calculateStatus()` returns correct status for date ranges
- `ProductService.getExpiringProducts(days)` filters and calculates correctly

**Sample Test**:
```java
@Test
void testCreateProductWithValidData() {
  // Arrange
  CreateProductRequest request = new CreateProductRequest(
    "Laptop", 1L, LocalDate.of(2024, 6, 15), 24
  );
  when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
  when(productRepository.save(any())).thenReturn(savedProduct);

  // Act
  ProductResponse response = productService.createProduct(request);

  // Assert
  assertEquals("Laptop", response.getName());
  assertEquals(LocalDate.of(2026, 6, 15), response.getWarrantyEndDate());
}
```

#### 2. Controller Tests
**Focus**: Verify endpoints return correct HTTP status and response structure.

**Approach**: Use `@WebMvcTest` with MockMvc; mock service layer.

**Examples**:
- `POST /api/products` returns 201 with ProductResponse
- `GET /api/products/999` returns 404
- `POST /api/products` with invalid data returns 400 with error details

**Sample Test**:
```java
@Test
void testCreateProductReturns201() throws Exception {
  mockMvc.perform(post("/api/products")
    .contentType(MediaType.APPLICATION_JSON)
    .content(json)
  )
  .andExpect(status().isCreated())
  .andExpect(jsonPath("$.id").exists())
  .andExpect(jsonPath("$.warrantyStatus").value("ACTIVE"));
}
```

### Warranty Calculator Unit Tests

**Focus**: Test all three status conditions with various date scenarios.

```
Test Case 1: EXPIRED
  warrantyEndDate: 2024-01-10
  today: 2024-06-15
  Expected: EXPIRED

Test Case 2: EXPIRING_SOON (at boundary)
  warrantyEndDate: today + 15 days
  today: today
  Expected: EXPIRING_SOON

Test Case 3: ACTIVE
  warrantyEndDate: today + 60 days
  today: today
  Expected: ACTIVE
```

### Validation Unit Tests

```
Test: Empty product name → ValidationException
Test: Non-existent category ID → CategoryNotFoundException (404)
Test: Future purchase date → ValidationException
Test: Zero or negative warranty months → ValidationException
```

### Test Coverage Goals

- **Service Layer**: 80%+ coverage (core business logic)
- **Controller Layer**: 70%+ coverage (endpoint behavior)
- **WarrantyCalculator**: 100% coverage (simple, critical logic)

### Testing Tools and Dependencies

- **JUnit 5**: Test framework
- **Mockito**: Mocking library
- **AssertJ**: Fluent assertions (optional but recommended)
- **Spring Test**: `@WebMvcTest`, `@DataJpaTest`, MockMvc

### Test File Structure

```
src/test/java/com/warranty/
├── controller/
│   ├── ProductControllerTest.java
│   └── CategoryControllerTest.java
├── service/
│   ├── ProductServiceTest.java
│   ├── CategoryServiceTest.java
│   └── WarrantyCalculatorTest.java
└── integration/
    └── ApiIntegrationTest.java (optional; full stack)
```

---

## Design Principles & Patterns

### 1. Separation of Concerns

**Controller Layer**: HTTP handling only
- Parse requests, invoke services, return responses
- No business logic

**Service Layer**: Business logic
- Validation, calculations, orchestration
- No HTTP or database knowledge (except exception handling)

**Repository Layer**: Data access only
- CRUD and query methods
- Delegates to Spring Data JPA

**Benefit**: Easy to test, modify, and understand. Each layer has single responsibility.

### 2. DTO Pattern

**Benefits**:
- **Contract Independence**: API contract separate from database schema
- **Version Safety**: Schema changes don't break client contracts
- **Calculated Fields**: warrantyEndDate and warrantyStatus computed server-side
- **Security**: Avoid exposing sensitive fields

### 3. Validation at Service Layer

**Why**: Service layer knows business rules (e.g., "warranty months must be positive").
Controller validates structure (@Valid); Service validates business rules.

### 4. Warranty Calculation Isolation

**Why**: WarrantyCalculator is a pure, testable component. Easy to reuse and test independently.

### 5. Immutable Status Rules

**Why**: Warranty status rules (EXPIRED, EXPIRING_SOON, ACTIVE) are baked into code, not configurable. Prevents logic drift.

### 6. No Caching

**Why**: MVP simplicity. H2 in-memory is fast; no optimization needed. Warranty status changes daily; caching would be stale.

### 7. Global Exception Handling

**Why**: Consistent error responses across all endpoints. Centralized logging and error formatting.

### 8. Calculated Fields, Never Persisted

**Why**: warrantyEndDate and warrantyStatus are derived from other fields. Calculating on-demand avoids sync issues and ensures accuracy.

---

## Interaction Flow: Create Product (Detailed Example)

```
1. Client Request
   POST /api/products
   Content-Type: application/json
   {
     "name": "Microwave",
     "categoryId": 1,
     "purchaseDate": "2023-06-15",
     "warrantyMonths": 24
   }

2. ProductController.createProduct()
   ├─ @Valid annotation triggers Bean Validation
   │  └─ Validates @NotBlank, @NotNull constraints on fields
   ├─ Calls productService.createProduct(request)
   └─ Returns ResponseEntity with 201 status

3. ProductService.createProduct()
   ├─ Validate business rules:
   │  ├─ Check name is not empty (redundant with @Valid, but defensive)
   │  ├─ Check categoryId exists → repository.findById(1)
   │  │  └─ If not found: throw CategoryNotFoundException
   │  ├─ Check purchaseDate is not future
   │  └─ Check warrantyMonths > 0
   ├─ Calculate warrantyEndDate
   │  └─ warrantyEndDate = purchaseDate.plusMonths(warrantyMonths)
   │  └─ = 2023-06-15 + 24 months = 2025-06-15
   ├─ Create Product entity
   ├─ Save via repository.save(product)
   ├─ Refresh/retrieve saved entity (with generated ID)
   └─ Construct ProductResponse
      ├─ id: 5 (generated by database)
      ├─ name: "Microwave"
      ├─ warrantyEndDate: 2025-06-15 (calculated)
      ├─ warrantyStatus: WarrantyCalculator.calculateStatus(
      │   warrantyEndDate=2025-06-15, today=2024-06-15
      │ ) = ACTIVE
      └─ category: CategoryResponse(1, "Kitchen Appliances")

4. ProductController returns response
   HTTP/1.1 201 Created
   Location: /api/products/5
   Content-Type: application/json
   {
     "id": 5,
     "name": "Microwave",
     "categoryId": 1,
     "purchaseDate": "2023-06-15",
     "warrantyMonths": 24,
     "warrantyEndDate": "2025-06-15",
     "warrantyStatus": "ACTIVE",
     "category": {"id": 1, "name": "Kitchen Appliances"}
   }

5. Client receives response
   ├─ Status: 201 (created)
   ├─ Body: Product with calculated fields
   └─ Location header: /api/products/5

Error Scenarios:

A. Invalid Category ID
   Service throws CategoryNotFoundException
   → GlobalExceptionHandler catches
   → Returns 404 with message: "Category with ID 99 not found"

B. Invalid Name (empty)
   @Valid catches @NotBlank violation
   → Controller receives MethodArgumentNotValidException
   → GlobalExceptionHandler catches
   → Returns 400 with fieldErrors

C. Future Purchase Date
   Service throws ValidationException
   → GlobalExceptionHandler catches
   → Returns 400 with message: "Purchase date cannot be in the future"
```

---

## Key Design Decisions & Rationales

| Decision | Rationale |
|----------|-----------|
| **3-Tier Layered Architecture** | Clear separation of concerns; easy to test; standard Spring pattern |
| **DTOs for API Contracts** | Decouples API from database schema; enables calculated fields; security |
| **Warranty Status Calculated On-Demand** | Ensures accuracy; no sync issues; status reflects current date automatically |
| **Service Layer Validation** | Business rules live where they belong; reusable across endpoints |
| **Global Exception Handler** | Consistent error responses; centralized logging; professional UX |
| **H2 In-Memory Database** | MVP requirements; zero configuration; data reset on each start |
| **Springdoc OpenAPI** | Automatic documentation; interactive Swagger UI; minimal configuration |
| **No Caching** | MVP scope; H2 is fast; warranty status is time-sensitive |
| **No Authentication/Authorization** | MVP scope; simplifies design; adds only when needed |
| **No Microservices/Async** | MVP scope; monolithic is simpler; scale up later if needed |
| **Simple Data Loader** | No duplicate detection; reset-on-start model fits MVP |
| **Simple Unit Testing** | Straightforward JUnit and Mockito testing of services and controllers; no complex testing frameworks |

---

## Constraints & Limitations (MVP)

1. **No Authentication**: All endpoints are publicly accessible.
2. **No Authorization**: No role-based access control.
3. **No Pagination**: All queries return complete result sets.
4. **No Sorting**: Results returned in database order.
5. **No Search/Filtering**: Only expiring-soon filtering supported.
6. **No Transactions**: Service-level transaction handling; no complex TX logic.
7. **No Caching**: All queries hit the database.
8. **No Performance Optimization**: Indexes minimal; adequate for small datasets.
9. **Data Loss on Restart**: H2 in-memory; no persistence across restarts.
10. **Single Instance**: No clustering or replication.

---

## Implementation Guidelines

### Code Organization

- Follow package structure strictly for maintainability
- Use dependency injection (constructor-based); avoid service locator pattern
- Keep methods focused; aim for single responsibility
- Use meaningful names; avoid abbreviations

### Exception Handling

- Create custom exceptions for business errors (CategoryNotFoundException, etc.)
- Throw checked exceptions rarely; prefer unchecked (RuntimeException subclasses)
- Always include context in exception messages (e.g., which ID was not found?)

### DTO Mapping

- Use manual mapping (simple project); MapStruct only if complexity grows
- Keep DTOs lightweight; include only fields needed by clients
- Use constructor-based initialization or builders for clarity

### Testing

- Test behavior, not implementation details
- Use descriptive test method names: `testCreateProductWithValidDataReturns201`
- Mock external dependencies (repositories); test in isolation
- Cover happy path, edge cases, and error scenarios

### Performance Considerations

- For MVP, premature optimization is the enemy; keep it simple
- Monitor actual performance; optimize only when bottleneck identified
- H2 indexes on foreign keys; add more if query patterns warrant

---

## Summary

The Warranty Tracker REST API design follows a **simple, layered architecture** aligned with Spring Boot best practices. The system uses **DTOs to decouple API contracts** from database entities, **calculates warranty status on-demand** for accuracy, and **centralizes validation and error handling** for consistency. The **H2 in-memory database** provides zero-configuration persistence suitable for MVP portfolio work. **Springdoc OpenAPI** automatically generates interactive API documentation. All components are **testable in isolation** using straightforward JUnit and Mockito unit tests for services and controllers.

This design prioritizes **clarity and simplicity** over complexity, avoiding unnecessary abstractions while maintaining professional structure. The architecture scales naturally as requirements grow—additional features (authentication, caching, advanced filtering) can be added without restructuring core layers.
