package com.warranty.repository;

import com.warranty.dto.internal.ProductSearchResult;
import com.warranty.dto.internal.SearchFilters;
import com.warranty.entity.Product;
import com.warranty.exception.ValidationException;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Custom implementation of ProductRepositoryCustom.
 * Provides complex Product queries using native SQL for advanced filtering and pagination.
 *
 * This implementation uses native queries to handle:
 * - Database-specific warranty end date calculations (e.g., PostgreSQL make_interval vs H2 DATEADD)
 * - Warranty status filtering at the database level
 * - Complex sorting with field validation
 * - Pagination with accurate totalCount calculation
 */
@Repository
@RequiredArgsConstructor
public class ProductRepositoryCustomImpl implements ProductRepositoryCustom {

    private final EntityManager entityManager;

    @Value("${db.warranty-end-date-expr}")
    private String warrantyEndDateExpr;

    /**
     * Search products with filtering, sorting, and pagination.
     *
     * @param filters SearchFilters containing categoryId, status, and business date
     * @param sortField the field to sort by
     * @param sortDirection the sort direction: "asc" or "desc"
     * @param limit the maximum number of products per page
     * @param offset the number of products to skip
     * @return ProductSearchResult containing products and totalCount
     */
    @Override
    public ProductSearchResult searchProducts(
            SearchFilters filters,
            String sortField,
            String sortDirection,
            int limit,
            int offset) {

        // Build WHERE clause from filters
        String whereClause = buildWhereClause(filters);

        // Build ORDER BY clause with validation
        String orderByClause = buildOrderByClause(sortField, sortDirection);

        // Execute COUNT query to get totalCount
        String countQuery = "SELECT COUNT(p.id) FROM product p " +
                           "JOIN category c ON p.category_id = c.id " +
                           whereClause;
        long totalCount = executeCountQuery(countQuery, filters);

        // Execute data query with pagination
        String dataQuery = "SELECT p.id, p.name, p.purchase_date, p.warranty_months, p.category_id, " +
                          "       c.id AS cat_id, c.name AS cat_name " +
                          "FROM product p " +
                          "JOIN category c ON p.category_id = c.id " +
                          whereClause +
                          orderByClause +
                          " LIMIT :limit OFFSET :offset";

        List<Product> products = executeDataQuery(dataQuery, filters, limit, offset);

        return new ProductSearchResult(products, totalCount);
    }

    /**
     * Build the WHERE clause from search filters.
     * Handles category ID and warranty status filters.
     */
    private String buildWhereClause(SearchFilters filters) {
        List<String> conditions = new ArrayList<>();

        // Category filter
        if (filters.getCategoryId() != null) {
            conditions.add("p.category_id = :categoryId");
        }

        // Warranty status filter
        if (filters.getStatus() != null) {
            String statusCondition = buildStatusCondition(filters.getStatus(), filters.getToday());
            conditions.add("(" + statusCondition + ")");
        }

        return conditions.isEmpty() ?
                "WHERE 1=1" :
                "WHERE " + String.join(" AND ", conditions);
    }

    /**
     * Build warranty status condition for the WHERE clause.
     * Conditions:
     * - ACTIVE: warrantyEndDate > today + 30 days
     * - EXPIRING_SOON: today <= warrantyEndDate <= today + 30 days
     * - EXPIRED: today > warrantyEndDate
     */
    private String buildStatusCondition(String status, LocalDate today) {
        String expr = warrantyEndDateExpr;
        String todayStr = today.toString();
        String todayPlus30Str = today.plusDays(30).toString();

        return switch(status) {
            case "ACTIVE" ->
                expr + " > DATE '" + todayPlus30Str + "'";
            case "EXPIRING_SOON" ->
                expr + " >= DATE '" + todayStr + "' AND " +
                expr + " <= DATE '" + todayPlus30Str + "'";
            case "EXPIRED" ->
                expr + " < DATE '" + todayStr + "'";
            default ->
                throw new ValidationException("Invalid warranty status: " + status);
        };
    }

    /**
     * Build the ORDER BY clause with field validation.
     * Validates sort field against whitelist to prevent SQL injection.
     * Uses the injected warrantyEndDateExpr for warranty end date sorting.
     */
    private String buildOrderByClause(String sortField, String sortDirection) {
        // Whitelist of allowed sort fields
        Map<String, String> fieldMapping = Map.ofEntries(
                Map.entry("id", "p.id"),
                Map.entry("name", "p.name"),
                Map.entry("purchaseDate", "p.purchase_date"),
                Map.entry("warrantyEndDate", warrantyEndDateExpr),
                Map.entry("warrantyMonths", "p.warranty_months"),
                Map.entry("categoryId", "p.category_id")
        );

        if (!fieldMapping.containsKey(sortField)) {
            throw new ValidationException("Invalid sort field: " + sortField + 
                ". Supported fields are: id, name, purchaseDate, warrantyEndDate, warrantyMonths, categoryId");
        }

        String columnExpr = fieldMapping.get(sortField);
        String direction = sortDirection.equalsIgnoreCase("DESC") ? "DESC" : "ASC";

        return " ORDER BY " + columnExpr + " " + direction;
    }

    /**
     * Execute COUNT query and return total count.
     */
    private long executeCountQuery(String query, SearchFilters filters) {
        Query q = entityManager.createNativeQuery(query);

        if (filters.getCategoryId() != null) {
            q.setParameter("categoryId", filters.getCategoryId());
        }

        Object result = q.getSingleResult();
        return ((Number) result).longValue();
    }

    /**
     * Execute data query and return Product entities.
     */
    private List<Product> executeDataQuery(String query, SearchFilters filters, int limit, int offset) {
        Query q = entityManager.createNativeQuery(query, Product.class);

        if (filters.getCategoryId() != null) {
            q.setParameter("categoryId", filters.getCategoryId());
        }

        q.setParameter("limit", limit);
        q.setParameter("offset", offset);

        @SuppressWarnings("unchecked")
        List<Product> results = q.getResultList();
        return results;
    }
}
