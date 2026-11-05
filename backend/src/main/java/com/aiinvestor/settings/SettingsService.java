package com.aiinvestor.settings;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.aiinvestor.audit.AuditLevel;
import com.aiinvestor.audit.AuditService;

@Service
public class SettingsService {

    private static final Logger log = LoggerFactory.getLogger(SettingsService.class);

    private final AppSettingsRepository settings;
    private final AuditService audit;

    public SettingsService(AppSettingsRepository settings, AuditService audit) {
        this.settings = settings;
        this.audit = audit;
    }

    public AppSettings current() {
        return settings.findById(AppSettings.SINGLE_ROW_ID)
                .orElseThrow(() -> new IllegalStateException("app_settings row is missing"));
    }

    /** The locked row. Changes to it are saved when the caller's transaction commits. */
    @Transactional(propagation = Propagation.MANDATORY)
    public AppSettings currentForUpdate() {
        return settings.findForUpdate(AppSettings.SINGLE_ROW_ID)
                .orElseThrow(() -> new IllegalStateException("app_settings row is missing"));
    }

    /** The kill switch. Stored in the database, so it survives restarts and account switches. */
    @Transactional
    public void setTradingEnabled(boolean enabled) {
        currentForUpdate().setTradingEnabled(enabled);
        String event = enabled ? "KILL_SWITCH_START" : "KILL_SWITCH_STOP";
        String message = enabled ? "Automated trading resumed" : "Automated trading stopped";
        audit.record(AuditLevel.WARN, event, null, message);
        log.warn(message);
    }
}
