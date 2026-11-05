package com.aiinvestor.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    // Only the local frontend, never a wildcard: see docs/rules/configuration.md.
    private static final String LOCAL_FRONTEND = "http://localhost:5173";

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins(LOCAL_FRONTEND)
                .allowedMethods("GET", "POST", "PUT");
    }
}
