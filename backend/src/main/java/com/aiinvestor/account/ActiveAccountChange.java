package com.aiinvestor.account;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** One switch of the active account. Accounts are null when there was or is no active account. */
@Entity
@Table(name = "active_account_changes")
public class ActiveAccountChange {

    public static final String SOURCE_UI = "UI";
    public static final String SOURCE_ENV = "ENV";
    public static final String SOURCE_STARTUP = "STARTUP";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "from_account_id")
    private String fromAccountId;

    @Column(name = "to_account_id")
    private String toAccountId;

    @Column(nullable = false)
    private String source;

    @Column(name = "changed_at", nullable = false)
    private Instant changedAt;

    protected ActiveAccountChange() {
    }

    ActiveAccountChange(String fromAccountId, String toAccountId, String source) {
        this.fromAccountId = fromAccountId;
        this.toAccountId = toAccountId;
        this.source = source;
        this.changedAt = Instant.now();
    }
}
