package com.warranty.service;

import com.warranty.dto.internal.WarrantyStatus;
import java.time.LocalDate;

/**
 * Pure, stateless utility class for warranty status calculations.
 * Determines warranty status based on warranty end date and current date.
 */
public class WarrantyCalculator {
    private static final int EXPIRING_SOON_DAYS = 30;

    /**
     * Calculates the warranty status based on warranty end date and today's date.
     *
     * Rules:
     * - EXPIRED: today > warrantyEndDate
     * - EXPIRING_SOON: today <= warrantyEndDate AND warrantyEndDate <= today + 30 days
     * - ACTIVE: warrantyEndDate > today + 30 days
     *
     * @param warrantyEndDate the date when the warranty expires
     * @param today the current date to compare against
     * @return the warranty status (ACTIVE, EXPIRING_SOON, or EXPIRED)
     */
    public static WarrantyStatus calculateStatus(LocalDate warrantyEndDate, LocalDate today) {
        if (today.isAfter(warrantyEndDate)) {
            return WarrantyStatus.EXPIRED;
        }

        LocalDate expiringThreshold = today.plusDays(EXPIRING_SOON_DAYS);
        if (warrantyEndDate.isBefore(expiringThreshold) || warrantyEndDate.isEqual(expiringThreshold)) {
            return WarrantyStatus.EXPIRING_SOON;
        }

        return WarrantyStatus.ACTIVE;
    }
}
