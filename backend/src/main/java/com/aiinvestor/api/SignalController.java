package com.aiinvestor.api;

import java.util.List;

import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.aiinvestor.signal.SignalRepository;

@RestController
@RequestMapping("/api/signals")
public class SignalController {

    private final SignalRepository signals;
    private final AccountParams accounts;

    public SignalController(SignalRepository signals, AccountParams accounts) {
        this.signals = signals;
        this.accounts = accounts;
    }

    @GetMapping
    public List<SignalDto> signals(@RequestParam(required = false) String accountId,
                                   @RequestParam(defaultValue = "50") int limit) {
        return accounts.resolve(accountId)
                .map(account -> signals
                        .findByAccountIdOrderByCreatedAtDesc(account.getId(), PageRequest.of(0, Math.clamp(limit, 1, 500)))
                        .stream()
                        .map(signal -> SignalDto.of(signal, account.getBroker()))
                        .toList())
                .orElse(List.of());
    }
}
