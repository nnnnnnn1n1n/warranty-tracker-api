package com.warranty.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * Response DTO for products.
 * Contains product information including calculated fields (warrantyEndDate, warrantyStatus)
 * and nested category information.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductResponse {

    private Long id;
    private String name;
    private Long categoryId;
    private LocalDate purchaseDate;
    private Integer warrantyMonths;
    
    // Calculated fields
    private LocalDate warrantyEndDate;
    private String warrantyStatus;
    
    // Nested category information
    private CategoryResponse category;
}
