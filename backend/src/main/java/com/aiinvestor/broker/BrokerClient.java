package com.aiinvestor.broker;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import com.aiinvestor.domain.AccountSummary;
import com.aiinvestor.domain.Fill;
import com.aiinvestor.domain.Instrument;
import com.aiinvestor.domain.Order;
import com.aiinvestor.domain.OrderRequest;
import com.aiinvestor.domain.Position;

/** All core code talks to brokers through this. Native broker types stay inside the adapter's own package. */
public interface BrokerClient {

    CompletableFuture<Void> connect();

    CompletableFuture<Void> disconnect();

    boolean isConnected();

    CompletableFuture<AccountSummary> getAccountSummary();

    CompletableFuture<List<Position>> getPositions();

    CompletableFuture<List<Order>> getOpenOrders();

    CompletableFuture<Order> placeOrder(OrderRequest request);

    CompletableFuture<Void> cancelOrder(String brokerOrderId);

    /** Looks an order up by our own ID, so a retry can check whether it was already placed. */
    CompletableFuture<Order> getOrderStatus(String clientOrderId);

    CompletableFuture<List<Fill>> getFills(String brokerOrderId);

    /** Fails if the symbol can't be resolved unambiguously: that means no order. */
    CompletableFuture<Instrument> resolveInstrument(String symbol);

    CompletableFuture<Boolean> isMarketOpen(Instrument instrument);

    BrokerCapabilities capabilities();
}
