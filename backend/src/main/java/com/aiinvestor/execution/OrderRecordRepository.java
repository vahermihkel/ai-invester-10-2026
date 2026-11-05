package com.aiinvestor.execution;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.Repository;

public interface OrderRecordRepository extends Repository<OrderRecord, Long> {

    List<OrderRecord> findByAccountIdOrderByCreatedAtDesc(String accountId, Pageable page);
}
