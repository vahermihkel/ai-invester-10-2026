package com.aiinvestor.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpMethod;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.aiinvestor.account.AccountService;
import com.aiinvestor.account.ActiveAccountService;
import com.aiinvestor.broker.BrokerRegistry;
import com.aiinvestor.execution.OrderRecordRepository;

/** An endpoint that isn't built must say so, not return made-up data. */
@WebMvcTest({AccountController.class, OrderController.class, AiProviderController.class})
class UnbuiltEndpointsTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private AccountService accounts;
    @MockitoBean
    private ActiveAccountService activeAccount;
    @MockitoBean
    private BrokerRegistry brokers;
    @MockitoBean
    private StatusService status;
    @MockitoBean
    private OrderRecordRepository orders;
    @MockitoBean
    private AccountParams accountParams;

    @ParameterizedTest
    @CsvSource({
            "GET, /api/account",
            "GET, /api/positions",
            "GET, /api/account/history",
            "GET, /api/cash-transactions",
            "GET, /api/orders/1/events",
            "GET, /api/orders/1/fills",
            "GET, /api/trades",
            "GET, /api/ai/providers",
            "PUT, /api/ai/providers",
    })
    void returnsNotImplemented(String method, String path) throws Exception {
        mvc.perform(request(HttpMethod.valueOf(method), path)).andExpect(status().isNotImplemented());
    }

    @Test
    void explainsWhyInAFormTheUiCanShow() throws Exception {
        mvc.perform(request(HttpMethod.GET, "/api/account"))
                .andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.containsString("adapter")));
    }
}
