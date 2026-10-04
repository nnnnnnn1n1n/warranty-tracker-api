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
- [ ] 3. Update ProductService.listProducts() to call getCurrentDate() and pass to repository
- [x] 4. Update ProductServiceTest to mock Clock for deterministic dates
- [ ] 5. Verify existing tests still pass
- [ ] 6. Create PaginationMetadata schema in OpenAPI
- [ ] 7. Create ProductListResponse schema
- [ ] 8. Create WarrantyStatus enum schema
- [ ] 9. Update ProductResponse schema to include warrantyStatus field
- [ ] 10. Define GET /api/products endpoint with query parameters in OpenAPI
- [ ] 11. Document HTTP 400 validation errors with specific messages
- [ ] 12. Document HTTP 200 response structure
- [ ] 13. Document HTTP 200 with empty array for non-existent categoryId
- [ ] 14. Add example requests and responses to OpenAPI
- [ ] 15. Verify Swagger UI displays correctly
- [ ] 16. Create dto/response/PaginationMetadata.java
- [ ] 17. Create dto/response/ProductListResponse.java
- [ ] 18. Create dto/SearchFilters.java
- [ ] 19. Create dto/ProductSearchResult.java
- [ ] 20. Add GET /api/products to ProductController
- [ ] 21. Validate limit > 0 (default 20)
- [ ] 22. Validate offset >= 0 (default 0)
- [ ] 23. Validate status in {ACTIVE, EXPIRING_SOON, EXPIRED}
- [ ] 24. Validate sort format and field whitelist
- [ ] 25. Call ProductService.listProducts() and return HTTP 200
- [ ] 26. Add listProducts() method to ProductService
- [ ] 27. Call getCurrentDate() and create SearchFilters DTO
- [ ] 28. Call productRepository.searchProducts()
- [ ] 29. Convert ProductSearchResult to ProductListResponse
- [ ] 30. Update convertToResponse() to use getCurrentDate()
- [ ] 31. Create 
epository/ProductRepositoryCustom interface
- [ ] 32. Create 
epository/ProductRepositoryCustomImpl
- [ ] 33. Implement uildWhereClause() for category and status filters
- [ ] 34. Implement uildStatusCondition() for warranty status logic
- [ ] 35. Implement uildOrderByClause() with whitelist validation
- [ ] 36. Implement searchProducts() with COUNT and data queries
- [ ] 37. Set bind parameters (categoryId, limit, offset)
- [ ] 38. Use DATE literals for date comparisons in WHERE clause
- [ ] 39. Update ProductRepository to extend ProductRepositoryCustom
- [ ] 40. Add db.warranty-end-date-expr property to pplication.properties
- [ ] 41. Add db.warranty-end-date-expr property to pplication-postgresql.yml
- [ ] 42. Write unit tests for controller parameter validation
- [ ] 43. Write unit tests for service with mocked repository
- [ ] 44. Write integration tests for pagination
- [ ] 45. Write integration tests for category filtering
- [ ] 46. Write integration tests for warranty status filtering
- [ ] 47. Write integration tests for combined filters
- [ ] 48. Write integration tests for sorting by all fields
- [ ] 49. Write integration tests for edge cases
- [ ] 50. Verify all existing tests pass

---

## Notes

Clock (Wave 1, Tasks 1-5) required first. OpenAPI (Wave 1, Tasks 6-15) can start in parallel. Implementation (Wave 2, Tasks 16-50) requires Clock completion.
