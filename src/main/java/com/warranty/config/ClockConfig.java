package com.warranty.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.ZoneId;

/**
 * Clock Configuration for the Warranty Tracker API.
 * This configuration provides a Spring-managed Clock bean configured for the Asia/Bangkok timezone.
 * The Clock is used throughout the application for consistent date/time calculations, particularly
 * for warranty status determination (ACTIVE, EXPIRING_SOON, EXPIRED).
 */
@Configuration
public class ClockConfig {

    /**
     * Create a Clock bean configured for Asia/Bangkok timezone.
     * This ensures all date/time operations use a consistent, application-wide timezone.
     *
     * @return Clock instance configured for Asia/Bangkok timezone
     */
    @Bean
    public Clock clock() {
        return Clock.system(ZoneId.of("Asia/Bangkok"));
    }
}
