package com.aiinvestor.ai;

/** The same prompt goes to every provider. It never says which broker is used. */
public record AnalysisRequest(String prompt) {
}
