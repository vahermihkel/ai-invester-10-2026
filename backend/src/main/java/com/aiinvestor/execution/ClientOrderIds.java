package com.aiinvestor.execution;

/** Our idempotency keys. The same signal and account always give the same ID, so a retry can't create a second order. */
public final class ClientOrderIds {

    private ClientOrderIds() {
    }

    public static String forSignal(long signalId, String accountId) {
        return "sig-" + signalId + "-" + accountId;
    }
}
