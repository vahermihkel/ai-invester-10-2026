package com.aiinvestor.domain;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

class OrderRequestTest {

    private static final Instrument AAPL =
            new Instrument("AAPL", "", "USD", "STOCK", "1", BigDecimal.ONE, null, null, null);
    private static final OrderSize ONE_UNIT = new OrderSize(SizeType.UNITS, BigDecimal.ONE);

    private static OrderRequest order(String clientOrderId, PositionEffect effect, String positionId,
                                      OrderType type, BigDecimal limitPrice) {
        return new OrderRequest(clientOrderId, AAPL, OrderSide.BUY, effect, positionId, ONE_UNIT, type,
                limitPrice, null, "USD");
    }

    @Test
    void acceptsValidOrders() {
        assertThatCode(() -> order("c1", null, null, OrderType.MARKET, null)).doesNotThrowAnyException();
        assertThatCode(() -> order("c1", null, null, OrderType.LIMIT, BigDecimal.TEN)).doesNotThrowAnyException();
        assertThatCode(() -> order("c1", PositionEffect.CLOSE, "p1", OrderType.MARKET, null)).doesNotThrowAnyException();
    }

    @Test
    void requiresClientOrderId() {
        assertThatThrownBy(() -> order(" ", null, null, OrderType.MARKET, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void refusesOrderTypesWeDoNotPlace() {
        assertThatThrownBy(() -> order("c1", null, null, OrderType.OTHER, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void limitOrderNeedsPositivePrice() {
        assertThatThrownBy(() -> order("c1", null, null, OrderType.LIMIT, null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> order("c1", null, null, OrderType.LIMIT, BigDecimal.ZERO))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void closingNeedsPositionId() {
        assertThatThrownBy(() -> order("c1", PositionEffect.CLOSE, null, OrderType.MARKET, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void sizeMustBePositive() {
        assertThatThrownBy(() -> new OrderSize(SizeType.UNITS, BigDecimal.ZERO))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
