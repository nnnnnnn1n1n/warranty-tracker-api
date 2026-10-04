package com.warranty.dto.internal;

import com.warranty.entity.Product;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * Internal DTO for holding repository query results.
 * Returned by the custom repository searchProducts() method.
 * Contains the matching products and the total count of matching results (before pagination).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ProductSearchResult {
    /**
     * List of Product entities matching the search criteria (filtered and sorted, but before pagination)
     */
    private List<Product> products;

    /**
     * Total count of products matching all filter criteria (before pagination).
     * Used to determine pagination metadata like totalCount and hasMore.
     */
    private long totalCount;
}