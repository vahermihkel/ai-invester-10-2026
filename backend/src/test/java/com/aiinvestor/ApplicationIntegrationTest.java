package com.aiinvestor;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

/** Full startup on real PostgreSQL: Flyway applies the migrations, then Hibernate validates the entities against them. */
@SpringBootTest(properties = "ACTIVE_ACCOUNT=")
@AutoConfigureMockMvc
@Testcontainers(disabledWithoutDocker = true)
class ApplicationIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:18");

    @Autowired
    private MockMvc mvc;

    @Test
    void freshDatabaseStartsWithTradingStoppedAndNoActiveAccount() throws Exception {
        mvc.perform(get("/api/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activeAccountId").isEmpty())
                .andExpect(jsonPath("$.tradingEnabled").value(false))
                .andExpect(jsonPath("$.brokerConnected").value(false))
                .andExpect(jsonPath("$.marketOpen").value(false))
                .andExpect(jsonPath("$.lastRunAt").isEmpty());
    }
}
