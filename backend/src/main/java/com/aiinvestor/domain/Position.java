package com.aiinvestor.domain;

import java.math.BigDecimal;

/** quantity is always positive, side gives the direction. brokerPositionId is null on netting brokers. */
public record Position(
        String symbol,
        String exchange,
        String assetClass,
        PositionSide side,
        String brokerPositionId,
        BigDecimal quantity,
        BigDecimal avgPrice,
        BigDecimal marketPrice,
        BigDecimal marketValue,
        BigDecimal unrealizedPnl,
        String currency,
        BigDecimal leverage) {
}
