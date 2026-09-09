package com.warranty;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Main entry point for the Warranty Tracker REST API application.
 * This is a Spring Boot 3 application that provides REST endpoints for managing
 * product categories and tracking warranty expiration dates.
 */
@SpringBootApplication
public class WarrantyTrackerApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(WarrantyTrackerApiApplication.class, args);
    }
}
