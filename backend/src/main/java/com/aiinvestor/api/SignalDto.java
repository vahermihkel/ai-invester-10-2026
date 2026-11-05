package com.aiinvestor.api;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import com.aiinvestor.signal.Signal;
import com.aiinvestor.signal.SignalAction;
import com.aiinvestor.signal.SignalStatus;

/** Must match Signal in frontend/src/types.ts. */
public record SignalDto(
        long id,
        Instant createdAt,
        String accountId,
        String broker,
        String symbol,
        SignalAction action,
        OrderSizeDto size,
        double confidence,
        String reasoning,
        SignalStatus status,
        String rejectionReason,
        String aiProvider,
        String aiModel,
        List<Map<String, String>> fallbackFrom) {

    static SignalDto of(Signal s, String broker) {
        return new SignalDto(s.getId(), s.getCreatedAt(), s.getAccountId(), broker, s.getSymbol(), s.getAction(),
                OrderSizeDto.of(s.getSizeType(), s.getSizeValue()), s.getConfidence().doubleValue(), s.getReasoning(),
                s.getStatus(), s.getRejectionReason(), s.getAiProviderId(), s.getAiModel(), s.getFallbackFrom());
    }
}
