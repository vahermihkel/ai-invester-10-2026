package com.aiinvestor.broker;

public enum PositionModel {
    /** All BUY/SELL on an instrument net into one position. */
    NETTING,
    /** Each opening is its own position with its own ID, closed by that ID. */
    SEPARATE
}
