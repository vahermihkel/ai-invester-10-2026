package com.aiinvestor.ai;

/** Technical failures that trigger a fallback. Adapters map native error codes to these. */
public enum AiFailureReason {
    QUOTA_EXHAUSTED,
    RATE_LIMITED,
    BUDGET_REACHED,
    AUTH_ERROR,
    SERVER_ERROR,
    TIMEOUT,
    CONNECTION_ERROR,
    INVALID_RESPONSE
}
