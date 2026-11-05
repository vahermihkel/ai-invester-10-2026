package com.aiinvestor.config;

import java.time.Duration;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/** The configured AI providers. apiKeyEnv is a variable name; the key itself stays in .env. */
@Validated
@ConfigurationProperties("app.ai")
public record AiProperties(List<@Valid Provider> providers) {

    public AiProperties {
        providers = providers == null ? List.of() : List.copyOf(providers);
    }

    public record Provider(
            @NotBlank String id,
            @NotBlank String type,
            @NotBlank String model,
            String baseUrl,
            String apiKeyEnv,
            @NotBlank String tier,
            int priority,
            @NotNull Duration timeout) {
    }
}
