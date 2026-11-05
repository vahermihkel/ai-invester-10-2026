package com.aiinvestor.broker;

import com.aiinvestor.config.BrokerProperties;

/** Each adapter provides one of these as a Spring bean. Adding a broker needs no change outside its adapter. */
public interface BrokerClientFactory {

    /** The account's broker-type value this adapter handles, e.g. IBKR. */
    String brokerType();

    /** Known without connecting, so the UI can show them for accounts that aren't active. */
    BrokerCapabilities capabilities();

    BrokerClient create(BrokerProperties.Account account);
}
