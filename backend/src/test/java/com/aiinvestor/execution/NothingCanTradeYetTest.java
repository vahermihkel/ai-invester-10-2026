package com.aiinvestor.execution;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

import com.aiinvestor.ai.AiRouter;
import com.aiinvestor.ai.AnalysisRequest;
import com.aiinvestor.domain.OrderSize;
import com.aiinvestor.domain.SizeType;
import com.aiinvestor.risk.RiskEngine;
import com.aiinvestor.signal.ParsedSignal;
import com.aiinvestor.signal.SignalAction;
import com.aiinvestor.signal.SignalParser;

/** Every placeholder in the pipeline must refuse. Replace each test when the real code is built. */
class NothingCanTradeYetTest {

    private final ParsedSignal buy = new ParsedSignal("AAPL", SignalAction.BUY,
            new OrderSize(SizeType.UNITS, BigDecimal.ONE), BigDecimal.ONE, "test");

    @Test
    void routerGivesNoResponse() {
        assertThat(new AiRouter().route(new AnalysisRequest("prompt"))).isEmpty();
    }

    @Test
    void parserAlwaysHolds() {
        String validLookingBuy = "{\"symbol\":\"AAPL\",\"action\":\"BUY\",\"confidence\":1}";

        ParsedSignal parsed = new SignalParser().parse(validLookingBuy, "AAPL");

        assertThat(parsed.action()).isEqualTo(SignalAction.HOLD);
        assertThat(parsed.size()).isNull();
    }

    @Test
    void riskEngineRejectsEverything() {
        assertThat(new RiskEngine().check(buy, "ibkr-paper").approved()).isFalse();
    }

    @Test
    void executionRefuses() {
        assertThatThrownBy(() -> new ExecutionService().execute(buy, 1, "ibkr-paper"))
                .isInstanceOf(IllegalStateException.class);
    }
}
