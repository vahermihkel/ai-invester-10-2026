package com.aiinvestor.api;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/** Must match RiskSettings in frontend/src/types.ts. */
public record RiskSettingsDto(
        @Min(1) int analysisIntervalMinutes,
        @NotNull List<@Valid AccountRisk> accounts) {

    public record AccountRisk(@NotBlank String accountId, @NotNull @Valid Limits limits) {
    }

    public record Limits(
            @NotNull @DecimalMin("0") @Digits(integer = 20, fraction = 18) String maxPositionValue,
            @NotNull @DecimalMin("0") @Digits(integer = 20, fraction = 18) String maxOrderValue,
            @NotNull @DecimalMin("0") @Digits(integer = 20, fraction = 18) String maxDailyLoss,
            @Min(0) int maxTradesPerDay,
            @NotNull List<@NotBlank String> allowedSymbols) {
    }
}
