package com.aiinvestor.risk;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Limits per account, in the account's base currency. */
@Entity
@Table(name = "risk_limits")
public class RiskLimits {

    @Id
    @Column(name = "account_id")
    private String accountId;

    @Column(name = "max_position_value", nullable = false, precision = 38, scale = 18)
    private BigDecimal maxPositionValue;

    @Column(name = "max_order_value", nullable = false, precision = 38, scale = 18)
    private BigDecimal maxOrderValue;

    @Column(name = "max_daily_loss", nullable = false, precision = 38, scale = 18)
    private BigDecimal maxDailyLoss;

    @Column(name = "max_trades_per_day", nullable = false)
    private int maxTradesPerDay;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "allowed_symbols", nullable = false)
    private List<String> allowedSymbols;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected RiskLimits() {
    }

    /** Zero limits and no allowed symbols: nothing can trade until the user sets real limits. */
    static RiskLimits nothingAllowed(String accountId) {
        RiskLimits limits = new RiskLimits();
        limits.accountId = accountId;
        limits.update(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, 0, List.of());
        return limits;
    }

    void update(BigDecimal maxPositionValue, BigDecimal maxOrderValue, BigDecimal maxDailyLoss,
                int maxTradesPerDay, List<String> allowedSymbols) {
        this.maxPositionValue = maxPositionValue;
        this.maxOrderValue = maxOrderValue;
        this.maxDailyLoss = maxDailyLoss;
        this.maxTradesPerDay = maxTradesPerDay;
        this.allowedSymbols = List.copyOf(allowedSymbols);
        this.updatedAt = Instant.now();
    }

    public String getAccountId() {
        return accountId;
    }

    public BigDecimal getMaxPositionValue() {
        return maxPositionValue;
    }

    public BigDecimal getMaxOrderValue() {
        return maxOrderValue;
    }

    public BigDecimal getMaxDailyLoss() {
        return maxDailyLoss;
    }

    public int getMaxTradesPerDay() {
        return maxTradesPerDay;
    }

    public List<String> getAllowedSymbols() {
        return allowedSymbols;
    }
}
