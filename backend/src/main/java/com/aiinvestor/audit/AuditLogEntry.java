package com.aiinvestor.audit;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "audit_log")
public class AuditLogEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_at", nullable = false)
    private Instant eventAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AuditLevel level;

    @Column(name = "event_type", nullable = false)
    private String eventType;

    @Column(name = "account_id")
    private String accountId;

    @Column(nullable = false)
    private String message;

    protected AuditLogEntry() {
    }

    AuditLogEntry(AuditLevel level, String eventType, String accountId, String message) {
        this.eventAt = Instant.now();
        this.level = level;
        this.eventType = eventType;
        this.accountId = accountId;
        this.message = message;
    }
}
