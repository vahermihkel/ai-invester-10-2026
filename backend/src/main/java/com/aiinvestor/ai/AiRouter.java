package com.aiinvestor.ai;

import java.util.Optional;

import org.springframework.stereotype.Service;

/** Picks the provider for each request and fails over: see docs/rules/ai-providers.md. */
@Service
public class AiRouter {

    /** Empty = no provider gave a valid response, so the run must end in HOLD. Always empty until providers exist. */
    public Optional<AiResponse> route(AnalysisRequest request) {
        return Optional.empty();
    }
}
