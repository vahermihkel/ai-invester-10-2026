package com.aiinvestor.account;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.aiinvestor.config.BrokerProperties;
import com.aiinvestor.risk.RiskLimitsService;

@Service
public class AccountService {

    private static final Logger log = LoggerFactory.getLogger(AccountService.class);

    private final BrokerProperties properties;
    private final BrokerAccountRepository accounts;
    private final RiskLimitsService riskLimits;
    private final Environment environment;

    public AccountService(BrokerProperties properties, BrokerAccountRepository accounts,
                          RiskLimitsService riskLimits, Environment environment) {
        this.properties = properties;
        this.accounts = accounts;
        this.riskLimits = riskLimits;
        this.environment = environment;
    }

    public List<BrokerAccount> list() {
        return accounts.findAllByOrderById();
    }

    public Optional<BrokerAccount> find(String accountId) {
        return accounts.findById(accountId);
    }

    /**
     * Saves the configured accounts to the database. An account is enabled only when all its .env values are filled in.
     * Refuses to start if an alias changed broker or trading mode, because its history would then be wrong.
     */
    @Transactional
    public void syncFromConfig() {
        Set<String> configured = new HashSet<>();
        for (BrokerProperties.Account config : properties.accounts()) {
            configured.add(config.alias());
            boolean enabled = allVariablesFilledIn(config);
            accounts.findById(config.alias()).ifPresentOrElse(
                    existing -> {
                        requireSameBrokerAndMode(existing, config);
                        existing.update(config.displayName(), config.baseCurrency(), enabled);
                    },
                    () -> accounts.save(new BrokerAccount(config.alias(), config.brokerType(), config.displayName(),
                            config.tradingMode(), config.baseCurrency(), enabled)));
            riskLimits.createIfMissing(config.alias());
        }
        // Rows stay for their history, but an account no longer in the config can't be used.
        accounts.findAllByOrderById().stream()
                .filter(account -> !configured.contains(account.getId()) && account.isEnabled())
                .forEach(account -> {
                    log.warn("Account {} is no longer configured, so it is disabled", account.getId());
                    account.disable();
                });
    }

    private boolean allVariablesFilledIn(BrokerProperties.Account config) {
        boolean enabled = true;
        for (String variable : config.requiredEnv()) {
            String value = environment.getProperty(variable);
            if (value == null || value.isBlank()) {
                // Only the variable's name is logged, never a value.
                log.warn("Account {} is disabled: {} is empty in .env", config.alias(), variable);
                enabled = false;
            }
        }
        return enabled;
    }

    private static void requireSameBrokerAndMode(BrokerAccount existing, BrokerProperties.Account config) {
        if (!existing.getBroker().equals(config.brokerType()) || existing.getTradingMode() != config.tradingMode()) {
            throw new IllegalStateException("Account " + config.alias() + " is stored as " + existing.getBroker() + "/"
                    + existing.getTradingMode() + " but configured as " + config.brokerType() + "/"
                    + config.tradingMode() + ". An alias can't change broker or trading mode: use a new alias.");
        }
    }
}
