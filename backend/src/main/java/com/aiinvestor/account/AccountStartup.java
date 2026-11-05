package com.aiinvestor.account;

import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.stereotype.Component;

/** Runs before the web server accepts requests, so the API never sees accounts in an undecided state. */
@Component
public class AccountStartup implements SmartInitializingSingleton {

    private final AccountService accounts;
    private final ActiveAccountService activeAccount;

    public AccountStartup(AccountService accounts, ActiveAccountService activeAccount) {
        this.accounts = accounts;
        this.activeAccount = activeAccount;
    }

    @Override
    public void afterSingletonsInstantiated() {
        accounts.syncFromConfig();
        activeAccount.resolveAtStartup();
    }
}
