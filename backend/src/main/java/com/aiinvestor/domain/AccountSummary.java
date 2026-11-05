package com.aiinvestor.domain;

import java.math.BigDecimal;

/** Live account values from the broker. investedValue and reservedForOrders are null when it doesn't report them. */
public record AccountSummary(
        BigDecimal equity,
        BigDecimal cash,
        BigDecimal investedValue,
        BigDecimal reservedForOrders,
        BigDecimal dailyPnl,
        BigDecimal unrealizedPnl,
        String currency) {
}
