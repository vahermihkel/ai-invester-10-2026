package com.aiinvestor.account;

import java.time.Instant;

import com.aiinvestor.domain.TradingMode;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** A configured account. id is our alias, never the real broker account number. */
@Entity
@Table(name = "broker_accounts")
public class BrokerAccount {

    @Id
    private String id;

    @Column(nullable = false)
    private String broker;

    @Column(name = "display_name", nullable = false)
    private String displayName;

    @Enumerated(EnumType.STRING)
    @Column(name = "trading_mode", nullable = false)
    private TradingMode tradingMode;

    @Column(name = "base_currency", nullable = false)
    private String baseCurrency;

    @Column(nullable = false)
    private boolean enabled;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected BrokerAccount() {
    }

    public BrokerAccount(String id, String broker, String displayName, TradingMode tradingMode,
                         String baseCurrency, boolean enabled) {
        this.id = id;
        this.broker = broker;
        this.displayName = displayName;
        this.tradingMode = tradingMode;
        this.baseCurrency = baseCurrency;
        this.enabled = enabled;
        this.updatedAt = Instant.now();
    }

    /** Broker and trading mode can't change here: an alias keeps them forever. */
    void update(String displayName, String baseCurrency, boolean enabled) {
        this.displayName = displayName;
        this.baseCurrency = baseCurrency;
        this.enabled = enabled;
        this.updatedAt = Instant.now();
    }

    void disable() {
        this.enabled = false;
        this.updatedAt = Instant.now();
    }

    public String getId() {
        return id;
    }

    public String getBroker() {
        return broker;
    }

    public String getDisplayName() {
        return displayName;
    }

    public TradingMode getTradingMode() {
        return tradingMode;
    }

    public String getBaseCurrency() {
        return baseCurrency;
    }

    public boolean isEnabled() {
        return enabled;
    }
}
