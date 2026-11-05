package com.aiinvestor.api;

import java.math.BigDecimal;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.aiinvestor.audit.AuditLevel;
import com.aiinvestor.audit.AuditService;
import com.aiinvestor.risk.RiskLimits;
import com.aiinvestor.risk.RiskLimitsService;
import com.aiinvestor.settings.SettingsService;

@Service
public class RiskSettingsService {

    private final SettingsService settings;
    private final RiskLimitsService riskLimits;
    private final AuditService audit;

    public RiskSettingsService(SettingsService settings, RiskLimitsService riskLimits, AuditService audit) {
        this.settings = settings;
        this.riskLimits = riskLimits;
        this.audit = audit;
    }

    public RiskSettingsDto current() {
        return new RiskSettingsDto(settings.current().getAnalysisIntervalMinutes(),
                riskLimits.all().stream().map(RiskSettingsService::toDto).toList());
    }

    /** All or nothing. @throws IllegalArgumentException if an account is unknown */
    @Transactional
    public void save(RiskSettingsDto request) {
        settings.currentForUpdate().setAnalysisIntervalMinutes(request.analysisIntervalMinutes());
        for (RiskSettingsDto.AccountRisk account : request.accounts()) {
            RiskSettingsDto.Limits limits = account.limits();
            riskLimits.update(account.accountId(),
                    new BigDecimal(limits.maxPositionValue()),
                    new BigDecimal(limits.maxOrderValue()),
                    new BigDecimal(limits.maxDailyLoss()),
                    limits.maxTradesPerDay(),
                    limits.allowedSymbols());
            audit.record(AuditLevel.INFO, "RISK_LIMITS_SAVED", account.accountId(),
                    "Risk limits saved: " + limits);
        }
    }

    private static RiskSettingsDto.AccountRisk toDto(RiskLimits limits) {
        return new RiskSettingsDto.AccountRisk(limits.getAccountId(), new RiskSettingsDto.Limits(
                ApiDecimals.toApi(limits.getMaxPositionValue()),
                ApiDecimals.toApi(limits.getMaxOrderValue()),
                ApiDecimals.toApi(limits.getMaxDailyLoss()),
                limits.getMaxTradesPerDay(),
                limits.getAllowedSymbols()));
    }
}
