package com.aiinvestor.ai;

/** All core code talks to AI models through this. Provider SDKs and DTOs stay inside the adapter's own package. */
public interface AiProvider {

    String id();

    /** Throws AiProviderException on a technical failure, so the router can try the next provider. */
    AiResponse analyze(AnalysisRequest request);

    AiCapabilities capabilities();

    boolean healthCheck();
}
