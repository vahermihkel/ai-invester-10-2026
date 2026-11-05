package com.aiinvestor.domain;

/** For brokers that keep separate positions. Null on netting brokers, which net BUY/SELL by themselves. */
public enum PositionEffect {
    OPEN,
    CLOSE
}
