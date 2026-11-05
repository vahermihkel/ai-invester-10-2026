package com.aiinvestor.api;

import java.time.Instant;

import com.aiinvestor.domain.OrderSide;
import com.aiinvestor.domain.OrderSource;
import com.aiinvestor.domain.OrderStatus;
import com.aiinvestor.domain.OrderType;
import com.aiinvestor.domain.PositionEffect;
import com.aiinvestor.execution.OrderRecord;

/** Must match Order in frontend/src/types.ts. */
public record OrderDto(
        long id,
        Instant createdAt,
        String accountId,
        String broker,
        OrderSource source,
        String reasoning,
        String clientOrderId,
        String brokerOrderId,
        String symbol,
        String assetClass,
        OrderSide side,
        PositionEffect positionEffect,
        String brokerPositionId,
        OrderSizeDto size,
        OrderType orderType,
        String limitPrice,
        String leverage,
        OrderStatus status,
        String filledQuantity,
        String avgFillPrice,
        String commission,
        String currency,
        Long signalId,
        Instant submittedAt,
        Instant filledAt,
        Instant cancelledAt,
        Instant rejectedAt,
        String closeReason) {

    static OrderDto of(OrderRecord o, String broker) {
        return new OrderDto(o.getId(), o.getCreatedAt(), o.getAccountId(), broker, o.getSource(), o.getReasoning(),
                o.getClientOrderId(), o.getBrokerOrderId(), o.getSymbol(), o.getAssetClass(), o.getSide(),
                o.getPositionEffect(), o.getBrokerPositionId(), OrderSizeDto.of(o.getSizeType(), o.getSizeValue()),
                o.getOrderType(), ApiDecimals.toApi(o.getLimitPrice()), ApiDecimals.toApi(o.getLeverage()), o.getStatus(),
                ApiDecimals.toApi(o.getFilledQuantity()), ApiDecimals.toApi(o.getAvgFillPrice()),
                ApiDecimals.toApi(o.getCommission()), o.getCurrency(), o.getSignalId(), o.getSubmittedAt(),
                o.getFilledAt(), o.getCancelledAt(), o.getRejectedAt(), o.getCloseReason());
    }
}
