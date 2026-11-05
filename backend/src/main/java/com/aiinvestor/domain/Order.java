package com.aiinvestor.domain;

import java.math.BigDecimal;

/** An order as the broker reports it. clientOrderId is null for orders we didn't send. */
public record Order(
        String clientOrderId,
        String brokerOrderId,
        String symbol,
        OrderSide side,
        PositionEffect positionEffect,
        String brokerPositionId,
        OrderSize size,
        OrderType orderType,
        BigDecimal limitPrice,
        OrderStatus status,
        BigDecimal filledQuantity,
        BigDecimal avgFillPrice,
        String currency) {
}
