package com.aiinvestor.api;

import org.springframework.stereotype.Service;

import com.aiinvestor.settings.AppSettings;
import com.aiinvestor.settings.SettingsService;

@Service
public class StatusService {

    private final SettingsService settings;

    public StatusService(SettingsService settings) {
        this.settings = settings;
    }

    public SystemStatus currentStatus() {
        AppSettings current = settings.current();
        // No broker adapters or scheduler exist yet, so nothing can be connected, open or have run.
        return new SystemStatus(current.getActiveAccountId(), current.isTradingEnabled(), false, false, null);
    }
}
