package com.aiinvestor.domain;

import java.math.BigDecimal;
import java.util.Objects;

/** An order we want to place. Rejects what we must never send, whichever broker it goes to. */
public record OrderRequest(
        String clientOrderId,
        Instrument instrument,
        OrderSide side,
        PositionEffect positionEffect,
        String brokerPositionId,
        OrderSize size,
        OrderType orderType,
        BigDecimal limitPrice,
        BigDecimal leverage,
        String currency) {

    public OrderRequest {
        if (clientOrderId == null || clientOrderId.isBlank()) {
            throw new IllegalArgumentException("clientOrderId is required: it is what makes orders idempotent");
        }
        Objects.requireNonNull(instrument, "instrument");
        Objects.requireNonNull(side, "side");
        Objects.requireNonNull(size, "size");
        Objects.requireNonNull(orderType, "orderType");
        Objects.requireNonNull(currency, "currency");
        if (orderType == OrderType.OTHER) {
            throw new IllegalArgumentException("We only place MARKET and LIMIT orders");
        }
        if (orderType == OrderType.LIMIT && (limitPrice == null || limitPrice.signum() <= 0)) {
            throw new IllegalArgumentException("A LIMIT order needs a positive limit price");
        }
        if (positionEffect == PositionEffect.CLOSE && (brokerPositionId == null || brokerPositionId.isBlank())) {
            throw new IllegalArgumentException("Closing a position needs its brokerPositionId");
        }
    }
}
