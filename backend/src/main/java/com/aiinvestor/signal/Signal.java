package com.aiinvestor.signal;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import com.aiinvestor.domain.SizeType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** A stored AI decision and its risk-check outcome. */
@Entity
@Table(name = "signals")
public class Signal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "account_id", nullable = false)
    private String accountId;

    @Column(name = "ai_provider_id")
    private String aiProviderId;

    @Column(name = "ai_model")
    private String aiModel;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "fallback_from")
    private List<Map<String, String>> fallbackFrom;

    @Column(nullable = false)
    private String symbol;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SignalAction action;

    @Enumerated(EnumType.STRING)
    @Column(name = "size_type")
    private SizeType sizeType;

    @Column(name = "size_value", precision = 38, scale = 18)
    private BigDecimal sizeValue;

    @Column(nullable = false, precision = 5, scale = 4)
    private BigDecimal confidence;

    @Column(nullable = false)
    private String reasoning;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SignalStatus status;

    @Column(name = "rejection_reason")
    private String rejectionReason;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected Signal() {
    }

    public Long getId() {
        return id;
    }

    public String getAccountId() {
        return accountId;
    }

    public String getAiProviderId() {
        return aiProviderId;
    }

    public String getAiModel() {
        return aiModel;
    }

    public List<Map<String, String>> getFallbackFrom() {
        return fallbackFrom;
    }

    public String getSymbol() {
        return symbol;
    }

    public SignalAction getAction() {
        return action;
    }

    public SizeType getSizeType() {
        return sizeType;
    }

    public BigDecimal getSizeValue() {
        return sizeValue;
    }

    public BigDecimal getConfidence() {
        return confidence;
    }

    public String getReasoning() {
        return reasoning;
    }

    public SignalStatus getStatus() {
        return status;
    }

    public String getRejectionReason() {
        return rejectionReason;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
