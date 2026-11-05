package com.aiinvestor.api;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.aiinvestor.account.AccountService;
import com.aiinvestor.account.AccountSwitchRefusedException;
import com.aiinvestor.account.ActiveAccountService;
import com.aiinvestor.broker.BrokerRegistry;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

@RestController
@RequestMapping("/api")
public class AccountController {

    private static final String NEEDS_ADAPTER = "Live balance and positions need this broker's adapter, which isn't built yet";

    public record SwitchRequest(@NotBlank String accountId) {
    }

    private final AccountService accounts;
    private final ActiveAccountService activeAccount;
    private final BrokerRegistry brokers;
    private final StatusService status;

    public AccountController(AccountService accounts, ActiveAccountService activeAccount,
                             BrokerRegistry brokers, StatusService status) {
        this.accounts = accounts;
        this.activeAccount = activeAccount;
        this.brokers = brokers;
        this.status = status;
    }

    @GetMapping("/accounts")
    public List<BrokerAccountDto> accounts() {
        return accounts.list().stream()
                .map(account -> BrokerAccountDto.of(account, brokers.capabilitiesFor(account.getBroker())
                        .map(BrokerAccountDto.Capabilities::of)
                        .orElse(BrokerAccountDto.Capabilities.NONE)))
                .toList();
    }

    @PutMapping("/accounts/active")
    public SystemStatus setActiveAccount(@Valid @RequestBody SwitchRequest request) {
        try {
            activeAccount.switchTo(request.accountId());
        } catch (AccountSwitchRefusedException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
        return status.currentStatus();
    }

    @GetMapping("/account")
    public void account() {
        throw NotBuiltYet.error(NEEDS_ADAPTER);
    }

    @GetMapping("/positions")
    public void positions() {
        throw NotBuiltYet.error(NEEDS_ADAPTER);
    }

    @GetMapping("/account/history")
    public void accountHistory() {
        throw NotBuiltYet.error();
    }

    @GetMapping("/cash-transactions")
    public void cashTransactions() {
        throw NotBuiltYet.error();
    }
}
