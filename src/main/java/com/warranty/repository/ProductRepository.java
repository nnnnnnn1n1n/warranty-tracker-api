package com.warranty.repository;

import com.warranty.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

/**
 * Repository interface for Product entity.
 * Extends JpaRepository to provide CRUD operations and custom query methods.
 */
@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    /**
     * Find all products whose warranty expires on or before the specified end date.
     *
     * @param endDate the end date to filter by (products expiring on or before this date)
     * @return a list of products expiring within the specified timeframe
     */
    @Query("SELECT p FROM Product p WHERE p.purchaseDate IS NOT NULL AND p.warrantyMonths IS NOT NULL " +
           "AND DATE_ADD(p.purchaseDate, p.warrantyMonths, 'MONTH') <= :endDate")
    List<Product> findProductsExpiringWithin(@Param("endDate") LocalDate endDate);
}
