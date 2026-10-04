package com.warranty.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Response DTO for pagination metadata.
 * Included in every paginated API response to provide pagination state information.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PaginationMetadata {
    /**
     * Total count of products matching all filter criteria (before pagination).
     * Used by clients to determine total pages and whether more results exist.
     */
    @JsonProperty("totalCount")
    private long totalCount;

    /**
     * Maximum number of products requested for this page.
     * May be less than totalCount; actual results may be fewer than limit.
     */
    @JsonProperty("limit")
    private int limit;

    /**
     * Offset value applied to this page (number of products skipped).
     * Reflects the offset parameter used in the request.
     */
    @JsonProperty("offset")
    private int offset;

    /**
     * Boolean flag indicating whether more products exist beyond this page.
     * Calculated as (offset + limit) < totalCount.
     * Clients use this to determine if pagination can continue.
     */
    @JsonProperty("hasMore")
    private boolean hasMore;
}