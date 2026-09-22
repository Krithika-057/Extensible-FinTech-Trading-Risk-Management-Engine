package com.fintech.engine;

import java.math.BigDecimal;

public final class TradingApplication {
    public static void main(String[] args) {
        AccountRepository accounts = new AccountRepository();
        accounts.add(new Account("ACC-1001", new BigDecimal("50000")));
        MarketDataFeed feed = new MarketDataFeed();
        StrategyMonitor strategyMonitor = new StrategyMonitor();
        PortfolioTicker portfolioTicker = new PortfolioTicker();
        feed.subscribe(strategyMonitor);
        feed.subscribe(portfolioTicker);
        feed.publish("ACME", new BigDecimal("102.50"), new BigDecimal("0.08"));

        OrderExecutor guardedExecution = new StopLossGuardDecorator(
                new MarginRequirementDecorator(
                        new TransactionFeeDecorator(
                                new SecurityProxy(new BrokerGateway(), true, true)), accounts));
        TradeWorkflow workflow = new EquityTradeWorkflow(accounts, guardedExecution);
        TradingDeskFacade desk = new TradingDeskFacade(accounts, feed, workflow);
        TradeResult result = desk.placeOrder("ACC-1001", "ACME", Side.BUY, 100, new BigDecimal("102.50"), new BigDecimal("96.00"));

        System.out.println("=== FinTech Trading Engine Demo ===");
        System.out.println("Observers notified: " + portfolioTicker.updateCount());
        System.out.println("Selected quote: " + strategyMonitor.lastQuote());
        System.out.println("Execution: " + result.status() + " | fee=" + result.fee());
        System.out.println("Estimated tax: " + desk.estimateTax(result));
        System.out.println("Ledger entries: " + accounts.get("ACC-1001").ledger().size());
    }
}
