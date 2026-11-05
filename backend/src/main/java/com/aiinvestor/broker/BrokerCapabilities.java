package com.aiinvestor.broker;

import java.util.Set;

import com.aiinvestor.domain.OrderType;
import com.aiinvestor.domain.SizeType;

/** What an adapter supports. Core code checks this and never branches on the broker's name. */
public record BrokerCapabilities(
        Set<OrderType> orderTypes,
        Set<SizeType> sizeTypes,
        boolean fractionalUnits,
        boolean shortSelling,
        boolean leverage,
        Set<String> assetClasses,
        PositionModel positionModel) {
}
