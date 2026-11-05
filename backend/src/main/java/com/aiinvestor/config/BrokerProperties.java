package com.aiinvestor.config;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import com.aiinvestor.domain.TradingMode;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/** The known broker accounts from application.yml. Credentials are read from .env by the adapters, never bound here. */
@Validated
@ConfigurationProperties("app.brokers")
public record BrokerProperties(List<@Valid Account> accounts) {

    public BrokerProperties {
        accounts = accounts == null ? List.of() : List.copyOf(accounts);
    }

    public record Account(
            @NotBlank String alias,
            @NotBlank String brokerType,
            @NotBlank String displayName,
            @NotNull TradingMode tradingMode,
            @NotBlank String baseCurrency,
            List<String> requiredEnv) {
    }
}
