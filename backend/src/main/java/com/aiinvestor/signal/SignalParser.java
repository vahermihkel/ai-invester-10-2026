package com.aiinvestor.signal;

import org.springframework.stereotype.Component;

/** Turns raw AI output into a signal. Anything malformed or ambiguous must become HOLD: see docs/rules/safety.md. */
@Component
public class SignalParser {

    /** Always HOLD until strict schema validation is built, so no AI output can lead to an order. */
    public ParsedSignal parse(String rawResponse, String symbol) {
        return ParsedSignal.hold(symbol, "Signal parsing is not built yet");
    }
}
