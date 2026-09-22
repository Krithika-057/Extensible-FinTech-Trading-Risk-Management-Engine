# Extensible FinTech Trading and Risk Management Engine

## 1. Project Overview

This project is a Java 21 trading engine that demonstrates how software design patterns can be combined to build an extensible and risk-aware order execution system.

The system receives market quotes, evaluates trading strategies, validates risk conditions, executes orders through a protected broker connection, and records completed trades in an account ledger.

The main client entry point is:

```java
facade.executeAutoTrade("ACME", "Momentum");
```

The client does not need to know how market data, strategies, risk checks, broker access, or ledger updates are implemented internally.

## 2. Class Structure

```text
                         +----------------------+
                         |   TradingDeskFacade  |
                         |   Client Entry Point |
                         +----------+-----------+
                                    |
             +----------------------+----------------------+
             |                      |                      |
             v                      v                      v
      +-------------+       +---------------+       +----------------+
      | MarketData  |       | TradeWorkflow |       | SecurityProxy  |
      | Feed        |       | Template      |       | Proxy          |
      | Subject     |       | Method        |       |                |
      +------+------+       +-------+-------+       +--------+-------+
             |                      |                        |
             | notifies             | executes               | protects
             v                      v                        v
      +-------------+       +---------------+       +----------------+
      | Observers   |       | Decorators    |       | BrokerGateway  |
      | Strategy    |       | Fee/Margin/   |       | Real Broker    |
      | Monitor     |       | Stop Loss     |       | abstraction    |
      +------+------+       +-------+-------+       +----------------+
             |                      |
             | evaluates            | wraps
             v                      v
      +-------------+       +---------------+
      | Strategies  |       | TradeOrder    |
      | Momentum    |       | Decorated     |
      | Mean Return |       | Execution     |
      | Arbitrage   |       +---------------+
      +-------------+
```

## 3. Design Patterns Used

### 3.1 Observer Pattern

**Purpose:** Notify multiple objects whenever a market quote changes.

**Classes:**

- `MarketDataFeed` is the Subject.
- `MarketObserver` is the Observer interface.
- `StrategyMonitor` and `PortfolioTicker` are concrete observers.

When a quote is published:

```java
feed.publish("ACME", price, volatility);
```

The feed sends the quote to every subscribed observer. This keeps the market data source independent from the components that consume the data.

**Benefit:** New monitors, alerts, or analytics components can subscribe without modifying `MarketDataFeed`.

### 3.2 Strategy Pattern

**Purpose:** Encapsulate interchangeable trading algorithms.

**Classes:**

- `TradingStrategy` is the common strategy interface.
- `MomentumStrategy`
- `MeanReversionStrategy`
- `ArbitrageStrategy`
- `StrategySelector`

Each strategy evaluates a `Quote` and returns a `StrategySignal`:

```text
BUY, SELL, or HOLD
```

The strategy can be selected dynamically based on volatility or explicitly through:

```java
facade.executeAutoTrade("ACME", "Momentum");
```

**Benefit:** New algorithms can be added without changing the order execution workflow.

### 3.3 Decorator Pattern

**Purpose:** Add order checks and costs dynamically without changing the core broker executor.

**Classes:**

- `OrderExecutor` is the common component interface.
- `BrokerGateway` is the base executor.
- `TransactionFeeDecorator`
- `MarginRequirementDecorator`
- `StopLossGuardDecorator`

The execution chain is built like this:

```text
StopLossGuardDecorator
    -> MarginRequirementDecorator
        -> TransactionFeeDecorator
            -> SecurityProxy
                -> BrokerGateway
```

Each decorator performs its responsibility and forwards the order to the wrapped component.

**Benefit:** Compliance and risk rules can be combined, reordered, or extended without creating many subclasses.

### 3.4 Template Method Pattern

**Purpose:** Enforce the fixed order execution sequence.

**Classes:**

- `TradeWorkflow` defines the algorithm skeleton.
- `EquityTradeWorkflow` provides the concrete rules.

The template method always runs these steps in order:

```text
validateAccountBalance()
        -> calculateRisk()
        -> executeOrder()
        -> postTradeLedger()
```

Subclasses can customize individual steps while the overall sequence remains protected.

