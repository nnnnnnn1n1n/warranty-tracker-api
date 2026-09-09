package com.warranty.service;

import com.warranty.dto.response.CategoryResponse;
import com.warranty.entity.Category;
import com.warranty.exception.CategoryNotFoundException;
import com.warranty.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Service class for handling category-related business logic.
 * Manages CRUD operations and data transformation for categories.
 */
@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;

    /**
     * Retrieve all categories from the database.
     *
     * @return a list of CategoryResponse DTOs (empty list if no categories exist)
     */
    public List<CategoryResponse> getAllCategories() {
        return categoryRepository.findAll()
                .stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Retrieve a category by its ID.
     *
     * @param id the ID of the category to retrieve
     * @return a CategoryResponse DTO if found
     * @throws CategoryNotFoundException if the category with the given ID does not exist
     */
    public CategoryResponse getCategoryById(Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new CategoryNotFoundException(
                        "Category with ID " + id + " not found"
                ));
        return convertToResponse(category);
    }

    /**
     * Save sample categories to the database.
     * Creates five predefined categories: Kitchen Appliances, Bedroom, Beauty, Electronics, Home Appliances.
     */
    public void saveSampleCategories() {
        List<Category> categories = List.of(
                new Category("Kitchen Appliances"),
                new Category("Bedroom"),
                new Category("Beauty"),
                new Category("Electronics"),
                new Category("Home Appliances")
        );
        categoryRepository.saveAll(categories);
    }

    /**
     * Convert a Category entity to a CategoryResponse DTO.
     *
     * @param category the Category entity to convert
     * @return a CategoryResponse DTO
     */
    private CategoryResponse convertToResponse(Category category) {
        return new CategoryResponse(category.getId(), category.getName());
    }
}
