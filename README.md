# Extensible FinTech Trading & Risk Management Engine

A Java 21 reference project demonstrating seven classic design patterns in a realistic trading workflow.

For a presentation-ready explanation of the architecture, runtime flow, patterns, demo steps, and extension ideas, see [DESIGN_DOCUMENT.md](DESIGN_DOCUMENT.md).

## Patterns

| Pattern         | Implementation                                                                    | Responsibility                                                                |
| --------------- | --------------------------------------------------------------------------------- | ----------------------------------------------------------------------------- |
| Observer        | `MarketDataFeed`, `StrategyMonitor`, `PortfolioTicker`                            | Broadcasts quote changes to subscribed consumers                              |
| Strategy        | `MomentumStrategy`, `MeanReversionStrategy`, `ArbitrageStrategy`                  | Selects an order signal based on market conditions                            |
| Decorator       | `TransactionFeeDecorator`, `MarginRequirementDecorator`, `StopLossGuardDecorator` | Adds composable order checks and costs                                        |
| Facade          | `TradingDeskFacade`                                                               | Exposes one simple API for the trading desk                                   |
| Template Method | `TradeWorkflow`, `EquityTradeWorkflow`                                            | Fixes execution steps while allowing risk/ledger customization                |
| Proxy           | `SecurityProxy`                                                                   | Adds authorization, rate limiting, and simulation caching around broker calls |

## Run

```text
mvn clean test
mvn exec:java
```

The demo publishes live quotes, chooses a strategy, submits a decorated order through the facade, and prints the risk, execution, ledger, and tax results.

## Visual showcase

Start the browser dashboard with:

```text
mvn compile exec:java@dashboard "-Ddashboard.port=8081"
```

Then open `http://localhost:8081`. The dashboard lets you publish quotes through the Observer pattern and place simulated orders through the Facade. The execution panel shows the Strategy selection, Decorator chain, Template Method workflow, and Security Proxy in one view. Use `-Ddashboard.port=8090` if that port is also busy.

## Project shape

- `src/main/java/com/fintech/engine`: domain model and pattern implementations
- `src/test/java/com/fintech/engine`: focused JUnit tests for the workflow and guards
