package com.aiinvestor.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

import com.aiinvestor.domain.TradingMode;

/** A live account marked PAPER would slip past the live-trading switch, so the modes in application.yml are pinned here. */
@SpringBootTest(classes = BrokerPropertiesTest.Config.class, webEnvironment = SpringBootTest.WebEnvironment.NONE)
class BrokerPropertiesTest {

    @Configuration
    @EnableConfigurationProperties(BrokerProperties.class)
    static class Config {
    }

    @Autowired
    private BrokerProperties properties;

    @Test
    void knownAccountsHaveTheRightTradingMode() {
        Map<String, TradingMode> modes = properties.accounts().stream()
                .collect(Collectors.toMap(BrokerProperties.Account::alias, BrokerProperties.Account::tradingMode));

        assertThat(modes).containsOnly(
                Map.entry("ibkr-paper", TradingMode.PAPER),
                Map.entry("ibkr-live", TradingMode.LIVE),
                Map.entry("etoro-demo", TradingMode.PAPER),
                Map.entry("etoro-real", TradingMode.LIVE));
    }

    @Test
    void refusesAccountWithoutTradingMode() {
        new ApplicationContextRunner()
                .withUserConfiguration(Config.class)
                .withPropertyValues(
                        "app.brokers.accounts[0].alias=x",
                        "app.brokers.accounts[0].broker-type=IBKR",
                        "app.brokers.accounts[0].display-name=X",
                        "app.brokers.accounts[0].base-currency=USD")
                .run(context -> assertThat(context).hasFailed());
    }

    @Test
    void everyAccountNamesItsRequiredVariables() {
        assertThat(properties.accounts()).allSatisfy(account -> assertThat(account.requiredEnv()).isNotEmpty());
    }
}
