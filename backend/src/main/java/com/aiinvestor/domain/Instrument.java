package com.aiinvestor.domain;

import java.math.BigDecimal;

/** A resolved instrument. minQuantity, quantityStep and priceTick are null when the broker doesn't report them. */
public record Instrument(
        String symbol,
        String exchange,
        String currency,
        String assetClass,
        String brokerInstrumentId,
        BigDecimal multiplier,
        BigDecimal minQuantity,
        BigDecimal quantityStep,
        BigDecimal priceTick) {
}
