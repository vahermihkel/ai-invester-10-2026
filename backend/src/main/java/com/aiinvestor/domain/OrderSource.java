package com.aiinvestor.domain;

/** EXTERNAL = found at the broker, not sent by us (broker's own app, stop-out, liquidation). */
public enum OrderSource {
    AI,
    MANUAL,
    EXTERNAL
}
