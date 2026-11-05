package com.aiinvestor.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** ACTIVE_ACCOUNT and ALLOW_LIVE_TRADING from .env. Live trading stays off unless explicitly set to true. */
@ConfigurationProperties("app.trading")
public record TradingProperties(String activeAccount, boolean allowLiveTrading) {

    public TradingProperties {
        activeAccount = activeAccount == null || activeAccount.isBlank() ? null : activeAccount.trim();
    }
}
