package com.aiinvestor.ai;

import java.time.Instant;

/** A technical failure. The message must never contain API keys. */
public class AiProviderException extends RuntimeException {

    private final AiFailureReason reason;
    private final Instant resetAt;

    /** @param resetAt when the provider can be used again, or null if its error didn't say */
    public AiProviderException(AiFailureReason reason, Instant resetAt, String message) {
        super(message);
        this.reason = reason;
        this.resetAt = resetAt;
    }

    public AiFailureReason getReason() {
        return reason;
    }

    public Instant getResetAt() {
        return resetAt;
    }
}
