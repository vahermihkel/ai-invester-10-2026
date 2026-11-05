package com.aiinvestor.api;

import java.util.List;
import java.util.Set;

import com.aiinvestor.account.BrokerAccount;
import com.aiinvestor.broker.BrokerCapabilities;
import com.aiinvestor.domain.OrderType;
import com.aiinvestor.domain.SizeType;
import com.aiinvestor.domain.TradingMode;

/** Must match BrokerAccount in frontend/src/types.ts. */
public record BrokerAccountDto(
        String accountId,
        String broker,
        String displayName,
        TradingMode tradingMode,
        boolean enabled,
        String baseCurrency,
        Capabilities capabilities) {

    public record Capabilities(
            List<OrderType> orderTypes,
            List<SizeType> sizeTypes,
            boolean fractionalUnits,
            boolean shortSelling,
            boolean leverage,
            Set<String> assetClasses) {

        /** A broker with no adapter yet supports nothing. */
        static final Capabilities NONE = new Capabilities(List.of(), List.of(), false, false, false, Set.of());

        static Capabilities of(BrokerCapabilities c) {
            return new Capabilities(List.copyOf(c.orderTypes()), List.copyOf(c.sizeTypes()), c.fractionalUnits(),
                    c.shortSelling(), c.leverage(), c.assetClasses());
        }
    }

    static BrokerAccountDto of(BrokerAccount account, Capabilities capabilities) {
        return new BrokerAccountDto(account.getId(), account.getBroker(), account.getDisplayName(),
                account.getTradingMode(), account.isEnabled(), account.getBaseCurrency(), capabilities);
    }
}
