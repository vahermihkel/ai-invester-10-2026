package com.aiinvestor.ai;

/** rawResponse is untrusted until the signal parser has validated it. */
public record AiResponse(
        String providerId,
        String model,
        String rawResponse,
        Integer inputTokens,
        Integer outputTokens) {
}
