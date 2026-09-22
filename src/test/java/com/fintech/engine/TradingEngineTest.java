package com.fintech.engine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class TradingEngineTest {
    @Test
    void feedNotifiesAllObservers() {
        MarketDataFeed feed = new MarketDataFeed();
        StrategyMonitor monitor = new StrategyMonitor();
        PortfolioTicker ticker = new PortfolioTicker();
        feed.subscribe(monitor);
        feed.subscribe(ticker);

        feed.publish("ACME", new BigDecimal("102.50"), new BigDecimal("0.08"));

        assertEquals("ACME", monitor.lastQuote().symbol());
        assertEquals(1, ticker.updateCount());
    }

    @Test
    void selectorChoosesStrategyByVolatility() {
        StrategySelector selector = new StrategySelector();
        assertEquals("Momentum", selector.select(new BigDecimal("0.08")).name());
        assertEquals("Mean Reversion", selector.select(new BigDecimal("0.01")).name());
        assertEquals("Arbitrage", selector.select(new BigDecimal("0.03")).name());
    }

    @Test
    void stopLossDecoratorRejectsUnsafeBuyOrder() {
        OrderExecutor guarded = new StopLossGuardDecorator(new BrokerGateway());
        TradeOrder unsafe = new TradeOrder("A", "ACME", Side.BUY, 1, new BigDecimal("100"), new BigDecimal("100"));
        assertThrows(IllegalArgumentException.class, () -> guarded.execute(unsafe));
    }

    @Test
    void proxyCachesSimulationAndBlocksUnauthorizedUsers() {
        TradeOrder order = new TradeOrder("A", "ACME", Side.BUY, 1, new BigDecimal("100"), new BigDecimal("90"));
        SecurityProxy simulationProxy = new SecurityProxy(new BrokerGateway(), true, true);
        TradeResult first = simulationProxy.execute(order);
        TradeResult second = simulationProxy.execute(order);
        assertSame(first, second);

        SecurityProxy restrictedProxy = new SecurityProxy(new BrokerGateway(), false, false);
        assertThrows(SecurityException.class, () -> restrictedProxy.execute(order));
    }

    @Test
    void facadeRunsDecoratedTradeAndRecordsLedger() {
        AccountRepository accounts = new AccountRepository();
        accounts.add(new Account("ACC-1", new BigDecimal("10000")));
        MarketDataFeed feed = new MarketDataFeed();
        feed.publish("ACME", new BigDecimal("102.50"), new BigDecimal("0.08"));
        OrderExecutor executor = new StopLossGuardDecorator(
                new MarginRequirementDecorator(new TransactionFeeDecorator(new BrokerGateway()), accounts));
        TradingDeskFacade facade = new TradingDeskFacade(accounts, feed, new EquityTradeWorkflow(accounts, executor));

        TradeResult result = facade.placeOrder("ACC-1", "ACME", Side.BUY, 10, new BigDecimal("102.50"),
                new BigDecimal("90"));

        assertNotNull(result.orderId());
        assertEquals(new BigDecimal("1.54"), result.fee());
        assertEquals(1, accounts.get("ACC-1").ledger().size());
    }
}
