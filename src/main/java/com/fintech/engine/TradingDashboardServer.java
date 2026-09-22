package com.fintech.engine;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;

public final class TradingDashboardServer {
    private final AccountRepository accounts = new AccountRepository();
    private final MarketDataFeed feed = new MarketDataFeed();
    private final StrategyMonitor strategyMonitor = new StrategyMonitor();
    private final PortfolioTicker portfolioTicker = new PortfolioTicker();
    private final List<String> eventLog = new ArrayList<>();
    private final TradingDeskFacade desk;

    private TradingDashboardServer() {
        accounts.add(new Account("ACC-1001", new BigDecimal("50000")));
        feed.subscribe(strategyMonitor);
        feed.subscribe(portfolioTicker);
        log("Observer: subscribed StrategyMonitor");
        log("Observer: subscribed PortfolioTicker");
        feed.publish("ACME", new BigDecimal("102.50"), new BigDecimal("0.08"));

        OrderExecutor execution = new StopLossGuardDecorator(
                new MarginRequirementDecorator(
                        new TransactionFeeDecorator(
                                new SecurityProxy(new BrokerGateway(), true, true)),
                        accounts));
        desk = new TradingDeskFacade(accounts, feed, new EquityTradeWorkflow(accounts, execution));
    }

    public static void main(String[] args) throws IOException {
        TradingDashboardServer dashboard = new TradingDashboardServer();
        int port = Integer.getInteger("dashboard.port", 8081);
        HttpServer server = HttpServer.create(new InetSocketAddress("localhost", port), 0);
        server.createContext("/", dashboard::handlePage);
        server.createContext("/api/state", dashboard::handleState);
        server.createContext("/api/quote", dashboard::handleQuote);
        server.createContext("/api/trade", dashboard::handleTrade);
        server.createContext("/api/auto-trade", dashboard::handleAutoTrade);
        server.createContext("/api/observers", dashboard::handleObservers);
        server.createContext("/api/trace", dashboard::handleTrace);
        server.setExecutor(Executors.newVirtualThreadPerTaskExecutor());
        server.start();
        System.out.println("Trading dashboard running at http://localhost:" + port);
    }

    private void handlePage(HttpExchange exchange) throws IOException {
        if (!"GET".equals(exchange.getRequestMethod()) || !"/".equals(exchange.getRequestURI().getPath())) {
            send(exchange, 404, "text/plain", "Not found");
            return;
        }
        send(exchange, 200, "text/html; charset=utf-8", DashboardPage.HTML);
    }

    private synchronized void handleState(HttpExchange exchange) throws IOException {
        if (!"GET".equals(exchange.getRequestMethod())) {
            send(exchange, 405, "application/json", "{\"error\":\"Method not allowed\"}");
            return;
        }
        Quote quote = strategyMonitor.lastQuote();
        TradingStrategy strategy = new StrategySelector().select(quote.volatility());
        Account account = accounts.get("ACC-1001");
        String json = "{\"symbol\":\"" + quote.symbol() + "\",\"price\":" + quote.price()
                + ",\"volatility\":" + quote.volatility() + ",\"strategy\":\"" + strategy.name()
                + "\",\"signal\":\"" + strategy.evaluate(quote) + "\",\"observers\":" + feed.observerNames().size()
                + ",\"cash\":" + account.cash() + ",\"assets\":" + account.totalAssets(quote)
                + ",\"margin\":" + account.marginUsed()
                + ",\"trades\":" + account.ledger().size() + ",\"observerNames\":"
                + observerNamesJson() + "}";
        send(exchange, 200, "application/json", json);
    }

    private synchronized void handleQuote(HttpExchange exchange) throws IOException {
        if (!"POST".equals(exchange.getRequestMethod())) {
            send(exchange, 405, "application/json", "{\"error\":\"Method not allowed\"}");
            return;
        }
        Map<String, String> values = queryValues(exchange.getRequestURI());
        try {
            String symbol = values.getOrDefault("symbol", "ACME");
            BigDecimal price = new BigDecimal(values.getOrDefault("price", "102.50"));
            BigDecimal volatility = new BigDecimal(values.getOrDefault("volatility", "0.08"));
            feed.publish(symbol, price, volatility);
            TradingStrategy strategy = new StrategySelector().select(volatility);
            log("Observer: MarketDataFeed published " + symbol + " quote to " + feed.observerNames());
            log("Strategy: " + strategy.name() + " evaluated volatility " + volatility + " -> "
                    + strategy.evaluate(new Quote(symbol, price, volatility, java.time.Instant.now())));
            send(exchange, 200, "application/json", "{\"message\":\"Quote published\"}");
        } catch (RuntimeException exception) {
            send(exchange, 400, "application/json", jsonError(exception.getMessage()));
        }
    }

