package com.aiinvestor.audit;

import org.springframework.stereotype.Service;

/** Records every decision and error that isn't already stored elsewhere. Never put secrets in the message. */
@Service
public class AuditService {

    private final AuditLogRepository log;

    public AuditService(AuditLogRepository log) {
        this.log = log;
    }

    /** @param accountId the account alias, or null if the event isn't about one account */
    public void record(AuditLevel level, String eventType, String accountId, String message) {
        log.save(new AuditLogEntry(level, eventType, accountId, message));
    }
}
