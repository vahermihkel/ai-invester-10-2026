package com.aiinvestor.execution;

import java.math.BigDecimal;
import java.time.Instant;

import com.aiinvestor.domain.OrderSide;
import com.aiinvestor.domain.OrderSource;
import com.aiinvestor.domain.OrderStatus;
import com.aiinvestor.domain.OrderType;
import com.aiinvestor.domain.PositionEffect;
import com.aiinvestor.domain.SizeType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** A stored order, as history. Read-only for now: nothing creates orders until execution is built. */
@Entity
@Table(name = "orders")
public class OrderRecord {

    @Id
    private Long id;

    @Column(name = "account_id", nullable = false)
    private String accountId;

    @Column(name = "signal_id")
    private Long signalId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderSource source;

    private String reasoning;

    @Column(name = "client_order_id")
    private String clientOrderId;

    @Column(name = "broker_order_id")
    private String brokerOrderId;

    @Column(nullable = false)
    private String symbol;

    @Column(name = "asset_class", nullable = false)
    private String assetClass;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderSide side;

    @Enumerated(EnumType.STRING)
    @Column(name = "position_effect")
    private PositionEffect positionEffect;

    @Column(name = "broker_position_id")
    private String brokerPositionId;

    @Enumerated(EnumType.STRING)
    @Column(name = "order_type", nullable = false)
    private OrderType orderType;

    @Enumerated(EnumType.STRING)
    @Column(name = "size_type", nullable = false)
    private SizeType sizeType;

    @Column(name = "size_value", nullable = false, precision = 38, scale = 18)
    private BigDecimal sizeValue;

    @Column(name = "limit_price", precision = 38, scale = 18)
    private BigDecimal limitPrice;

    @Column(precision = 10, scale = 4)
    private BigDecimal leverage;

    @Column(nullable = false)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus status;

    @Column(name = "filled_quantity", nullable = false, precision = 38, scale = 18)
    private BigDecimal filledQuantity;

    @Column(name = "avg_fill_price", precision = 38, scale = 18)
    private BigDecimal avgFillPrice;

    @Column(nullable = false, precision = 38, scale = 18)
    private BigDecimal commission;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "submitted_at")
    private Instant submittedAt;

    @Column(name = "filled_at")
    private Instant filledAt;

    @Column(name = "cancelled_at")
    private Instant cancelledAt;

    @Column(name = "rejected_at")
    private Instant rejectedAt;

    @Column(name = "close_reason")
    private String closeReason;

    protected OrderRecord() {
    }

    public Long getId() {
        return id;
    }

    public String getAccountId() {
        return accountId;
    }

    public Long getSignalId() {
        return signalId;
    }

    public OrderSource getSource() {
        return source;
    }

    public String getReasoning() {
        return reasoning;
    }

    public String getClientOrderId() {
        return clientOrderId;
    }

    public String getBrokerOrderId() {
        return brokerOrderId;
    }

    public String getSymbol() {
        return symbol;
    }

    public String getAssetClass() {
        return assetClass;
    }

    public OrderSide getSide() {
        return side;
    }

    public PositionEffect getPositionEffect() {
        return positionEffect;
    }

    public String getBrokerPositionId() {
        return brokerPositionId;
    }

    public OrderType getOrderType() {
        return orderType;
    }

    public SizeType getSizeType() {
        return sizeType;
    }

    public BigDecimal getSizeValue() {
        return sizeValue;
    }

    public BigDecimal getLimitPrice() {
        return limitPrice;
    }

    public BigDecimal getLeverage() {
        return leverage;
    }

    public String getCurrency() {
        return currency;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public BigDecimal getFilledQuantity() {
        return filledQuantity;
    }

    public BigDecimal getAvgFillPrice() {
        return avgFillPrice;
    }

    public BigDecimal getCommission() {
        return commission;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getSubmittedAt() {
        return submittedAt;
    }

    public Instant getFilledAt() {
        return filledAt;
    }

    public Instant getCancelledAt() {
        return cancelledAt;
    }

    public Instant getRejectedAt() {
        return rejectedAt;
    }

    public String getCloseReason() {
        return closeReason;
    }
}
