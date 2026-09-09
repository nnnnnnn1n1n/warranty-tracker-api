package com.warranty.repository;

import com.warranty.entity.Category;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for CategoryRepository.
 * Tests CRUD operations and custom query methods.
 */
@DataJpaTest
class CategoryRepositoryTest {

    @Autowired
    private CategoryRepository categoryRepository;

    @Test
    void testSaveCategory() {
        // Arrange
        Category category = new Category("Electronics");

        // Act
        Category savedCategory = categoryRepository.save(category);

        // Assert
        assertNotNull(savedCategory.getId());
        assertEquals("Electronics", savedCategory.getName());
    }

    @Test
    void testFindByIdWithValidId() {
        // Arrange
        Category category = new Category("Kitchen Appliances");
        Category savedCategory = categoryRepository.save(category);

        // Act
        Optional<Category> foundCategory = categoryRepository.findById(savedCategory.getId());

        // Assert
        assertTrue(foundCategory.isPresent());
        assertEquals(savedCategory.getId(), foundCategory.get().getId());
        assertEquals("Kitchen Appliances", foundCategory.get().getName());
    }

    @Test
    void testFindByIdWithInvalidId() {
        // Act
        Optional<Category> foundCategory = categoryRepository.findById(999L);

        // Assert
        assertTrue(foundCategory.isEmpty());
    }

    @Test
    void testFindByName() {
        // Arrange
        Category category = new Category("Beauty");
        categoryRepository.save(category);

        // Act
        Optional<Category> foundCategory = categoryRepository.findByName("Beauty");

        // Assert
        assertTrue(foundCategory.isPresent());
        assertEquals("Beauty", foundCategory.get().getName());
    }

    @Test
    void testFindByNameNotFound() {
        // Act
        Optional<Category> foundCategory = categoryRepository.findByName("NonExistent");

        // Assert
        assertTrue(foundCategory.isEmpty());
    }

    @Test
    void testFindAll() {
        // Arrange
        categoryRepository.save(new Category("Electronics"));
        categoryRepository.save(new Category("Bedroom"));

        // Act
        var allCategories = categoryRepository.findAll();

        // Assert
        assertTrue(allCategories.size() >= 2);
    }

    @Test
    void testDeleteCategory() {
        // Arrange
        Category category = new Category("Home Appliances");
        Category savedCategory = categoryRepository.save(category);
        Long categoryId = savedCategory.getId();

        // Act
        categoryRepository.deleteById(categoryId);

        // Assert
        Optional<Category> deletedCategory = categoryRepository.findById(categoryId);
        assertTrue(deletedCategory.isEmpty());
    }
}
