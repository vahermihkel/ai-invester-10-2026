package com.aiinvestor.api;

import java.time.Instant;

/** Must match SystemStatus in frontend/src/types.ts. */
public record SystemStatus(
        String activeAccountId,
        boolean tradingEnabled,
        boolean brokerConnected,
        boolean marketOpen,
        Instant lastRunAt) {
}
