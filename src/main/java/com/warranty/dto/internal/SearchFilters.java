package com.warranty.dto.internal;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

/**
 * Internal DTO for passing search filter parameters to the repository.
 * Contains category ID, warranty status, and the business date for consistent
 * warranty calculations across filtering, sorting, and response assembly.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SearchFilters {
    /**
     * Category ID filter (nullable; if null, no category filtering applied)
     */
    private Long categoryId;

    /**
     * Warranty status filter: one of ACTIVE, EXPIRING_SOON, EXPIRED (nullable; if null, no status filtering applied)
     */
    private String status;

    /**
     * Business date for warranty calculations and filtering.
     * Passed from the application (via Clock) to ensure consistent date usage.
     */
    private LocalDate today;
}