package com.aiinvestor.broker;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

class BrokerRegistryTest {

    @SuppressWarnings("unchecked")
    private static BrokerRegistry registryWith(BrokerClientFactory... factories) {
        ObjectProvider<BrokerClientFactory> provider = mock(ObjectProvider.class);
        when(provider.orderedStream()).thenReturn(Stream.of(factories));
        return new BrokerRegistry(provider);
    }

    private static BrokerClientFactory factory(String type) {
        BrokerClientFactory factory = mock(BrokerClientFactory.class);
        when(factory.brokerType()).thenReturn(type);
        return factory;
    }

    @Test
    void findsAdapterByBrokerType() {
        BrokerClientFactory someBroker = factory("SOME_NEW_BROKER");

        assertThat(registryWith(someBroker, factory("IBKR")).factoryFor("SOME_NEW_BROKER")).isSameAs(someBroker);
    }

    @Test
    void refusesUnknownBrokerType() {
        assertThatThrownBy(() -> registryWith().factoryFor("IBKR")).isInstanceOf(BrokerException.class);
    }

    @Test
    void refusesTwoAdaptersForOneBrokerType() {
        assertThatThrownBy(() -> registryWith(factory("IBKR"), factory("IBKR")))
                .isInstanceOf(IllegalStateException.class);
    }
}
