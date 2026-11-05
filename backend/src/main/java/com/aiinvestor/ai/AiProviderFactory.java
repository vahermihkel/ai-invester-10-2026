package com.aiinvestor.ai;

import com.aiinvestor.config.AiProperties;

/** Each adapter provides one of these as a Spring bean. Adding a provider needs no change outside its adapter. */
public interface AiProviderFactory {

    /** The provider type this adapter handles, e.g. OPENAI. */
    String type();

    AiProvider create(AiProperties.Provider provider);
}
