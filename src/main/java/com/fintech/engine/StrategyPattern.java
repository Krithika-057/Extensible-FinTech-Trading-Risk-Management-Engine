package com.fintech.engine;

import java.math.BigDecimal;

interface TradingStrategy {
    String name();
    StrategySignal evaluate(Quote quote);
}

final class MomentumStrategy implements TradingStrategy {
    public String name() { return "Momentum"; }
    public StrategySignal evaluate(Quote quote) { return quote.volatility().compareTo(new BigDecimal("0.05")) > 0 ? StrategySignal.BUY : StrategySignal.HOLD; }
}

final class MeanReversionStrategy implements TradingStrategy {
    public String name() { return "Mean Reversion"; }
    public StrategySignal evaluate(Quote quote) { return quote.volatility().compareTo(new BigDecimal("0.02")) < 0 ? StrategySignal.SELL : StrategySignal.HOLD; }
}

final class ArbitrageStrategy implements TradingStrategy {
    public String name() { return "Arbitrage"; }
    public StrategySignal evaluate(Quote quote) { return quote.price().remainder(BigDecimal.TEN).compareTo(new BigDecimal("5")) < 0 ? StrategySignal.BUY : StrategySignal.HOLD; }
}

final class StrategySelector {
    TradingStrategy select(BigDecimal volatility) {
        if (volatility.compareTo(new BigDecimal("0.05")) >= 0) return new MomentumStrategy();
        if (volatility.compareTo(new BigDecimal("0.02")) <= 0) return new MeanReversionStrategy();
        return new ArbitrageStrategy();
    }
}
