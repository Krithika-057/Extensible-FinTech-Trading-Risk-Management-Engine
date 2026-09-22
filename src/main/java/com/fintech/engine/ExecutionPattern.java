package com.fintech.engine;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.UUID;

interface OrderExecutor { TradeResult execute(TradeOrder order); }

final class BrokerGateway implements OrderExecutor {
    public TradeResult execute(TradeOrder order) {
        return new TradeResult(UUID.randomUUID().toString(), order, order.notional(), BigDecimal.ZERO, "EXECUTED", "Broker accepted order");
    }
}

abstract class OrderDecorator implements OrderExecutor {
    protected final OrderExecutor wrapped;
    protected OrderDecorator(OrderExecutor wrapped) { this.wrapped = wrapped; }
}

final class TransactionFeeDecorator extends OrderDecorator {
    TransactionFeeDecorator(OrderExecutor wrapped) { super(wrapped); }
    public TradeResult execute(TradeOrder order) {
        TradeResult result = wrapped.execute(order);
        BigDecimal fee = result.grossValue().multiply(new BigDecimal("0.0015")).setScale(2, RoundingMode.HALF_UP);
        return new TradeResult(result.orderId(), order, result.grossValue(), fee, result.status(), result.message() + "; fee calculated");
    }
}

final class MarginRequirementDecorator extends OrderDecorator {
    private final AccountRepository accounts;
    MarginRequirementDecorator(OrderExecutor wrapped, AccountRepository accounts) { super(wrapped); this.accounts = accounts; }
    public TradeResult execute(TradeOrder order) {
        Account account = accounts.get(order.accountId());
        BigDecimal required = order.notional().multiply(new BigDecimal("0.25"));
        if (account.cash().compareTo(required) < 0) throw new IllegalStateException("Insufficient margin for order");
        account.reserve(required);
        return wrapped.execute(order);
    }
}

final class StopLossGuardDecorator extends OrderDecorator {
    StopLossGuardDecorator(OrderExecutor wrapped) { super(wrapped); }
    public TradeResult execute(TradeOrder order) {
        if (order.side() == Side.BUY && order.stopLossPrice().compareTo(order.limitPrice()) >= 0) {
            throw new IllegalArgumentException("Buy stop-loss must be below limit price");
        }
        return wrapped.execute(order);
    }
}
