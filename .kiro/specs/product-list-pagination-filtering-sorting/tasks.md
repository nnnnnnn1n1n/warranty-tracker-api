# Implementation Plan: Product List — Pagination, Filtering & Sorting

## Overview

This implementation plan delivers the GET /api/products endpoint with pagination, filtering by category and warranty status, and sorting by product attributes. All tasks follow approved requirements.md and design.md.

---

## Task Dependency Graph

```json
{
  "waves": [
    {
      "wave": 1,
      "tasks": [
        {
          "id": "1",
          "title": "Introduce Application Clock (Asia/Bangkok)",
          "dependencies": []
        },
        {
          "id": "2",
          "title": "Define OpenAPI Contract for GET /api/products",
          "dependencies": []
        }
      ]
    },
    {
      "wave": 2,
      "tasks": [
        {
          "id": "3",
          "title": "Implement Product List Endpoint",
          "dependencies": ["1"]
        }
      ]
    }
  ]
}
```

---

## Tasks

- [x] 1. Create config/ClockConfig.java with @Bean Clock for Asia/Bangkok timezone
- [x] 2. Update ProductService to inject Clock and add getCurrentDate() method
- [x] 3. Update ProductService.listProducts() to call getCurrentDate() and pass to repository
- [x] 4. Update ProductServiceTest to mock Clock for deterministic dates
- [x] 5. Verify existing tests still pass
- [x] 6. Create PaginationMetadata schema in OpenAPI
- [x] 7. Create ProductListResponse schema
- [x] 8. Create WarrantyStatus enum schema
- [x] 9. Update ProductResponse schema to include warrantyStatus field
- [x] 10. Define GET /api/products endpoint with query parameters in OpenAPI
- [x] 11. Document HTTP 400 validation errors with specific messages
- [x] 12. Document HTTP 200 response structure
- [x] 13. Document HTTP 200 with empty array for non-existent categoryId
- [x] 14. Add example requests and responses to OpenAPI
- [x] 15. Verify Swagger UI displays correctly
- [x] 16. Create dto/response/PaginationMetadata.java
- [x] 17. Create dto/response/ProductListResponse.java
- [x] 18. Create dto/SearchFilters.java
- [x] 19. Create dto/ProductSearchResult.java
- [x] 20. Add GET /api/products to ProductController
- [x] 21. Validate limit > 0 (default 20)
- [x] 22. Validate offset >= 0 (default 0)
- [x] 23. Validate status in {ACTIVE, EXPIRING_SOON, EXPIRED}
- [x] 24. Validate sort format and field whitelist
- [x] 25. Call ProductService.listProducts() and return HTTP 200
- [x] 26. Add listProducts() method to ProductService
- [x] 27. Call getCurrentDate() and create SearchFilters DTO
- [x] 28. Call productRepository.searchProducts()
- [x] 29. Convert ProductSearchResult to ProductListResponse
- [x] 30. Update convertToResponse() to use getCurrentDate()
- [x] 31. Create 
epository/ProductRepositoryCustom interface
- [x] 32. Create 
epository/ProductRepositoryCustomImpl
- [x] 33. Implement buildWhereClause() for category and status filters
- [x] 34. Implement buildStatusCondition() for warranty status logic
- [x] 35. Implement buildOrderByClause() with whitelist validation
- [x] 36. Implement searchProducts() with COUNT and data queries
- [x] 37. Set bind parameters (categoryId, limit, offset)
- [x] 38. Use DATE literals for date comparisons in WHERE clause
- [x] 39. Update ProductRepository to extend ProductRepositoryCustom
- [x] 40. Add db.warranty-end-date-expr property to application.properties
- [x] 41. Add db.warranty-end-date-expr property to application-postgresql.yml
- [x] 42. Create ProductControllerTest.java with unit tests for GET /api/products parameter validation
  - File: src/test/java/com/warranty/controller/ProductControllerTest.java
  - Use @ExtendWith(MockitoExtension.class) with MockMvc standaloneSetup (mirror CategoryControllerTest pattern)
  - Mock ProductService
  - Test cases for limit validation: limit=0 returns 400 with message "Limit must be greater than zero"; limit=-1 returns 400; limit=1 returns 200
  - Test cases for offset validation: offset=-1 returns 400 with message "Limit and offset must be non-negative integers"; offset=0 returns 200
  - Test cases for status validation: status=ACTIVE returns 200; status=EXPIRING_SOON returns 200; status=EXPIRED returns 200; status=active (lowercase) returns 400 with message "Status must be one of: ACTIVE, EXPIRING_SOON, EXPIRED"; status=INVALID returns 400; status omitted returns 200
  - Test cases for sort validation: valid sort fields (id,name,purchaseDate,warrantyMonths) return 200; invalid sort field returns 400; valid directions (asc,desc) return 200; missing comma separator returns 400
  - Default parameters (no query params) return 200
- [x] 43. Write unit tests for service with mocked repository
- [x] 44. Write integration tests for pagination
- [x] 45. Write integration tests for category filtering
- [x] 46. Write integration tests for warranty status filtering
- [x] 47. Write integration tests for combined filters
- [x] 48. Write integration tests for sorting by all fields
- [x] 49. Write integration tests for edge cases
- [ ] 50. Verify all existing tests pass

---

## Notes

Clock (Wave 1, Tasks 1-5) required first. OpenAPI (Wave 1, Tasks 6-15) can start in parallel. Implementation (Wave 2, Tasks 16-50) requires Clock completion.

