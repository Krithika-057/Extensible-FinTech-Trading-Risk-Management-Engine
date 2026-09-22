package com.fintech.engine;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

final class SecurityProxy implements OrderExecutor {
    private final OrderExecutor broker;
    private final boolean authorized;
    private final boolean simulation;
    private final Map<String, TradeResult> cache = new ConcurrentHashMap<>();
    private Instant lastCall;

    SecurityProxy(OrderExecutor broker, boolean authorized, boolean simulation) {
        this.broker = broker;
        this.authorized = authorized;
        this.simulation = simulation;
    }

    public synchronized TradeResult execute(TradeOrder order) {
        if (!authorized)
            throw new SecurityException("User is not permitted to trade");
        String key = order.accountId() + ":" + order.symbol() + ":" + order.side() + ":" + order.quantity() + ":"
                + order.limitPrice();
        if (simulation && cache.containsKey(key))
            return cache.get(key);
        Instant now = Instant.now();
        if (lastCall != null && Duration.between(lastCall, now).toMillis() < 100)
            throw new IllegalStateException("Broker rate limit exceeded");
        lastCall = now;
        TradeResult result = broker.execute(order);
        if (simulation)
            cache.put(key, result);
        return result;
    }
}
