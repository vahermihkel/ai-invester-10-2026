package com.aiinvestor.api;

import java.math.BigDecimal;

/** The API sends decimals as strings, so no precision is lost. */
final class ApiDecimals {

    private ApiDecimals() {
    }

    /** Without the database's trailing zeros: 20000, not 20000.000000000000000000. */
    static String toApi(BigDecimal value) {
        return value == null ? null : value.stripTrailingZeros().toPlainString();
    }
}
