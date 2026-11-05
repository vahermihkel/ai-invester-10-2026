package com.aiinvestor.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.aiinvestor.api.StatusController;
import com.aiinvestor.api.StatusService;
import com.aiinvestor.api.SystemStatus;

/** The API has no login, so these rules are all that keeps it private: see docs/rules/configuration.md. */
@WebMvcTest(StatusController.class)
class LocalOnlyAccessTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private StatusService statusService;

    @Value("${server.address}")
    private String serverAddress;

    @Test
    void bindsToLoopbackOnly() {
        assertThat(serverAddress).isEqualTo("127.0.0.1");
    }

    @Test
    void allowsLocalFrontend() throws Exception {
        when(statusService.currentStatus()).thenReturn(new SystemStatus(null, false, false, false, null));

        mvc.perform(get("/api/status").header("Origin", "http://localhost:5173"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"));
    }

    @Test
    void rejectsOtherOrigins() throws Exception {
        mvc.perform(get("/api/status").header("Origin", "https://evil.example"))
                .andExpect(status().isForbidden());
    }
}
