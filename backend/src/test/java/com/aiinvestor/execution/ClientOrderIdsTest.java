package com.aiinvestor.execution;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ClientOrderIdsTest {

    @Test
    void sameSignalAndAccountGiveTheSameId() {
        assertThat(ClientOrderIds.forSignal(103, "etoro-demo")).isEqualTo(ClientOrderIds.forSignal(103, "etoro-demo"));
    }

    @Test
    void differentSignalOrAccountGiveDifferentIds() {
        String id = ClientOrderIds.forSignal(103, "etoro-demo");

        assertThat(id).isNotEqualTo(ClientOrderIds.forSignal(104, "etoro-demo"));
        assertThat(id).isNotEqualTo(ClientOrderIds.forSignal(103, "ibkr-paper"));
    }
}
