package com.warranty.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI/Swagger UI Configuration for the Warranty Tracker REST API.
 * This configuration customizes the OpenAPI documentation with API metadata
 * including title and version information.
 */
@Configuration
public class OpenApiConfig {

    /**
     * Configure the OpenAPI specification with custom metadata.
     *
     * @return configured OpenAPI instance with title and version
     */
    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Warranty Tracker REST API")
                        .version("1.0.0")
                        .description("REST API for managing product categories and tracking warranty expiration dates"));
    }
}
