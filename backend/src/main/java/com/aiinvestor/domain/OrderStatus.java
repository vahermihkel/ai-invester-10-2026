package com.aiinvestor.domain;

public enum OrderStatus {
    CREATED,
    SUBMITTED,
    PARTIALLY_FILLED,
    FILLED,
    CANCELLED,
    REJECTED,
    /** Outcome not known (e.g. a timeout): the order may have been placed, so reconcile before doing anything else. */
    UNKNOWN
}
