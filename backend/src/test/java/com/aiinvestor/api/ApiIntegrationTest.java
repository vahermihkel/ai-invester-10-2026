package com.aiinvestor.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

/** What the frontend relies on, on real PostgreSQL. Values are set here so a local .env can't affect the outcome. */
@SpringBootTest(properties = {
        "ACTIVE_ACCOUNT=ibkr-paper",
        "ALLOW_LIVE_TRADING=false",
        "IBKR_PAPER_HOST=127.0.0.1", "IBKR_PAPER_PORT=7497", "IBKR_PAPER_CLIENT_ID=1", "IBKR_PAPER_ACCOUNT_ID=x",
        "IBKR_LIVE_HOST=127.0.0.1", "IBKR_LIVE_PORT=7496", "IBKR_LIVE_CLIENT_ID=1", "IBKR_LIVE_ACCOUNT_ID=x",
        "ETORO_DEMO_API_KEY=x", "ETORO_DEMO_USER_KEY=x",
        "ETORO_REAL_API_KEY=", "ETORO_REAL_USER_KEY=",
})
@AutoConfigureMockMvc
@Testcontainers(disabledWithoutDocker = true)
class ApiIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:18");

    @Autowired
    private MockMvc mvc;

    @Autowired
    private JdbcTemplate jdbc;

    private void putActive(String accountId, int expectedStatus) throws Exception {
        mvc.perform(put("/api/accounts/active").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"accountId\":\"" + accountId + "\"}"))
                .andExpect(status().is(expectedStatus));
    }

    @Test
    void startupActivatesAccountFromEnv() throws Exception {
        mvc.perform(get("/api/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activeAccountId").value("ibkr-paper"))
                .andExpect(jsonPath("$.brokerConnected").value(false))
                .andExpect(jsonPath("$.marketOpen").value(false));
    }

    @Test
    void listsAccountsEnabledByTheirEnvValues() throws Exception {
        mvc.perform(get("/api/accounts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.accountId == 'ibkr-paper')].enabled").value(true))
                .andExpect(jsonPath("$[?(@.accountId == 'etoro-real')].enabled").value(false))
                .andExpect(jsonPath("$[?(@.accountId == 'ibkr-paper')].tradingMode").value("PAPER"))
                .andExpect(jsonPath("$[?(@.accountId == 'ibkr-paper')].capabilities.orderTypes").isArray());
    }

    @Test
    void switchingFollowsTheRulesAndIsRecorded() throws Exception {
        putActive("ibkr-live", 400);
        putActive("etoro-real", 400);
        putActive("no-such-account", 400);
        mvc.perform(get("/api/status")).andExpect(jsonPath("$.activeAccountId").value("ibkr-paper"));

        putActive("etoro-demo", 200);
        mvc.perform(get("/api/status")).andExpect(jsonPath("$.activeAccountId").value("etoro-demo"));
        assertThat(jdbc.queryForObject(
                "SELECT count(*) FROM active_account_changes WHERE from_account_id = 'ibkr-paper' "
                        + "AND to_account_id = 'etoro-demo' AND source = 'UI'", Integer.class)).isEqualTo(1);

        putActive("ibkr-paper", 200);
    }

    @Test
    void refusalExplainsWhy() throws Exception {
        mvc.perform(put("/api/accounts/active").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"accountId\":\"ibkr-live\"}"))
                .andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.containsString("ALLOW_LIVE_TRADING")));
    }

    @Test
    void killSwitchIsStoredAndSurvivesASwitch() throws Exception {
        mvc.perform(post("/api/trading/start")).andExpect(status().isNoContent());
        mvc.perform(get("/api/status")).andExpect(jsonPath("$.tradingEnabled").value(true));

        mvc.perform(post("/api/trading/stop")).andExpect(status().isNoContent());
        mvc.perform(get("/api/status")).andExpect(jsonPath("$.tradingEnabled").value(false));
        assertThat(jdbc.queryForObject("SELECT trading_enabled FROM app_settings", Boolean.class)).isFalse();

        putActive("etoro-demo", 200);
        mvc.perform(get("/api/status")).andExpect(jsonPath("$.tradingEnabled").value(false));
        putActive("ibkr-paper", 200);
        assertThat(jdbc.queryForObject(
                "SELECT count(*) FROM audit_log WHERE event_type = 'KILL_SWITCH_STOP'", Integer.class)).isPositive();
    }

    @Test
    void newAccountsAllowNothingUntilLimitsAreSet() throws Exception {
        mvc.perform(get("/api/settings"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accounts[?(@.accountId == 'etoro-real')].limits.maxOrderValue").value("0"))
                .andExpect(jsonPath("$.accounts[?(@.accountId == 'etoro-real')].limits.maxTradesPerDay").value(0));
    }

    private static String settingsBody(String accountId, String maxOrderValue, int interval) {
        return """
                {"analysisIntervalMinutes":%d,"accounts":[{"accountId":"%s","limits":{
                 "maxPositionValue":"20000","maxOrderValue":"%s","maxDailyLoss":"1000.50",
                 "maxTradesPerDay":10,"allowedSymbols":["AAPL","BTC"]}}]}"""
                .formatted(interval, accountId, maxOrderValue);
    }

    @Test
    void savesRiskSettingsAndRefusesBadOnes() throws Exception {
        mvc.perform(put("/api/settings").contentType(MediaType.APPLICATION_JSON)
                        .content(settingsBody("ibkr-paper", "5000", 20)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.analysisIntervalMinutes").value(20))
                .andExpect(jsonPath("$.accounts[?(@.accountId == 'ibkr-paper')].limits.maxOrderValue").value("5000"))
                .andExpect(jsonPath("$.accounts[?(@.accountId == 'ibkr-paper')].limits.maxDailyLoss").value("1000.5"))
                .andExpect(jsonPath("$.accounts[?(@.accountId == 'ibkr-paper')].limits.allowedSymbols[1]").value("BTC"));

        for (String bad : new String[] {
                settingsBody("ibkr-paper", "-1", 20),
                settingsBody("ibkr-paper", "abc", 20),
                settingsBody("ibkr-paper", "1", 0),
                settingsBody("no-such-account", "1", 25)}) {
            mvc.perform(put("/api/settings").contentType(MediaType.APPLICATION_JSON).content(bad))
                    .andExpect(status().isBadRequest());
        }
        // The refused requests changed nothing, not even the interval that came before the unknown account.
        mvc.perform(get("/api/settings")).andExpect(jsonPath("$.analysisIntervalMinutes").value(20));
    }

    @Test
    void returnsStoredSignalsAndOrdersInTheContractShape() throws Exception {
        jdbc.update("INSERT INTO ai_providers (id, provider_type, model, tier) VALUES ('p1', 'ANY', 'm1', 'FREE')");
        jdbc.update("""
                INSERT INTO signals (id, account_id, ai_provider_id, ai_model, fallback_from, symbol, action, size_type,
                                     size_value, confidence, reasoning, status)
                VALUES (900, 'ibkr-paper', 'p1', 'm1', '[{"provider":"p0","reason":"QUOTA_EXHAUSTED"}]', 'AAPL', 'BUY',
                        'UNITS', 0.000000012345678912, 0.75, 'why', 'PENDING')""");
        jdbc.update("""
                INSERT INTO instrument_mappings (id, broker, symbol, currency, asset_class, broker_instrument_id)
                VALUES (900, 'IBKR', 'AAPL', 'USD', 'FUTURE', '1')""");
        jdbc.update("""
                INSERT INTO orders (id, account_id, source, broker_order_id, instrument_mapping_id, symbol, asset_class,
                                    side, order_type, size_type, size_value, currency, status, filled_quantity)
                VALUES (900, 'ibkr-paper', 'EXTERNAL', 'b1', 900, 'AAPL', 'FUTURE', 'SELL', 'OTHER', 'UNITS', 2.5, 'USDT',
                        'UNKNOWN', 2.5)""");

        mvc.perform(get("/api/signals").param("accountId", "ibkr-paper"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(900))
                .andExpect(jsonPath("$[0].broker").value("IBKR"))
                .andExpect(jsonPath("$[0].size.type").value("UNITS"))
                .andExpect(jsonPath("$[0].size.value").value("0.000000012345678912"))
                .andExpect(jsonPath("$[0].confidence").value(0.75))
                .andExpect(jsonPath("$[0].aiProvider").value("p1"))
                .andExpect(jsonPath("$[0].fallbackFrom[0].reason").value("QUOTA_EXHAUSTED"))
                .andExpect(jsonPath("$[0].createdAt").value(org.hamcrest.Matchers.endsWith("Z")));

        mvc.perform(get("/api/orders").param("accountId", "ibkr-paper"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].source").value("EXTERNAL"))
                .andExpect(jsonPath("$[0].clientOrderId").isEmpty())
                .andExpect(jsonPath("$[0].orderType").value("OTHER"))
                .andExpect(jsonPath("$[0].status").value("UNKNOWN"))
                .andExpect(jsonPath("$[0].assetClass").value("FUTURE"))
                .andExpect(jsonPath("$[0].size.value").value("2.5"))
                .andExpect(jsonPath("$[0].commission").value("0"))
                .andExpect(jsonPath("$[0].currency").value("USDT"));

        mvc.perform(get("/api/signals").param("accountId", "no-such-account")).andExpect(status().isNotFound());
        mvc.perform(get("/api/orders").param("accountId", "etoro-demo"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }
}
