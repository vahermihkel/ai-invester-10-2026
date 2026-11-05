package com.aiinvestor.api;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.aiinvestor.settings.SettingsService;

/** The kill switch. */
@RestController
@RequestMapping("/api/trading")
public class TradingController {

    private final SettingsService settings;

    public TradingController(SettingsService settings) {
        this.settings = settings;
    }

    @PostMapping("/stop")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void stop() {
        settings.setTradingEnabled(false);
    }

    @PostMapping("/start")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void start() {
        settings.setTradingEnabled(true);
    }
}
