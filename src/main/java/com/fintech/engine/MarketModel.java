package com.fintech.engine;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;

record Quote(String symbol, BigDecimal price, BigDecimal volatility, Instant timestamp) {
}

enum Side {
    BUY, SELL
}

enum StrategySignal {
    BUY, SELL, HOLD
}

record TradeOrder(String accountId, String symbol, Side side, int quantity,
        BigDecimal limitPrice, BigDecimal stopLossPrice) {
    TradeOrder {
        if (quantity <= 0)
            throw new IllegalArgumentException("Quantity must be positive");
        if (limitPrice.signum() <= 0)
            throw new IllegalArgumentException("Price must be positive");
    }

    BigDecimal notional() {
        return limitPrice.multiply(BigDecimal.valueOf(quantity)).setScale(2, RoundingMode.HALF_UP);
    }
}

record TradeResult(String orderId, TradeOrder order, BigDecimal grossValue,
        BigDecimal fee, String status, String message) {
}

final class Account {
    private final String id;
    private BigDecimal cash;
    private BigDecimal marginUsed = BigDecimal.ZERO;
    private final List<TradeResult> ledger = new ArrayList<>();

    Account(String id, BigDecimal cash) {
        this.id = id;
        this.cash = cash;
    }

    String id() {
        return id;
    }

    synchronized BigDecimal cash() {
        return cash;
    }

    synchronized BigDecimal marginUsed() {
        return marginUsed;
    }

    synchronized void reserve(BigDecimal amount) {
        cash = cash.subtract(amount);
        marginUsed = marginUsed.add(amount);
    }

    synchronized void releaseMargin(BigDecimal amount) {
        marginUsed = marginUsed.subtract(amount);
    }

    synchronized void record(TradeResult result) {
        BigDecimal margin = result.grossValue().multiply(new BigDecimal("0.25"));
        BigDecimal settlement = result.order().side() == Side.BUY
                ? result.grossValue().subtract(margin)
                : result.grossValue().add(margin);
        cash = result.order().side() == Side.BUY ? cash.subtract(settlement) : cash.add(settlement);
        ledger.add(result);
    }

    synchronized List<TradeResult> ledger() {
        return List.copyOf(ledger);
    }

    synchronized BigDecimal positionQuantity(String symbol) {
        return ledger.stream()
                .filter(result -> result.order().symbol().equals(symbol))
                .map(result -> result.order().side() == Side.BUY
                        ? BigDecimal.valueOf(result.order().quantity())
                        : BigDecimal.valueOf(-result.order().quantity()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    synchronized BigDecimal totalAssets(Quote quote) {
        return cash.add(positionQuantity(quote.symbol()).multiply(quote.price()));
    }
}

final class AccountRepository {
    private final Map<String, Account> accounts = new ConcurrentHashMap<>();

    void add(Account account) {
        accounts.put(account.id(), account);
    }

    Account get(String id) {
        Account account = accounts.get(id);
        if (account == null)
            throw new IllegalArgumentException("Unknown account: " + id);
        return account;
    }
}
