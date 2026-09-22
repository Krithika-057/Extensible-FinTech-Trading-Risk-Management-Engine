package com.fintech.engine;

import java.math.BigDecimal;

public final class TradingDeskFacade {
    private final AccountRepository accounts;
    private final MarketDataFeed marketData;
    private final StrategySelector strategies = new StrategySelector();
    private final TradeWorkflow workflow;

    public TradingDeskFacade(AccountRepository accounts, MarketDataFeed marketData, TradeWorkflow workflow) {
        this.accounts = accounts;
        this.marketData = marketData;
        this.workflow = workflow;
    }

    public TradeResult placeOrder(String accountId, String symbol, Side side, int quantity, BigDecimal price,
            BigDecimal stopLoss) {
        Quote quote = marketData.latest();
        if (quote == null || !quote.symbol().equals(symbol))
            throw new IllegalStateException("No live quote for " + symbol);
        TradingStrategy strategy = strategies.select(quote.volatility());
        StrategySignal signal = strategy.evaluate(quote);
        if (signal == StrategySignal.HOLD)
            throw new IllegalStateException(strategy.name() + " strategy produced HOLD");
        if (signal != (side == Side.BUY ? StrategySignal.BUY : StrategySignal.SELL))
            throw new IllegalStateException("Order conflicts with strategy signal");
        return workflow.process(new TradeOrder(accountId, symbol, side, quantity, price, stopLoss));
    }

    public TradeResult executeAutoTrade(String symbol, String strategyType) {
        Quote quote = marketData.latest();
        if (quote == null || !quote.symbol().equals(symbol))
            throw new IllegalStateException("No live quote for " + symbol);
        TradingStrategy strategy = switch (strategyType.toLowerCase()) {
            case "momentum" -> new MomentumStrategy();
            case "mean reversion" -> new MeanReversionStrategy();
            case "arbitrage" -> new ArbitrageStrategy();
            default -> throw new IllegalArgumentException("Unknown strategy: " + strategyType);
        };
        StrategySignal signal = strategy.evaluate(quote);
        if (signal == StrategySignal.HOLD)
            throw new IllegalStateException(strategy.name() + " strategy produced HOLD");
        Side side = signal == StrategySignal.BUY ? Side.BUY : Side.SELL;
        BigDecimal stopLoss = side == Side.BUY ? quote.price().multiply(new BigDecimal("0.94"))
                : quote.price().multiply(new BigDecimal("1.06"));
        return workflow.process(new TradeOrder("ACC-1001", symbol, side, 10, quote.price(), stopLoss));
    }

    public BigDecimal estimateTax(TradeResult result) {
        return result.grossValue().multiply(new BigDecimal("0.15"));
    }
}
