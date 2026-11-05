package com.aiinvestor.risk;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RiskLimitsService {

    private final RiskLimitsRepository limits;

    public RiskLimitsService(RiskLimitsRepository limits) {
        this.limits = limits;
    }

    public List<RiskLimits> all() {
        return limits.findAllByOrderByAccountId();
    }

    @Transactional
    public void createIfMissing(String accountId) {
        if (limits.findById(accountId).isEmpty()) {
            limits.save(RiskLimits.nothingAllowed(accountId));
        }
    }

    /** @throws IllegalArgumentException if the account has no limits row, i.e. it isn't a known account */
    @Transactional
    public void update(String accountId, BigDecimal maxPositionValue, BigDecimal maxOrderValue,
                       BigDecimal maxDailyLoss, int maxTradesPerDay, List<String> allowedSymbols) {
        limits.findById(accountId)
                .orElseThrow(() -> new IllegalArgumentException("Unknown account " + accountId))
                .update(maxPositionValue, maxOrderValue, maxDailyLoss, maxTradesPerDay, allowedSymbols);
    }
}
