package com.warranty.repository;

import com.warranty.dto.internal.ProductSearchResult;
import com.warranty.dto.internal.SearchFilters;

/**
 * Custom repository interface for advanced Product queries.
 * Provides methods for complex filtering, sorting, and pagination operations
 * that cannot be easily expressed using Spring Data derived queries.
 */
public interface ProductRepositoryCustom {

    /**
     * Search products with filtering, sorting, and pagination.
     *
     * Filtering:
     * - By category ID (if provided in filters.categoryId)
     * - By warranty status (if provided in filters.status)
     *
     * Sorting:
     * - By specified field and direction
     *
     * Pagination:
     * - Returns a page based on limit and offset parameters
     *
     * @param filters SearchFilters containing categoryId, status, and business date (today)
     * @param sortField the field to sort by (e.g., "id", "name", "purchaseDate", "warrantyEndDate", "warrantyMonths", "categoryId")
     * @param sortDirection the sort direction: "asc" or "desc"
     * @param limit the maximum number of products per page
     * @param offset the number of products to skip
     * @return ProductSearchResult containing the matching products and total count
     */
    ProductSearchResult searchProducts(
            SearchFilters filters,
            String sortField,
            String sortDirection,
            int limit,
            int offset);
}