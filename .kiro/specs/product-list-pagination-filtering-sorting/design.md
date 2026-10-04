# Design Document: Product List — Pagination, Filtering & Sorting

## Overview

The GET /api/products endpoint will support limit/offset pagination, filtering by category and warranty status, and sorting by product attributes. The design leverages a **custom Spring Data repository** with native SQL to execute warranty status filtering at the database level while maintaining pagination correctness.

All warranty status values (ACTIVE, EXPIRING_SOON, EXPIRED) remain runtime-calculated and are not persisted. Filtering and counting occur in the database before pagination is applied. The response wraps the product list and pagination metadata in a consistent structure.

---

## Architecture

### Request Processing Flow

1. **Controller** receives query parameters (limit, offset, categoryId, status, sort)
2. **Validation Layer** parses and validates parameters; throws ValidationException for invalid inputs
3. **Service Layer** calls the custom repository with business date (from Clock), filters, and pagination details
4. **Custom Repository** builds and executes dynamic native SQL queries for data and count
5. **Response Assembly** converts Product entities to ProductResponse with calculated warranty status

### Key Design Decision: Custom Repository with Native SQL

We implement a **custom Spring Data repository** (ProductRepositoryCustom + ProductRepositoryCustomImpl) rather than JPA Criteria API or method explosion because:

- **Portable**: Derived field filtering (warrantyEndDate computed from purchaseDate + warrantyMonths) requires database-specific SQL expressions (PostgreSQL make_interval vs H2 DATEADD). Hibernate Criteria API cannot portably express this across dialects.
- **Single method**: One searchProducts() method handles all filter combinations (category, status, sort), avoiding method explosion.
- **Correct pagination**: Filtering occurs at the database level before LIMIT/OFFSET is applied, ensuring accurate totalCount and hasMore.
- **Lean**: Minimal abstraction; direct use of EntityManager and native SQL with bind parameters.

---

## Components and Interfaces

### DTOs

**PaginationMetadata**
- 	otalCount (long): Total products matching all filters (before pagination)
- limit (int): Page size requested
- offset (int): Offset applied
- hasMore (boolean): true if (offset + limit) < totalCount

**ProductListResponse**
- data (List<ProductResponse>): Product array with calculated warranty fields
- pagination (PaginationMetadata): Pagination metadata

**SearchFilters** (internal, passed to repository)
- categoryId (Long, nullable): Filter by category
- status (String, nullable): Filter by warranty status (ACTIVE, EXPIRING_SOON, EXPIRED)
- 	oday (LocalDate): Business date for warranty calculations (provided by service from Clock)

ProductResponse and CategoryResponse are reused from existing codebase.

### OpenAPI Schemas

Update OpenApiConfig to define:
- PaginationMetadata: Pagination object structure
- ProductListResponse: Wrapper containing data and pagination
- WarrantyStatus: Enum (ACTIVE, EXPIRING_SOON, EXPIRED)
- Update ProductResponse schema to include warrantyStatus

---

## Data Models

### Warranty End Date Calculation

Warranty end date is a **derived field**, calculated from:
`
warrantyEndDate = purchaseDate + (warrantyMonths months)
`

**Storage**: Not persisted. Calculated at response time by the service layer.

**Database Calculation** (for filtering/sorting): Uses database-specific expressions:
- **PostgreSQL**: purchase_date + make_interval(months => warranty_months)
- **H2**: DATEADD('MONTH', warranty_months, purchase_date)

### Warranty Status Calculation

Status is computed using three conditions, evaluated at the database query level:
- **EXPIRED**: today > warrantyEndDate
- **EXPIRING_SOON**: today ≤ warrantyEndDate AND warrantyEndDate ≤ today + 30 days
- **ACTIVE**: warrantyEndDate > today + 30 days

where 	oday is a business date passed from the application (via Clock, timezone-aware).

**Implementation**: Status filter is applied in the WHERE clause of the native SQL query. All returned products already satisfy the requested status, so status recalculation in response assembly will confirm the same status.

**Storage**: Warranty status is not persisted. It is derived at response time using WarrantyCalculator.

### Product & Category Entities

Product and Category table schemas remain unchanged. No new columns are added.

---

## Correctness Properties

Formal property-based testing is **not applicable** to this feature. Infrastructure concerns (pagination, filtering, sorting) are validated through example-based and integration tests.

### Property 1: Pagination Slice Correctness

For any valid pagination parameters (limit, offset) and filter/sort combination, the returned product list must be the correct slice of the filtered and sorted result set. The slice boundaries (determined by offset and limit) must be accurate, and totalCount must reflect all products matching filters before pagination.

**Validates: Requirements 1.1, 1.2, 1.3, 2.1, 2.2**

