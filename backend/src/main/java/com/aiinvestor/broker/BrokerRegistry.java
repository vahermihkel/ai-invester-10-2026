package com.aiinvestor.broker;

import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

/** Finds the adapter for a broker type. There are no adapters yet, so every lookup fails for now. */
@Component
public class BrokerRegistry {

    private final Map<String, BrokerClientFactory> factories;

    public BrokerRegistry(ObjectProvider<BrokerClientFactory> factories) {
        this.factories = factories.orderedStream()
                .collect(Collectors.toUnmodifiableMap(BrokerClientFactory::brokerType, Function.identity()));
    }

    /** Empty if this broker has no adapter yet. */
    public Optional<BrokerCapabilities> capabilitiesFor(String brokerType) {
        return Optional.ofNullable(factories.get(brokerType)).map(BrokerClientFactory::capabilities);
    }

    public BrokerClientFactory factoryFor(String brokerType) {
        BrokerClientFactory factory = factories.get(brokerType);
        if (factory == null) {
            throw new BrokerException("No adapter for broker type " + brokerType);
        }
        return factory;
    }
}
