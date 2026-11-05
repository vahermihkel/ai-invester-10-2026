package com.aiinvestor.risk;

/** rejectionReason is stored with the signal. */
public record RiskDecision(boolean approved, String rejectionReason) {

    public static RiskDecision approve() {
        return new RiskDecision(true, null);
    }

    public static RiskDecision rejected(String reason) {
        return new RiskDecision(false, reason);
    }
}
