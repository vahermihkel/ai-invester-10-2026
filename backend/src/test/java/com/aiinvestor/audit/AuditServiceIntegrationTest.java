package com.aiinvestor.audit;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@SpringBootTest
@Testcontainers(disabledWithoutDocker = true)
class AuditServiceIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:18");

    @Autowired
    private AuditService audit;

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void storesEntry() {
        audit.record(AuditLevel.WARN, "TEST_EVENT", null, "something happened");

        Map<String, Object> row = jdbc.queryForMap(
                "SELECT level, account_id, message FROM audit_log WHERE event_type = 'TEST_EVENT'");
        assertThat(row.get("level")).isEqualTo("WARN");
        assertThat(row.get("account_id")).isNull();
        assertThat(row.get("message")).isEqualTo("something happened");
    }
}
