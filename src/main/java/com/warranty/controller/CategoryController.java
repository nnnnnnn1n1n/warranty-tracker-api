package com.warranty.controller;

import com.warranty.dto.response.CategoryResponse;
import com.warranty.service.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST Controller for Category endpoints.
 * Handles HTTP requests for retrieving categories and category details.
 */
@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    /**
     * Retrieve all categories.
     *
     * @return ResponseEntity with HTTP 200 status and a list of CategoryResponse objects
     */
    @GetMapping
    public ResponseEntity<List<CategoryResponse>> getAllCategories() {
        List<CategoryResponse> categories = categoryService.getAllCategories();
        return ResponseEntity.status(HttpStatus.OK).body(categories);
    }

    /**
     * Retrieve a specific category by its ID.
     *
     * @param id the category ID (path variable)
     * @return ResponseEntity with HTTP 200 status and the CategoryResponse object if found
     * @throws CategoryNotFoundException if the category with the given ID does not exist (handled by GlobalExceptionHandler)
     */
    @GetMapping("/{id}")
    public ResponseEntity<CategoryResponse> getCategoryById(@PathVariable Long id) {
        CategoryResponse category = categoryService.getCategoryById(id);
        return ResponseEntity.status(HttpStatus.OK).body(category);
    }
}
