package com.aiinvestor.risk;

import java.util.List;
import java.util.Optional;

import org.springframework.data.repository.Repository;

public interface RiskLimitsRepository extends Repository<RiskLimits, String> {

    List<RiskLimits> findAllByOrderByAccountId();

    Optional<RiskLimits> findById(String accountId);

    RiskLimits save(RiskLimits limits);
}
