package com.example.ridematching.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI rideMatchingOpenApi() {
        return new OpenAPI().info(new Info()
                .title("Ride Matching API")
                .version("v1")
                .description("Register drivers, find the nearest available ones, book and complete rides. "
                        + "Errors share one body: {code, message, timestamp}."));
    }
}
