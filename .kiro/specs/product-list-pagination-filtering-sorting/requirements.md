# Requirements Document

## Introduction

### Product List — Pagination, Filtering & Sorting

This specification defines the requirements for enhancing the GET `/api/products` endpoint to support pagination, filtering by category and warranty status, and sorting by product attributes. These features enable users to efficiently navigate and discover products in the warranty tracker system without loading all products at once.

The solution maintains existing warranty status calculation logic (ACTIVE, EXPIRING_SOON, EXPIRED) and does not persist status to the database. All filtering, sorting, and pagination operations work with the current Product entity structure.

---

## Glossary

- **Pagination**: The process of dividing a large result set into smaller pages for retrieval, specified by page number/size or limit/offset parameters.
- **Limit**: The maximum number of products requested for a page (the actual page may contain fewer products).
- **Offset**: The number of products to skip before returning results (zero-indexed).
- **Filtering**: Restricting results based on specific field values or criteria.
- **Category_ID**: The unique identifier of a product's category, used for category filtering.
- **Warranty_Status**: The calculated status of a product's warranty: ACTIVE, EXPIRING_SOON, or EXPIRED.
- **Sorting**: Ordering results by one or more fields in ascending or descending direction.
- **Sort_Field**: The product attribute by which results are ordered (e.g., name, purchaseDate, warrantyEndDate).
- **Sort_Direction**: The order direction: ascending (asc) or descending (desc).
- **Default_Page_Size**: The standard number of products returned if the limit/pageSize parameter is not provided.
- **Default_Sort**: The standard sort field and direction applied if no sort parameter is provided.
- **Query_Parameter**: A name-value pair in the URL query string that specifies filtering, sorting, or pagination options.
- **Warranty_End_Date**: The calculated date when a product's warranty expires, computed as purchaseDate + warrantyMonths.

---

## Requirements

### Requirement 1: Pagination Support with Limit/Offset

**User Story:** As an API consumer, I want to retrieve products page-by-page using limit and offset parameters, so that I can fetch large product lists incrementally without overwhelming the client or server.

#### Acceptance Criteria

1. WHEN the GET `/api/products` endpoint is called WITH query parameters `limit` and `offset`, THE Product_List_API SHALL return a paginated response containing only the requested subset of products.
2. WHEN `limit` parameter is provided AND is a positive integer, THE Product_List_API SHALL request at most that many products.
3. WHEN `offset` parameter is provided AND is zero or a positive integer, THE Product_List_API SHALL skip that many products before returning results.
4. WHEN neither `limit` nor `offset` is provided, THE Product_List_API SHALL apply a default limit of 20 and default offset of 0.
5. WHEN `limit` is not provided BUT `offset` is provided, THE Product_List_API SHALL use the default limit of 20 with the provided offset.
6. WHEN `limit` or `offset` is provided AND is a negative integer, THE Product_List_API SHALL return a 400 Bad Request error with message "Limit and offset must be non-negative integers".
7. WHEN `limit` is provided AND is zero, THE Product_List_API SHALL return a 400 Bad Request error with message "Limit must be greater than zero".
8. WHEN the pagination parameters would result in an offset beyond all available products, THE Product_List_API SHALL return an empty list of products with appropriate pagination metadata.

### Requirement 2: Pagination Response Metadata

**User Story:** As an API consumer, I want each paginated response to include metadata about the pagination state, so that I can understand the current page, total items available, and construct navigation links.

#### Acceptance Criteria

1. THE Product_List_API SHALL include pagination metadata in every response containing the following fields:
   - `totalCount`: The total number of products matching the filter criteria (without pagination).
   - `limit`: The maximum number of products requested for this page (the actual page may contain fewer).
   - `offset`: The offset value used for this page (reflects the applied offset).
   - `hasMore`: A boolean flag indicating whether more products exist beyond the current page.
2. WHEN products are returned, THE `hasMore` field SHALL be true if `offset + limit < totalCount`, and false otherwise.
3. WHEN the result set is empty (no matching products), THE Product_List_API SHALL return `totalCount: 0`, `hasMore: false`, and an empty products array.

