package com.aiinvestor.domain;

/** We only place MARKET and LIMIT. OTHER describes orders found at the broker (stop, …). */
public enum OrderType {
    MARKET,
    LIMIT,
    OTHER
}
