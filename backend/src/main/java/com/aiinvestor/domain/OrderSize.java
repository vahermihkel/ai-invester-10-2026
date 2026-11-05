package com.aiinvestor.domain;

import java.math.BigDecimal;
import java.util.Objects;

public record OrderSize(SizeType type, BigDecimal value) {

    public OrderSize {
        Objects.requireNonNull(type, "type");
        if (value == null || value.signum() <= 0) {
            throw new IllegalArgumentException("Order size must be positive");
        }
    }
}