**Testable behaviors**:
- Parameter validation (invalid limit, offset, sort, status values)
- Warranty status filter correctness (ACTIVE, EXPIRING_SOON, EXPIRED conditions)
- Category filtering with existing and non-existing categories
- Combined filter scenarios (category + status)
- Sort by warrantyEndDate and other fields
- Pagination metadata calculation (totalCount, hasMore)
- Default values and edge cases (empty results, single page, offset beyond results)

---

## Error Handling

**Validation Errors**:
- Invalid limit (≤ 0), offset (< 0) → ValidationException → HTTP 400
- Invalid status enum value → ValidationException → HTTP 400
- Invalid sort field or direction → ValidationException → HTTP 400
- Invalid categoryId format → ValidationException → HTTP 400

**Non-validation Errors**:
- Non-existent categoryId → HTTP 200 with empty data array (repository query returns no results)

**Exception Handling**: Global exception handler in GlobalExceptionHandler converts ValidationException to HTTP 400 responses with ErrorResponse.

---

## API Contract & Response Structure

### Query Parameters

| Parameter | Type | Required | Default | Validation |
|-----------|------|----------|---------|-----------|
| limit | integer | No | 20 | > 0; non-positive → 400 |
| offset | integer | No | 0 | ≥ 0; negative → 400 |
| categoryId | integer | No | — | Valid integer; non-existent → 200 with empty results |
| status | enum | No | — | One of ACTIVE, EXPIRING_SOON, EXPIRED; case-sensitive; invalid → 400 |
| sort | string | No | id,asc | Format: "field,direction"; field must be whitelisted; invalid → 400 |

**Supported sort fields**: id, 
ame, purchaseDate, warrantyEndDate, warrantyMonths, categoryId

### Response Structure

```json
{
  "data": [
    {
      "id": 1,
      "name": "Laptop",
      "categoryId": 1,
      "purchaseDate": "2023-01-15",
      "warrantyMonths": 24,
      "warrantyEndDate": "2025-01-15",
      "warrantyStatus": "ACTIVE",
      "category": {
        "id": 1,
        "name": "Electronics"
      }
    }
  ],
  "pagination": {
    "totalCount": 150,
    "limit": 20,
    "offset": 0,
    "hasMore": true
  }
}
```

### Pagination Semantics

The API preserves **arbitrary limit/offset semantics**, not page-based pagination:
- limit: Number of products per response (default 20)
- offset: Starting index in the result set (default 0)
- hasMore: Calculated as (offset + limit) < totalCount

**Example**: A client can request offset=5, limit=10 to get products 5-14, then offset=15, limit=10 for products 15-24. This is **not** equivalent to page-based PageRequest.of(pageNumber, pageSize).

### Sort Parameter Parsing

Parse sort parameter (format: "field,direction"):
- Extract field name and direction (case-insensitive direction, case-sensitive field per API contract)
- Validate field is in whitelist: {id, name, purchaseDate, warrantyEndDate, warrantyMonths, categoryId}
- Validate direction is "asc" or "desc"
- Throw ValidationException for invalid format or values
- Default sort if not provided: "id,asc"

---

## Implementation Layers

### Controller

```java
@GetMapping
public ResponseEntity<ProductListResponse> listProducts(
    @RequestParam(defaultValue = "20") Integer limit,
    @RequestParam(defaultValue = "0") Integer offset,
    @RequestParam(required = false) Long categoryId,
    @RequestParam(required = false) String status,
    @RequestParam(defaultValue = "id,asc") String sort) {
  
  // Validate parameters
  // Parse sort into field + direction
  // Call service with parsed parameters
  // Return ProductListResponse
}
```

**Validation logic**:
- Validate limit > 0; throw ValidationException if not
- Validate offset ≥ 0; throw ValidationException if not
- Validate status is null or one of {ACTIVE, EXPIRING_SOON, EXPIRED}; throw ValidationException if not
- Validate sort format and field/direction; throw ValidationException if not

### Service (ProductService)

**New method**: listProducts()
```java
public ProductListResponse listProducts(Long categoryId, String status, 
                                        String sortField, String sortDirection, 
                                        int limit, int offset) {
  // Get business date from Clock (Asia/Bangkok)
  LocalDate today = getCurrentDate();
  
  // Create SearchFilters DTO
  SearchFilters filters = new SearchFilters(categoryId, status, today);
  
  // Call custom repository
  ProductSearchResult result = productRepository.searchProducts(
      filters, 
      sortField, 
      sortDirection, 
      limit, 
      offset
  );
  
  // Convert products to responses with warranty status
  List<ProductResponse> responses = result.getProducts().stream()
      .map(this::convertToResponse)
      .collect(Collectors.toList());
  
  // Assemble and return response
  PaginationMetadata pagination = new PaginationMetadata(
      result.getTotalCount(),
      limit,
      offset,
      (offset + limit) < result.getTotalCount()
  );
  
  return new ProductListResponse(responses, pagination);
}

private LocalDate getCurrentDate() {
  // Injected Clock bean configured for Asia/Bangkok
  // Returns LocalDate
}
```

