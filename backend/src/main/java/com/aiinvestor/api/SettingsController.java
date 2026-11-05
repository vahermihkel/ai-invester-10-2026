package com.aiinvestor.api;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/settings")
public class SettingsController {

    private final RiskSettingsService riskSettings;

    public SettingsController(RiskSettingsService riskSettings) {
        this.riskSettings = riskSettings;
    }

    @GetMapping
    public RiskSettingsDto settings() {
        return riskSettings.current();
    }

    @PutMapping
    public RiskSettingsDto saveSettings(@Valid @RequestBody RiskSettingsDto request) {
        try {
            riskSettings.save(request);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
        return riskSettings.current();
    }
}
