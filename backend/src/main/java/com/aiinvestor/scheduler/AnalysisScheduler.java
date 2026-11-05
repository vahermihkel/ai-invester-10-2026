package com.aiinvestor.scheduler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.aiinvestor.account.ActiveAccountService;
import com.aiinvestor.settings.AppSettings;
import com.aiinvestor.settings.SettingsService;

/**
 * Runs analysis cycles. Deliberately not @Scheduled yet: nothing runs by itself until the pipeline exists.
 * Steps once built: analysis, risk check, execution.
 */
@Component
public class AnalysisScheduler {

    private static final Logger log = LoggerFactory.getLogger(AnalysisScheduler.class);

    private final ActiveAccountService activeAccount;
    private final SettingsService settings;

    public AnalysisScheduler(ActiveAccountService activeAccount, SettingsService settings) {
        this.activeAccount = activeAccount;
        this.settings = settings;
    }

    public void runOnce() {
        AppSettings current = settings.current();
        if (!current.isTradingEnabled()) {
            log.info("Analysis run skipped: kill switch is on");
            return;
        }
        // Read once per run, so a switch mid-run can't split a run across brokers.
        activeAccount.activeAccountId().ifPresentOrElse(
                accountId -> log.info("Analysis run skipped for {}: pipeline is not built yet", accountId),
                () -> log.info("Analysis run skipped: no active account"));
    }
}