**Warranty status calculation** (in response assembly):
```java
private ProductResponse convertToResponse(Product product) {
  LocalDate warrantyEndDate = product.getPurchaseDate()
      .plusMonths(product.getWarrantyMonths());
  
  String warrantyStatus = WarrantyCalculator.calculateStatus(
      warrantyEndDate, 
      getCurrentDate() // Same date as filter
  ).toString();
  
  // Build and return ProductResponse with category
  return new ProductResponse(..., warrantyStatus, ...);
}
```

### Repository: Custom Implementation

The repository extends both JpaRepository and a custom interface:

```java
public interface ProductRepository 
    extends JpaRepository<Product, Long>, 
            ProductRepositoryCustom {
}

public interface ProductRepositoryCustom {
  ProductSearchResult searchProducts(SearchFilters filters, 
                                     String sortField, 
                                     String sortDirection,
                                     int limit, 
                                     int offset);
}

@Repository
public class ProductRepositoryCustomImpl implements ProductRepositoryCustom {
  
  @Autowired
  private EntityManager entityManager;
  
  @Value("\")
  private String warrantyEndDateExpr; // Resolved from active profile config (application.properties or application-postgresql.yml)
  
  @Override
  public ProductSearchResult searchProducts(SearchFilters filters, 
                                            String sortField, 
                                            String sortDirection,
                                            int limit, 
                                            int offset) {
    // Build shared WHERE clause from filters
    String whereClause = buildWhereClause(filters);
    
    // Build ORDER BY clause with whitelist validation
    String orderByClause = buildOrderByClause(sortField, sortDirection);
    
    // Execute COUNT query
    String countQuery = "SELECT COUNT(p.id) FROM product p " +
                        "JOIN category c ON p.category_id = c.id " +
                        whereClause;
    long totalCount = executeCountQuery(countQuery, filters);
    
    // Execute data query
    String dataQuery = "SELECT p.id, p.name, p.purchase_date, p.warranty_months, p.category_id, " +
                       "       c.id AS cat_id, c.name AS cat_name " +
                       "FROM product p " +
                       "JOIN category c ON p.category_id = c.id " +
                       whereClause +
                       orderByClause +
                       " LIMIT :limit OFFSET :offset";
    
    List<Product> products = executeDataQuery(dataQuery, filters, limit, offset);
    
    return new ProductSearchResult(products, totalCount);
  }
  
  private String buildWhereClause(SearchFilters filters) {
    List<String> conditions = new ArrayList<>();
    
    // Category filter
    if (filters.getCategoryId() != null) {
      conditions.add("p.category_id = :categoryId");
    }
    
    // Warranty status filter
    if (filters.getStatus() != null) {
      String statusCondition = buildStatusCondition(filters.getStatus(), filters.getToday());
      conditions.add("(" + statusCondition + ")");
    }
    
    return conditions.isEmpty() ? 
        "WHERE 1=1" : 
        "WHERE " + String.join(" AND ", conditions);
  }
  
  private String buildStatusCondition(String status, LocalDate today) {
    String expr = warrantyEndDateExpr;
    String todayPlus30 = today.plusDays(30).toString();
    String todayStr = today.toString();
    
    return switch(status) {
      case "ACTIVE" -> 
        expr + " > DATE '" + todayPlus30 + "'";
      case "EXPIRING_SOON" -> 
        expr + " >= DATE '" + todayStr + "' AND " +
        expr + " <= DATE '" + todayPlus30 + "'";
      case "EXPIRED" -> 
        expr + " < DATE '" + todayStr + "'";
      default -> 
        throw new ValidationException("Invalid warranty status: " + status);
    };
  }
  
  private String buildOrderByClause(String sortField, String sortDirection) {
    // Whitelist of allowed sort fields
    Map<String, String> fieldMapping = Map.ofEntries(
      Map.entry("id", "p.id"),
      Map.entry("name", "p.name"),
      Map.entry("purchaseDate", "p.purchase_date"),
      Map.entry("warrantyEndDate", warrantyEndDateExpr),
      Map.entry("warrantyMonths", "p.warranty_months"),
      Map.entry("categoryId", "p.category_id")
    );
    
    if (!fieldMapping.containsKey(sortField)) {
      throw new ValidationException("Invalid sort field: " + sortField);
    }
    
    if (!sortDirection.equalsIgnoreCase("ASC") && !sortDirection.equalsIgnoreCase("DESC")) {
      throw new ValidationException("Invalid sort direction: " + sortDirection);
    }
    
    String columnExpr = fieldMapping.get(sortField);
    return " ORDER BY " + columnExpr + " " + sortDirection.toUpperCase();
  }
  
  private long executeCountQuery(String query, SearchFilters filters) {
    Query q = entityManager.createNativeQuery(query);
    if (filters.getCategoryId() != null) {
      q.setParameter("categoryId", filters.getCategoryId());
    }
    return ((Number) q.getSingleResult()).longValue();
  }
  
  private List<Product> executeDataQuery(String query, SearchFilters filters, 
                                         int limit, int offset) {
    Query q = entityManager.createNativeQuery(query, Product.class);
    if (filters.getCategoryId() != null) {
      q.setParameter("categoryId", filters.getCategoryId());
    }
    q.setParameter("limit", limit);
    q.setParameter("offset", offset);
    return q.getResultList();
  }
}
```

