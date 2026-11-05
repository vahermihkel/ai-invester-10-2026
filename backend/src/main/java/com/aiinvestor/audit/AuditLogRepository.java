package com.aiinvestor.audit;

import org.springframework.data.repository.Repository;

public interface AuditLogRepository extends Repository<AuditLogEntry, Long> {

    AuditLogEntry save(AuditLogEntry entry);
}