    private synchronized void handleTrade(HttpExchange exchange) throws IOException {
        if (!"POST".equals(exchange.getRequestMethod())) {
            send(exchange, 405, "application/json", "{\"error\":\"Method not allowed\"}");
            return;
        }
        Map<String, String> values = queryValues(exchange.getRequestURI());
        try {
            TradeResult result = desk.placeOrder("ACC-1001", values.getOrDefault("symbol", "ACME"),
                    Side.valueOf(values.getOrDefault("side", "BUY")),
                    Integer.parseInt(values.getOrDefault("quantity", "100")),
                    new BigDecimal(values.getOrDefault("price", "102.50")),
                    new BigDecimal(values.getOrDefault("stopLoss", "96")));
            String json = "{\"status\":\"" + result.status() + "\",\"gross\":" + result.grossValue()
                    + ",\"fee\":" + result.fee() + ",\"tax\":" + desk.estimateTax(result) + "}";
            send(exchange, 200, "application/json", json);
        } catch (RuntimeException exception) {
            send(exchange, 400, "application/json", jsonError(exception.getMessage()));
        }
    }

    private synchronized void handleAutoTrade(HttpExchange exchange) throws IOException {
        if (!"POST".equals(exchange.getRequestMethod())) {
            send(exchange, 405, "application/json", "{\"error\":\"Method not allowed\"}");
            return;
        }
        Map<String, String> values = queryValues(exchange.getRequestURI());
        try {
            String symbol = values.getOrDefault("symbol", "ACME");
            String strategyName = values.getOrDefault("strategy", "Momentum");
            Quote quote = feed.latest();
            TradingStrategy strategy = strategyFor(strategyName);
            StrategySignal signal = strategy.evaluate(quote);
            log("Facade: executeAutoTrade(" + symbol + ", " + strategyName + ") received");
            log("Strategy: " + strategy.name() + " forwarded " + signal + " to Facade");
            if (signal == StrategySignal.HOLD) {
                log("Execution stopped: strategy returned HOLD");
            } else {
                log("Decorator: fee, margin, and stop-loss checks queued");
                log("Template Method: validateAccountBalance -> calculateRisk -> executeOrder -> postTradeLedger");
                log("Proxy: permission and rate-limit checks forwarded to BrokerGateway");
            }
            TradeResult result = desk.executeAutoTrade(symbol, strategyName);
            log("Ledger: recorded " + result.status() + " order " + result.orderId());
            send(exchange, 200, "application/json",
                    "{\"status\":\"" + result.status() + "\",\"gross\":" + result.grossValue()
                            + ",\"fee\":" + result.fee() + ",\"tax\":" + desk.estimateTax(result) + "}");
        } catch (RuntimeException exception) {
            send(exchange, 400, "application/json", jsonError(exception.getMessage()));
        }
    }

    private synchronized void handleObservers(HttpExchange exchange) throws IOException {
        if (!"POST".equals(exchange.getRequestMethod())) {
            send(exchange, 405, "application/json", "{\"error\":\"Method not allowed\"}");
            return;
        }
        Map<String, String> values = queryValues(exchange.getRequestURI());
        String name = values.getOrDefault("name", "StrategyMonitor");
        MarketObserver observer = "PortfolioTicker".equals(name) ? portfolioTicker : strategyMonitor;
        boolean subscribe = !"unsubscribe".equalsIgnoreCase(values.getOrDefault("action", "subscribe"));
        if (subscribe) {
            feed.subscribe(observer);
            log("Observer: subscribed " + name);
        } else {
            feed.unsubscribe(observer);
            log("Observer: unsubscribed " + name);
        }
        send(exchange, 200, "application/json", "{\"observers\":" + observerNamesJson() + "}");
    }

    private synchronized void handleTrace(HttpExchange exchange) throws IOException {
        StringBuilder json = new StringBuilder("{\"events\":[");
        for (int index = 0; index < eventLog.size(); index++) {
            if (index > 0)
                json.append(',');
            json.append('"').append(escape(eventLog.get(index))).append('"');
        }
        json.append("]}");
        send(exchange, 200, "application/json", json.toString());
    }

    private TradingStrategy strategyFor(String strategyType) {
        return switch (strategyType.toLowerCase()) {
            case "momentum" -> new MomentumStrategy();
            case "mean reversion" -> new MeanReversionStrategy();
            case "arbitrage" -> new ArbitrageStrategy();
            default -> throw new IllegalArgumentException("Unknown strategy: " + strategyType);
        };
    }

    private void log(String message) {
        eventLog.add(message);
        if (eventLog.size() > 30)
            eventLog.remove(0);
    }

    private String observerNamesJson() {
        if (feed.observerNames().isEmpty())
            return "[]";
        return "[\"" + String.join("\",\"", feed.observerNames()) + "\"]";
    }

    private static Map<String, String> queryValues(URI uri) {
        Map<String, String> values = new HashMap<>();
        String query = uri.getRawQuery();
        if (query == null)
            return values;
        for (String part : query.split("&")) {
            String[] pair = part.split("=", 2);
            if (pair.length == 2)
                values.put(URLDecoder.decode(pair[0], StandardCharsets.UTF_8),
                        URLDecoder.decode(pair[1], StandardCharsets.UTF_8));
        }
        return values;
    }

    private static String jsonError(String message) {
        return "{\"error\":\"" + escape(message) + "\"}";
    }

    private static String escape(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private static void send(HttpExchange exchange, int status, String contentType, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", contentType);
        exchange.getResponseHeaders().set("Cache-Control", "no-store");
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream output = exchange.getResponseBody()) {
            output.write(bytes);
        }
    }
}
