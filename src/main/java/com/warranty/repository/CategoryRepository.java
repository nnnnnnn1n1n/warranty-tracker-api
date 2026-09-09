package com.warranty.repository;

import com.warranty.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository interface for Category entity.
 * Extends JpaRepository to provide CRUD operations and custom query methods.
 */
@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {

    /**
     * Find a category by its name.
     *
     * @param name the name of the category
     * @return an Optional containing the category if found, or empty if not
     */
    Optional<Category> findByName(String name);
}
