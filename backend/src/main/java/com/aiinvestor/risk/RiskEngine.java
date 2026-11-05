package com.aiinvestor.risk;

import org.springframework.stereotype.Service;

import com.aiinvestor.signal.ParsedSignal;

/** Checks every signal against the account's limits, in code rather than in the AI prompt: see docs/rules/safety.md. */
@Service
public class RiskEngine {

    /** Rejects everything until the limit checks are built. */
    public RiskDecision check(ParsedSignal signal, String accountId) {
        return RiskDecision.rejected("Risk engine is not built yet");
    }
}
