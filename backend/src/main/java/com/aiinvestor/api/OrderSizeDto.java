package com.aiinvestor.api;

import java.math.BigDecimal;

import com.aiinvestor.domain.SizeType;

/** Must match OrderSize in frontend/src/types.ts. */
public record OrderSizeDto(SizeType type, String value) {

    static OrderSizeDto of(SizeType type, BigDecimal value) {
        return type == null ? null : new OrderSizeDto(type, ApiDecimals.toApi(value));
    }
}
