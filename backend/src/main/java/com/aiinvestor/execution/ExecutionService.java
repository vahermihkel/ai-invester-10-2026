package com.aiinvestor.execution;

import org.springframework.stereotype.Service;

import com.aiinvestor.signal.ParsedSignal;

/** Turns approved signals into broker orders: see docs/rules/safety.md. */
@Service
public class ExecutionService {

    /** Refuses until execution is built, so nothing can reach a broker by accident. */
    public void execute(ParsedSignal signal, long signalId, String accountId) {
        throw new IllegalStateException("Order execution is not built yet");
    }
}
