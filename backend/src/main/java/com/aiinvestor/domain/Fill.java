package com.aiinvestor.domain;

import java.math.BigDecimal;
import java.time.Instant;

/** One execution. commissionCurrency can differ from currency. */
public record Fill(
        String brokerFillId,
        String brokerOrderId,
        BigDecimal quantity,
        BigDecimal price,
        BigDecimal commission,
        String commissionCurrency,
        String currency,
        Instant filledAt) {
}