**Benefit:** Every trade follows the same safety process, reducing the chance that a future workflow skips a risk or accounting step.

A future `CryptoTradeWorkflow` could use different balance and risk rules while keeping the same sequence.

### 3.5 Proxy Pattern

**Purpose:** Protect access to the broker service.

**Classes:**

- `OrderExecutor` is the common interface.
- `BrokerGateway` represents the real broker connection.
- `SecurityProxy` controls access to the broker.

The proxy currently demonstrates:

- Permission checks
- Broker rate limiting
- Simulation caching

The client and workflow call the proxy through the same `OrderExecutor` interface, so they do not need to know whether they are using the real broker or the protected proxy.

**Benefit:** Security and operational controls remain outside the broker implementation.

### 3.6 Facade Pattern

**Purpose:** Provide one simple API over the complete trading subsystem.

**Class:** `TradingDeskFacade`

The facade coordinates:

- Live quote lookup
- Strategy selection
- Buy or sell signal validation
- Stop-loss creation
- Trade workflow execution
- Tax estimation

The client only calls:

```java
TradeResult result = facade.executeAutoTrade("ACME", "Momentum");
```

**Benefit:** The client is decoupled from the many internal classes required to execute a trade.

## 4. Complete Runtime Flow

Example call:

```java
facade.executeAutoTrade("ACME", "Momentum");
```

The runtime flow is:

1. `TradingDeskFacade` receives the request.
2. The facade reads the latest `Quote` from `MarketDataFeed`.
3. The selected `MomentumStrategy` evaluates the quote.
4. The strategy returns a `BUY`, `SELL`, or `HOLD` signal.
5. A `HOLD` signal stops the trade.
6. For a valid signal, the facade creates a `TradeOrder` and stop-loss value.
7. `TradeWorkflow` validates the account balance.
8. `TradeWorkflow` calculates the order risk.
9. `StopLossGuardDecorator` checks the stop-loss rule.
10. `MarginRequirementDecorator` checks and reserves margin.
11. `TransactionFeeDecorator` calculates the transaction fee.
12. `SecurityProxy` checks permission and rate limits.
13. `BrokerGateway` executes the order.
14. `postTradeLedger()` records the result in the account ledger.
15. The facade returns a `TradeResult` to the caller.

## 5. Browser Demonstration

Start the dashboard:

```text
mvn compile exec:java@dashboard "-Ddashboard.port=8081"
```

Open:

```text
http://localhost:8081
```

To demonstrate the patterns:

1. Select `Momentum` from the strategy list.
2. Click `Execute trade`.
3. Observe the execution result.
4. Read the `Runtime trace` displayed below the button.
5. Observe the ledger trade count increase.

The runtime trace shows:

```text
Facade receives the request
Observer provides the market tick
Strategy produces the trading signal
Decorators apply fees and risk guards
Template Method runs the four workflow steps
Security Proxy authorizes the broker call
```

## 6. Console Demonstration

The original console demo can be run with:

```text
mvn exec:java
```

It prints:

- Observer notification count
- Selected quote
- Execution status
- Transaction fee
- Estimated tax
- Ledger entry count

## 7. Testing

Run the automated tests with:

```text
mvn clean test
```

The test suite verifies:

- Observer notification
- Strategy selection
- Stop-loss rejection
- Proxy authorization and simulation cache
- End-to-end facade execution

## 8. Extension Possibilities

The engine can be extended with:

- `CryptoTradeWorkflow` for cryptocurrency-specific risk rules
- `OptionsTradeWorkflow` for derivatives
- Additional strategies such as pairs trading or machine-learning signals
- A persistent database ledger
- Authentication and role management
- A real broker adapter behind `BrokerGateway`
- WebSocket-based live market updates

The patterns make these extensions possible without changing the client-facing facade or rewriting the complete order pipeline.

## 9. Conclusion

This project demonstrates that design patterns are most useful when they solve real responsibilities:

- Observer manages market event distribution.
- Strategy manages algorithm variation.
- Decorator manages optional order controls.
- Template Method protects workflow order.
- Proxy protects external broker access.
- Facade simplifies the client API.

Together, they create a trading engine that is easier to explain, test, secure, and extend.
