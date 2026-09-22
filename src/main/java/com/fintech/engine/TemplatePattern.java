package com.fintech.engine;

import java.math.BigDecimal;

abstract class TradeWorkflow {
    public final TradeResult process(TradeOrder order) {
        Account account = validateAccountBalance(order);
        BigDecimal risk = calculateRisk(order);
        if (risk.compareTo(new BigDecimal("0.75")) > 0) throw new IllegalStateException("Risk score exceeds account policy");
        TradeResult result = executeOrder(order);
        postTradeLedger(account, result);
        return result;
    }

    protected abstract Account validateAccountBalance(TradeOrder order);
    protected abstract BigDecimal calculateRisk(TradeOrder order);
    protected abstract TradeResult executeOrder(TradeOrder order);
    protected abstract void postTradeLedger(Account account, TradeResult result);
}

final class EquityTradeWorkflow extends TradeWorkflow {
    private final AccountRepository accounts;
    private final OrderExecutor executor;
    EquityTradeWorkflow(AccountRepository accounts, OrderExecutor executor) { this.accounts = accounts; this.executor = executor; }
    protected Account validateAccountBalance(TradeOrder order) {
        Account account = accounts.get(order.accountId());
        if (account.cash().signum() <= 0) throw new IllegalStateException("Account has no available cash");
        return account;
    }
    protected BigDecimal calculateRisk(TradeOrder order) { return order.notional().divide(new BigDecimal("100000"), 4, java.math.RoundingMode.HALF_UP); }
    protected TradeResult executeOrder(TradeOrder order) { return executor.execute(order); }
    protected void postTradeLedger(Account account, TradeResult result) { account.record(result); account.releaseMargin(result.grossValue().multiply(new BigDecimal("0.25"))); }
}
