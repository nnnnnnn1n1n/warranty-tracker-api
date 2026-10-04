package com.warranty.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * Response DTO for paginated product list endpoint.
 * Wraps the product data array and pagination metadata in a consistent response structure.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ProductListResponse {
    /**
     * Array of ProductResponse objects matching the search and filter criteria.
     * May be empty if no products match the filters or if offset is beyond all results.
     */
    @JsonProperty("data")
    private List<ProductResponse> data;

    /**
     * Pagination metadata object containing totalCount, limit, offset, and hasMore.
     * Present in every response, even if the data array is empty.
     */
    @JsonProperty("pagination")
    private PaginationMetadata pagination;
}