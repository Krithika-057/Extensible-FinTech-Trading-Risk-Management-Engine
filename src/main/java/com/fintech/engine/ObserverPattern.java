package com.fintech.engine;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

interface MarketObserver {
    void onQuote(Quote quote);

    default String name() {
        return getClass().getSimpleName();
    }
}

final class MarketDataFeed {
    private final List<MarketObserver> observers = new CopyOnWriteArrayList<>();
    private Quote latest;

    void subscribe(MarketObserver observer) {
        if (!observers.contains(observer))
            observers.add(observer);
    }

    void unsubscribe(MarketObserver observer) {
        observers.remove(observer);
    }

    Quote latest() {
        return latest;
    }

    boolean isSubscribed(MarketObserver observer) {
        return observers.contains(observer);
    }

    List<String> observerNames() {
        return observers.stream().map(MarketObserver::name).toList();
    }

    void publish(String symbol, BigDecimal price, BigDecimal volatility) {
        latest = new Quote(symbol, price, volatility, Instant.now());
        observers.forEach(observer -> observer.onQuote(latest));
    }
}

final class StrategyMonitor implements MarketObserver {
    private Quote lastQuote;

    @Override
    public void onQuote(Quote quote) {
        lastQuote = quote;
    }

    Quote lastQuote() {
        return lastQuote;
    }
}

final class PortfolioTicker implements MarketObserver {
    private int updateCount;

    @Override
    public void onQuote(Quote quote) {
        updateCount++;
    }

    int updateCount() {
        return updateCount;
    }
}