### Requirement 3: Category Filtering

**User Story:** As an API consumer, I want to filter products by category ID, so that I can view only products in a specific category.

#### Acceptance Criteria

1. WHEN the GET `/api/products` endpoint is called WITH query parameter `categoryId` set to a valid category ID, THE Product_List_API SHALL return only products belonging to that category.
2. WHEN `categoryId` is provided AND the category does not exist, THE Product_List_API SHALL return an empty products list with pagination metadata reflecting zero matches (HTTP 200).
3. WHEN `categoryId` is provided AND is not a valid integer, THE Product_List_API SHALL return a 400 Bad Request error with message "Category ID must be a valid integer".
4. WHEN `categoryId` is not provided, THE Product_List_API SHALL return products from all categories.
5. WHEN `categoryId` is provided AND matches a category with no products, THE Product_List_API SHALL return an empty products list with pagination metadata reflecting zero matches.
6. WHEN multiple products belong to the requested category, THE Product_List_API SHALL return all matching products (subject to pagination limits).

### Requirement 4: Warranty Status Filtering

**User Story:** As an API consumer, I want to filter products by warranty status (ACTIVE, EXPIRING_SOON, EXPIRED), so that I can quickly locate products based on their warranty state.

#### Acceptance Criteria

1. WHEN the GET `/api/products` endpoint is called WITH query parameter `status` set to one of (ACTIVE, EXPIRING_SOON, EXPIRED), THE Product_List_API SHALL return only products with the matching warranty status.
2. WHEN `status` is provided AND is one of the valid values (ACTIVE, EXPIRING_SOON, EXPIRED), THE Product_List_API SHALL calculate warranty status for each product using the current date and return matching products.
3. WHEN `status` is provided AND is not one of the valid values, THE Product_List_API SHALL return a 400 Bad Request error with message "Status must be one of: ACTIVE, EXPIRING_SOON, EXPIRED".
4. WHEN `status` is provided AND is provided in mixed case (e.g., Active, active), THE Product_List_API SHALL treat it as invalid and return a 400 Bad Request error.
5. WHEN `status` is not provided, THE Product_List_API SHALL return products with any warranty status (no status filtering applied).
6. WHEN `categoryId` and `status` filters are both provided, THE Product_List_API SHALL apply both filters (AND logic) and return only products matching both the category and status.

### Requirement 5: Sorting

**User Story:** As an API consumer, I want to sort products by various fields and directions, so that I can view results in a meaningful order.

#### Acceptance Criteria

1. WHEN the GET `/api/products` endpoint is called WITH query parameter `sort` in the format `field,direction` (e.g., `name,asc` or `warrantyEndDate,desc`), THE Product_List_API SHALL sort results by the specified field in the specified direction.
2. THE Product_List_API SHALL support sorting by the following fields:
   - `id`: Sort by product ID.
   - `name`: Sort by product name (alphabetically).
   - `purchaseDate`: Sort by purchase date.
   - `warrantyEndDate`: Sort by calculated warranty end date.
   - `warrantyMonths`: Sort by warranty duration.
   - `categoryId`: Sort by category ID.
3. WHEN sort direction is specified, THE Product_List_API SHALL support both `asc` (ascending) and `desc` (descending) directions.
4. WHEN `sort` parameter is provided WITH an invalid field name, THE Product_List_API SHALL return a 400 Bad Request error with message "Invalid sort field: {field}. Supported fields are: id, name, purchaseDate, warrantyEndDate, warrantyMonths, categoryId".
5. WHEN `sort` parameter is provided WITH an invalid direction (not asc or desc), THE Product_List_API SHALL return a 400 Bad Request error with message "Sort direction must be either 'asc' or 'desc'".
6. WHEN `sort` parameter is provided in incorrect format (e.g., missing direction or extra commas), THE Product_List_API SHALL return a 400 Bad Request error with message "Sort parameter must be in format 'field,direction' (e.g., 'name,asc')".
7. WHEN `sort` is not provided, THE Product_List_API SHALL apply a default sort of `id,asc`.

