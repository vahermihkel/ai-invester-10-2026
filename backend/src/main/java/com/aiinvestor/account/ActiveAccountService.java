package com.aiinvestor.account;

import java.util.Objects;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.aiinvestor.audit.AuditLevel;
import com.aiinvestor.audit.AuditService;
import com.aiinvestor.config.TradingProperties;
import com.aiinvestor.domain.TradingMode;
import com.aiinvestor.settings.AppSettings;
import com.aiinvestor.settings.SettingsService;

/** The one place core code asks for the active account; never cache it elsewhere. Rules: docs/rules/brokers.md. */
@Service
public class ActiveAccountService {

    private static final Logger log = LoggerFactory.getLogger(ActiveAccountService.class);

    private final SettingsService settings;
    private final AccountService accounts;
    private final ActiveAccountChangeRepository changes;
    private final AuditService audit;
    private final TradingProperties trading;

    public ActiveAccountService(SettingsService settings, AccountService accounts,
                                ActiveAccountChangeRepository changes, AuditService audit, TradingProperties trading) {
        this.settings = settings;
        this.accounts = accounts;
        this.changes = changes;
        this.audit = audit;
        this.trading = trading;
    }

    public Optional<String> activeAccountId() {
        return Optional.ofNullable(settings.current().getActiveAccountId());
    }

    /** Switches the active account. Never touches the kill switch, open orders or positions. */
    @Transactional
    public void switchTo(String accountId) {
        AppSettings current = settings.currentForUpdate();
        refusalFor(accountId).ifPresent(reason -> {
            audit.record(AuditLevel.WARN, "ACCOUNT_SWITCH_REFUSED", null, reason);
            throw new AccountSwitchRefusedException(reason);
        });
        change(current, accountId, ActiveAccountChange.SOURCE_UI);
    }

    /**
     * Decides the active account at startup. A changed ACTIVE_ACCOUNT in .env wins over the last choice in the UI.
     * Whatever is chosen must still be allowed, so turning off ALLOW_LIVE_TRADING also drops an active LIVE account.
     */
    @Transactional
    public void resolveAtStartup() {
        AppSettings current = settings.currentForUpdate();
        String fromEnv = trading.activeAccount();
        boolean envChanged = !Objects.equals(fromEnv, current.getLastSeenEnvActiveAccount());
        boolean useEnv = fromEnv != null && (envChanged || current.getActiveAccountId() == null);
        String target = useEnv ? fromEnv : current.getActiveAccountId();
        current.setLastSeenEnvActiveAccount(fromEnv);

        if (target == null) {
            log.warn("No active account: set ACTIVE_ACCOUNT in .env or choose one in the UI");
            return;
        }
        Optional<String> refusal = refusalFor(target);
        if (refusal.isPresent()) {
            log.error("Starting with no active account: {}", refusal.get());
            audit.record(AuditLevel.ERROR, "ACTIVE_ACCOUNT_REFUSED", null, refusal.get());
            change(current, null, ActiveAccountChange.SOURCE_STARTUP);
            return;
        }
        change(current, target, useEnv ? ActiveAccountChange.SOURCE_ENV : ActiveAccountChange.SOURCE_STARTUP);
    }

    private Optional<String> refusalFor(String accountId) {
        Optional<BrokerAccount> account = accounts.find(accountId);
        if (account.isEmpty()) {
            return Optional.of("Unknown account " + accountId);
        }
        if (!account.get().isEnabled()) {
            return Optional.of("Account " + accountId + " is disabled: fill in its values in .env");
        }
        if (account.get().getTradingMode() == TradingMode.LIVE && !trading.allowLiveTrading()) {
            return Optional.of("Account " + accountId + " is LIVE, and ALLOW_LIVE_TRADING is not true in .env");
        }
        return Optional.empty();
    }

    private void change(AppSettings current, String to, String source) {
        String from = current.getActiveAccountId();
        if (Objects.equals(from, to)) {
            return;
        }
        current.setActiveAccountId(to);
        changes.save(new ActiveAccountChange(from, to, source));
        audit.record(AuditLevel.INFO, "ACTIVE_ACCOUNT_CHANGED", to, "Active account changed from " + from + " to " + to);
        log.info("Active account changed from {} to {} ({})", from, to, source);
    }
}
