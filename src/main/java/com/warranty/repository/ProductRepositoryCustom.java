package com.warranty.repository;

import com.warranty.dto.internal.ProductSearchResult;
import com.warranty.dto.internal.SearchFilters;

/**
 * Custom Spring Data repository interface for advanced product search operations.
 * 
 * This interface defines a single method for searching products with filtering, sorting, and pagination.
 * It is implemented separately (ProductRepositoryCustomImpl) using native SQL to support:
 * - Database-agnostic warranty end date calculation (via injected expressions)
 * - Efficient warranty status filtering at the database level
 * - Correct pagination with accurate total count before LIMIT/OFFSET
 * 
 * This interface is mixed into ProductRepository through Spring's custom repository pattern.
 */
public interface ProductRepositoryCustom {

    /**
     * Search products with filtering, sorting, and pagination.
     * 
     * Executes a native SQL query that:
     * 1. Filters by category ID (if provided in filters)
     * 2. Filters by warranty status (ACTIVE, EXPIRING_SOON, EXPIRED) at the database level
     *    using the provided business date (today)
     * 3. Sorts by the specified field and direction
     * 4. Applies LIMIT/OFFSET for pagination
     * 
     * The returned ProductSearchResult contains the paginated list of products and the total count
     * of all products matching the filters (before pagination). This allows clients to determine
     * if additional pages exist and to understand the full scope of the filtered dataset.
     * 
     * @param filters SearchFilters containing:
     *                - categoryId: optional Long for filtering by category
     *                - status: optional String (ACTIVE, EXPIRING_SOON, EXPIRED) for warranty status filtering
     *                - today: LocalDate representing the business date for warranty calculations
     * @param sortField field name for sorting (validated whitelist: id, name, purchaseDate, 
     *                  warrantyEndDate, warrantyMonths, categoryId)
     * @param sortDirection sort direction (ASC or DESC)
     * @param limit number of products to return (page size)
     * @param offset starting index in the filtered result set (for pagination)
     * 
     * @return ProductSearchResult containing:
     *         - products: list of Product entities matching filters, sorted, and paginated
     *         - totalCount: total number of products matching all filters (before pagination)
     * 
     * @throws IllegalArgumentException if sortField is not in the whitelist
     * @throws IllegalArgumentException if sortDirection is not ASC or DESC
     */
    ProductSearchResult searchProducts(SearchFilters filters,
                                       String sortField,
                                       String sortDirection,
                                       int limit,
                                       int offset);
}