### Requirement 6: Parameter Validation and Error Handling

**User Story:** As an API consumer, I want clear error messages for invalid query parameters, so that I can correct my requests quickly.

#### Acceptance Criteria

1. WHEN any query parameter value cannot be parsed or is invalid, THE Product_List_API SHALL return a 400 Bad Request HTTP status code with a `message` field containing a specific description of the validation failure.
2. WHEN `offset` is provided without `limit`, THE Product_List_API SHALL apply the default limit of 20 and proceed normally (not an error).
3. WHEN unrecognized query parameters are provided (e.g., `unknownParam=value`), THE Product_List_API SHALL ignore them and process recognized parameters normally.

### Requirement 7: Filter and Sort Interaction

**User Story:** As an API consumer, I want to combine filters and sorting flexibly, so that I can refine results and view them in a meaningful order.

#### Acceptance Criteria

1. WHEN `categoryId` filter is combined WITH `sort` parameter, THE Product_List_API SHALL return products filtered by category and sorted by the specified field and direction.
2. WHEN `status` filter is combined WITH `sort` parameter, THE Product_List_API SHALL return products filtered by warranty status and sorted by the specified field and direction.
3. WHEN both `categoryId` AND `status` filters are combined, THE Product_List_API SHALL return only products matching both filters (AND logic).
4. WHEN filters and sorting are applied, THE pagination SHALL be applied to the combined filtered and sorted result set.
5. WHEN multiple filters result in zero matching products, THE Product_List_API SHALL return an empty list with pagination metadata showing `totalCount: 0` and `hasMore: false`.

### Requirement 8: Warranty Status Calculation During Filtering

**User Story:** As an API consumer, I want status filtering to use the current date for warranty calculations, so that the warranty status is always accurate and reflects today's state.

#### Acceptance Criteria

1. WHEN filtering by warranty `status`, THE Product_List_API SHALL calculate warrantyEndDate for each product as `purchaseDate + warrantyMonths`.
2. WHEN filtering by warranty `status`, THE Product_List_API SHALL determine warranty status using the following rules:
   - EXPIRED: today > warrantyEndDate
   - EXPIRING_SOON: today <= warrantyEndDate AND warrantyEndDate <= today + 30 days
   - ACTIVE: warrantyEndDate > today + 30 days
3. WHEN status filtering is performed, THE calculation SHALL use the current date at request time (not cached or pre-calculated).
4. WARRANTY_STATUS SHALL NOT be stored in the database; it is always calculated at request time.

### Requirement 9: Response Structure

**User Story:** As an API consumer, I want a consistent response structure for paginated, filtered, and sorted results, so that I can reliably parse and handle API responses.

#### Acceptance Criteria

1. THE Product_List_API SHALL return responses in a JSON object containing:
   - `data`: An array of ProductResponse objects (may be empty).
   - `pagination`: An object containing pagination metadata (totalCount, limit, offset, hasMore).
2. EACH ProductResponse object SHALL contain the following fields:
   - `id`: The product ID.
   - `name`: The product name.
   - `categoryId`: The category ID.
   - `purchaseDate`: The purchase date.
   - `warrantyMonths`: The warranty duration in months.
   - `warrantyEndDate`: The calculated warranty end date.
   - `warrantyStatus`: The calculated warranty status (ACTIVE, EXPIRING_SOON, or EXPIRED).
   - `category`: An object containing the category `id` and `name`.
3. WHEN the response contains no products, THE `data` array SHALL be empty, but the `pagination` object SHALL still be present with accurate metadata.

---

## Notes

- The Product_List_API maintains the existing Product entity schema and does not introduce new columns or tables.
- Warranty status filtering introduces runtime calculation overhead; no caching strategy is mandated by these requirements.
- The API response may be extended in the future with additional metadata fields without breaking existing consumers (additive backward compatibility).
- Query parameters follow Spring Web conventions for naming and parsing (camelCase for parameter names).
