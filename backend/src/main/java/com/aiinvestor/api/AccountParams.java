package com.aiinvestor.api;

import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import com.aiinvestor.account.AccountService;
import com.aiinvestor.account.ActiveAccountService;
import com.aiinvestor.account.BrokerAccount;

/** Resolves the optional accountId parameter of history endpoints. */
@Component
class AccountParams {

    private final AccountService accounts;
    private final ActiveAccountService activeAccount;

    AccountParams(AccountService accounts, ActiveAccountService activeAccount) {
        this.accounts = accounts;
        this.activeAccount = activeAccount;
    }

    /** The given account, or the active one if omitted. Empty if omitted and nothing is active. */
    Optional<BrokerAccount> resolve(String accountId) {
        if (accountId != null) {
            return Optional.of(accounts.find(accountId).orElseThrow(() ->
                    new ResponseStatusException(HttpStatus.NOT_FOUND, "Unknown account " + accountId)));
        }
        return activeAccount.activeAccountId().flatMap(accounts::find);
    }
}
