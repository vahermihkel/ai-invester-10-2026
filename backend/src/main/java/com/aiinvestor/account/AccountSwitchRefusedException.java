package com.aiinvestor.account;

/** The message is safe to show in the UI. */
public class AccountSwitchRefusedException extends RuntimeException {

    public AccountSwitchRefusedException(String message) {
        super(message);
    }
}
