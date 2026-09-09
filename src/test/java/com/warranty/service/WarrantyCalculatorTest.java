package com.warranty.service;

import com.warranty.dto.internal.WarrantyStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Unit tests for WarrantyCalculator warranty status determination.
 * Verifies all acceptance criteria and boundary conditions.
 */
@DisplayName("WarrantyCalculator Status Determination")
class WarrantyCalculatorTest {

    // Test data: base date is 2024-06-15
    private static final LocalDate BASE_DATE = LocalDate.of(2024, 6, 15);

    @Test
    @DisplayName("AC 1: EXPIRED - warrantyEndDate=2024-01-15, today=2024-06-15")
    void testExpiredWarranty() {
        LocalDate warrantyEndDate = LocalDate.of(2024, 1, 15);
        WarrantyStatus status = WarrantyCalculator.calculateStatus(warrantyEndDate, BASE_DATE);
        assertEquals(WarrantyStatus.EXPIRED, status);
    }

    @Test
    @DisplayName("AC 2: EXPIRING_SOON - warrantyEndDate=2024-07-10, today=2024-06-15")
    void testExpiringWarranty() {
        LocalDate warrantyEndDate = LocalDate.of(2024, 7, 10);
        WarrantyStatus status = WarrantyCalculator.calculateStatus(warrantyEndDate, BASE_DATE);
        assertEquals(WarrantyStatus.EXPIRING_SOON, status);
    }

    @Test
    @DisplayName("AC 3: ACTIVE - warrantyEndDate=2025-06-15, today=2024-06-15")
    void testActiveWarranty() {
        LocalDate warrantyEndDate = LocalDate.of(2025, 6, 15);
        WarrantyStatus status = WarrantyCalculator.calculateStatus(warrantyEndDate, BASE_DATE);
        assertEquals(WarrantyStatus.ACTIVE, status);
    }

    @Test
    @DisplayName("AC 4: Boundary Inclusive - warrantyEndDate=today+30 returns EXPIRING_SOON")
    void testBoundaryInclusiveThirtyDays() {
        LocalDate warrantyEndDate = BASE_DATE.plusDays(30); // 2024-07-15
        WarrantyStatus status = WarrantyCalculator.calculateStatus(warrantyEndDate, BASE_DATE);
        assertEquals(WarrantyStatus.EXPIRING_SOON, status);
    }

    @Test
    @DisplayName("AC 5: Boundary Exclusive - warrantyEndDate=today+31 returns ACTIVE")
    void testBoundaryExclusiveThirtyOneDays() {
        LocalDate warrantyEndDate = BASE_DATE.plusDays(31); // 2024-07-16
        WarrantyStatus status = WarrantyCalculator.calculateStatus(warrantyEndDate, BASE_DATE);
        assertEquals(WarrantyStatus.ACTIVE, status);
    }

    @Test
    @DisplayName("Edge case: warrantyEndDate equals today returns EXPIRING_SOON")
    void testWarrantyEndsToday() {
        WarrantyStatus status = WarrantyCalculator.calculateStatus(BASE_DATE, BASE_DATE);
        assertEquals(WarrantyStatus.EXPIRING_SOON, status);
    }

    @Test
    @DisplayName("Edge case: warrantyEndDate is yesterday returns EXPIRED")
    void testWarrantyExpiredYesterday() {
        LocalDate warrantyEndDate = BASE_DATE.minusDays(1);
        WarrantyStatus status = WarrantyCalculator.calculateStatus(warrantyEndDate, BASE_DATE);
        assertEquals(WarrantyStatus.EXPIRED, status);
    }

    @Test
    @DisplayName("Edge case: warrantyEndDate is tomorrow returns EXPIRING_SOON")
    void testWarrantyExpiresTomorrow() {
        LocalDate warrantyEndDate = BASE_DATE.plusDays(1);
        WarrantyStatus status = WarrantyCalculator.calculateStatus(warrantyEndDate, BASE_DATE);
        assertEquals(WarrantyStatus.EXPIRING_SOON, status);
    }

    @Test
    @DisplayName("Edge case: warrantyEndDate is 29 days from today returns EXPIRING_SOON")
    void testWarrantyExpires29DaysFromToday() {
        LocalDate warrantyEndDate = BASE_DATE.plusDays(29);
        WarrantyStatus status = WarrantyCalculator.calculateStatus(warrantyEndDate, BASE_DATE);
        assertEquals(WarrantyStatus.EXPIRING_SOON, status);
    }

    @Test
    @DisplayName("Edge case: warrantyEndDate is 32 days from today returns ACTIVE")
    void testWarrantyExpires32DaysFromToday() {
        LocalDate warrantyEndDate = BASE_DATE.plusDays(32);
        WarrantyStatus status = WarrantyCalculator.calculateStatus(warrantyEndDate, BASE_DATE);
        assertEquals(WarrantyStatus.ACTIVE, status);
    }

    @Test
    @DisplayName("Edge case: warrantyEndDate is 60 days from today returns ACTIVE")
    void testWarrantyExpires60DaysFromToday() {
        LocalDate warrantyEndDate = BASE_DATE.plusDays(60);
        WarrantyStatus status = WarrantyCalculator.calculateStatus(warrantyEndDate, BASE_DATE);
        assertEquals(WarrantyStatus.ACTIVE, status);
    }

    @Test
    @DisplayName("Edge case: warrantyEndDate is 1 day from today returns EXPIRING_SOON")
    void testWarrantyExpires1DayFromToday() {
        LocalDate warrantyEndDate = BASE_DATE.plusDays(1);
        WarrantyStatus status = WarrantyCalculator.calculateStatus(warrantyEndDate, BASE_DATE);
        assertEquals(WarrantyStatus.EXPIRING_SOON, status);
    }

    @Test
    @DisplayName("Edge case: very old warranty (many years expired) returns EXPIRED")
    void testVeryOldExpiredWarranty() {
        LocalDate warrantyEndDate = LocalDate.of(2000, 1, 1);
        WarrantyStatus status = WarrantyCalculator.calculateStatus(warrantyEndDate, BASE_DATE);
        assertEquals(WarrantyStatus.EXPIRED, status);
    }

    @Test
    @DisplayName("Edge case: very far future warranty (many years active) returns ACTIVE")
    void testVeryFarFutureActiveWarranty() {
        LocalDate warrantyEndDate = LocalDate.of(2050, 1, 1);
        WarrantyStatus status = WarrantyCalculator.calculateStatus(warrantyEndDate, BASE_DATE);
        assertEquals(WarrantyStatus.ACTIVE, status);
    }
}
