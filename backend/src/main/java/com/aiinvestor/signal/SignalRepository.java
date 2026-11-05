package com.aiinvestor.signal;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.Repository;

public interface SignalRepository extends Repository<Signal, Long> {

    List<Signal> findByAccountIdOrderByCreatedAtDesc(String accountId, Pageable page);
}
