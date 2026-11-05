package com.aiinvestor.broker;

/** Adapters map native broker errors to this. The message must never contain credentials. */
public class BrokerException extends RuntimeException {

    public BrokerException(String message) {
        super(message);
    }

    public BrokerException(String message, Throwable cause) {
        super(message, cause);
    }
}
