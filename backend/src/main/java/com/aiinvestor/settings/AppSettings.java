package com.aiinvestor.settings;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** The single row of app state. */
@Entity
@Table(name = "app_settings")
public class AppSettings {

    public static final short SINGLE_ROW_ID = 1;

    @Id
    private Short id;

    @Column(name = "active_account_id")
    private String activeAccountId;

    @Column(name = "last_seen_env_active_account")
    private String lastSeenEnvActiveAccount;

    @Column(name = "trading_enabled", nullable = false)
    private boolean tradingEnabled;

    @Column(name = "analysis_interval_minutes", nullable = false)
    private int analysisIntervalMinutes;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected AppSettings() {
    }

    public String getActiveAccountId() {
        return activeAccountId;
    }

    public void setActiveAccountId(String activeAccountId) {
        this.activeAccountId = activeAccountId;
        this.updatedAt = Instant.now();
    }

    public String getLastSeenEnvActiveAccount() {
        return lastSeenEnvActiveAccount;
    }

    public void setLastSeenEnvActiveAccount(String lastSeenEnvActiveAccount) {
        this.lastSeenEnvActiveAccount = lastSeenEnvActiveAccount;
        this.updatedAt = Instant.now();
    }

    public boolean isTradingEnabled() {
        return tradingEnabled;
    }

    public void setTradingEnabled(boolean tradingEnabled) {
        this.tradingEnabled = tradingEnabled;
        this.updatedAt = Instant.now();
    }

    public int getAnalysisIntervalMinutes() {
        return analysisIntervalMinutes;
    }

    public void setAnalysisIntervalMinutes(int analysisIntervalMinutes) {
        this.analysisIntervalMinutes = analysisIntervalMinutes;
        this.updatedAt = Instant.now();
    }
}