**Key design decisions**:

- **Database-agnostic repository**: The repository contains no database detection or branching logic. It consumes the warrantyEndDateExpr value directly as a configuration property resolved by Spring at startup. The @Value annotation injects the correct expression based on the active profile (determined by Spring Boot's profile-based configuration resolution).
- **Profile-resolved configuration**: When the application starts, Spring loads either pplication.properties (default) or pplication-postgresql.yml (if -Dspring.profiles.active=postgresql is set). The db.warranty-end-date-expr property is resolved from the active configuration and injected into the repository. No repository code checks which database is active.
- **Shared WHERE clause**: Both count and data queries use uildWhereClause(), ensuring totalCount matches filtered data
- **Bind parameters**: All filter values (categoryId, limit, offset, today dates) use :parameterName; no string concatenation of user input
- **Warranty expression injected**: Database-specific warrantyEndDateExpr comes from @Value (profile-specific config)
- **Whitelist mapping**: uildOrderByClause() uses a Map to validate sort fields and translate names, preventing SQL injection
- **EntityManager native queries**: Returns managed Product entities; categories loaded via JOIN (no N+1)
- **Arbitrary offset/limit**: Direct LIMIT/OFFSET in SQL preserves client semantics without page conversion

### Database Configuration

The application uses Spring profiles to manage database-specific configuration. The `db.warranty-end-date-expr` property is resolved from the active profile's configuration file.

**Default configuration** (`application.properties` when no profile is explicitly active or when using the default profile):
```properties
db.warranty-end-date-expr=DATEADD('MONTH', warranty_months, purchase_date)
```

**PostgreSQL configuration** (`application-postgresql.yml` when `-Dspring.profiles.active=postgresql` is set):
```yaml
db:
  warranty-end-date-expr: "purchase_date + make_interval(months => warranty_months)"
```

**Profile activation:**
- Default (H2): `mvn spring-boot:run` or `java -jar application.jar`
- PostgreSQL: `mvn spring-boot:run -Dspring-boot.run.arguments=--spring.profiles.active=postgresql` or `java -jar application.jar --spring.profiles.active=postgresql`
---

## Category Loading Strategy

Categories are loaded using SQL JOIN in the native query. The query result includes both product and category columns, which EntityManager maps to a Product entity with the category relationship populated. No additional queries are required.

---

## Clock Configuration

A Spring Clock bean is configured for the Asia/Bangkok timezone. The service injects this Clock and calls LocalDate.now(clock) to get the business date. This date is passed to:
1. The repository (for warranty status filtering in the WHERE clause)
2. WarrantyCalculator (for warranty status calculation in response assembly)

This ensures all warranty calculations use a consistent date.

---

## Testing Strategy

**Unit testing** (example-based, with mocked EntityManager):
- Test parameter validation (invalid limit, offset, sort, status)
- Test WHERE clause building with various filter combinations
- Test ORDER BY clause whitelist validation
- Test warranty status conditions (ACTIVE, EXPIRING_SOON, EXPIRED)
- Test response assembly with calculated warranty status

**Integration testing**:
- Test full endpoint with real database (H2, PostgreSQL)
- Verify pagination correctness across different offsets/limits
- Verify sort order accuracy for all fields
- Verify filter combinations with real Product/Category data
- Verify totalCount and hasMore calculation

---

## Summary of Changes

**New DTOs**: 
- PaginationMetadata
- ProductListResponse
- SearchFilters (internal)
- ProductSearchResult (internal)

**Modified Controller**: 
- Add /api/products GET endpoint with limit, offset, categoryId, status, sort parameters

**Modified Service**: 
- Add listProducts() method
- Add getCurrentDate() method (using injected Clock)

**New Repository**:
- ProductRepositoryCustom interface
- ProductRepositoryCustomImpl implementation

**Updated ProductRepository**: 
- Extend ProductRepositoryCustom

**Updated OpenAPI Config**: 
- Define schemas for PaginationMetadata, ProductListResponse, WarrantyStatus

**Database Configuration**: 
- Add db.warranty-end-date-expr property to application.properties and profile-specific configs

**No schema changes**: Product and Category tables remain unchanged.
