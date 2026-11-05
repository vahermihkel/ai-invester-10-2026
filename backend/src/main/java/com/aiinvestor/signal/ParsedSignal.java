package com.aiinvestor.signal;

import java.math.BigDecimal;

import com.aiinvestor.domain.OrderSize;

/** A validated, broker-neutral AI decision. size is null for HOLD. */
public record ParsedSignal(
        String symbol,
        SignalAction action,
        OrderSize size,
        BigDecimal confidence,
        String reasoning) {

    public static ParsedSignal hold(String symbol, String reasoning) {
        return new ParsedSignal(symbol, SignalAction.HOLD, null, BigDecimal.ZERO, reasoning);
    }
}
