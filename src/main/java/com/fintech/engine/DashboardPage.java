package com.fintech.engine;

final class DashboardPage {
    private DashboardPage() {
    }

    static final String HTML = """
            <!doctype html>
            <html lang="en">
            <head>
              <meta charset="utf-8">
              <meta name="viewport" content="width=device-width, initial-scale=1">
              <title>Trading Pattern Demo</title>
              <style>
                * { box-sizing: border-box; }
                body { margin: 0; background: #f4f4f0; color: #202522; font: 15px Arial, sans-serif; }
                main { width: min(680px, calc(100% - 32px)); margin: 40px auto; }
                h1 { margin: 0 0 8px; font-size: 30px; }
                h2 { margin: 0 0 16px; font-size: 18px; }
                p { color: #68706b; line-height: 1.5; }
                .box { background: white; border: 1px solid #d8ddd8; border-radius: 5px; padding: 20px; margin-top: 16px; }
                .quote { display: flex; justify-content: space-between; align-items: end; border-bottom: 1px solid #e1e5e1; padding-bottom: 16px; }
                .symbol { color: #68706b; font-size: 13px; text-transform: uppercase; }
                .price { font-size: 38px; font-weight: bold; margin-top: 6px; }
                .badge { background: #e2f2e7; color: #21663a; border-radius: 4px; padding: 8px 10px; font-size: 12px; }
                .details { display: grid; grid-template-columns: repeat(3, 1fr); gap: 10px; margin-top: 16px; }
                .detail { background: #f4f4f0; padding: 12px; }
                .detail small { display: block; color: #68706b; margin-bottom: 5px; }
                label { display: block; color: #68706b; font-size: 12px; margin: 12px 0 6px; }
                select, button { width: 100%; padding: 11px; border: 1px solid #cbd2cc; border-radius: 4px; font: inherit; }
                button { background: #236b43; color: white; border: 0; cursor: pointer; margin-top: 14px; }
                button:hover { background: #185333; }
                .row { display: grid; grid-template-columns: 1fr 1fr; gap: 10px; }
                .secondary { background: #68706b; }
                .observer { display: flex; justify-content: space-between; align-items: center; padding: 9px 0; border-bottom: 1px solid #e1e5e1; }
                .observer button { width: auto; margin: 0; padding: 6px 9px; font-size: 12px; }
                #events { margin: 0; padding-left: 20px; color: #68706b; font-size: 13px; line-height: 1.7; }
                .patterns { display: grid; grid-template-columns: repeat(2, 1fr); gap: 8px; }
                .pattern { border-left: 3px solid #78b58c; background: #f4f4f0; padding: 10px 12px; }
                .pattern strong { display: block; font-size: 13px; }
                .pattern span { color: #68706b; font-size: 12px; }
                #message { min-height: 22px; margin: 14px 0 0; color: #236b43; }
                #trace { display: none; margin-top: 14px; padding: 12px; background: #f4f4f0; border-left: 3px solid #78b58c; color: #68706b; font-size: 13px; line-height: 1.8; }
                @media (max-width: 560px) { main { margin: 24px auto; } .details, .patterns { grid-template-columns: 1fr; } .quote { display: block; } .badge { display: inline-block; margin-top: 14px; } }
              </style>
            </head>
            <body>
              <main>
                <h1>Extensible FinTech Trading &amp; Risk Management Engine</h1>
                <section class="box">
                  <div class="quote"><div><div class="symbol" id="symbol">ACME</div><div class="price" id="price">$102.50</div></div><div class="badge" id="strategy">Momentum / BUY</div></div>
                  <div class="details"><div class="detail"><small>Volatility</small><span id="volatility">8%</span></div><div class="detail"><small>Observers</small><span id="observers">2</span></div><div class="detail"><small>Total assets</small><span id="assets">$50,000.00</span></div><div class="detail"><small>Ledger trades</small><span id="trades">0</span></div></div>
                </section>
                <section class="box"><h2>Company and strategy</h2><label for="company-select">Company</label><select id="company-select"><option value="ACME" data-price="102.50">ACME (demo company)</option><option value="AAPL" data-price="195.20">AAPL (Apple)</option><option value="TSLA" data-price="242.10">TSLA (Tesla)</option><option value="RELIANCE" data-price="2940.00">RELIANCE (Reliance Industries)</option></select><label for="strategy-select">Strategy</label><select id="strategy-select"><option>Momentum</option><option>Mean Reversion</option><option>Arbitrage</option></select><button id="trade-button">Execute trade</button><p id="message"></p><div id="trace"></div></section>
                <section class="box"><h2>Publish market tick</h2><div class="row"><div><label for="price-input">Price</label><input id="price-input" value="102.50"></div><div><label for="volatility-input">Volatility</label><input id="volatility-input" value="0.08"></div></div><button id="quote-button" class="secondary">Publish quote and recalculate signal</button><p id="quote-message"></p></section>
                <section class="box"><h2>Observers</h2><div class="observer"><span>StrategyMonitor</span><button data-observer="StrategyMonitor">Toggle</button></div><div class="observer"><span>PortfolioTicker</span><button data-observer="PortfolioTicker">Toggle</button></div><p id="observer-list"></p></section>
                <section class="box"><h2>Intermediate outputs</h2><ol id="events"><li>Waiting for a quote or trade...</li></ol></section>
                <section class="box"><h2>What the strategies mean</h2><div class="patterns"><div class="pattern"><strong>Momentum</strong><span>Follows strong movement. Volatility above 5% produces BUY.</span></div><div class="pattern"><strong>Mean Reversion</strong><span>Expects a quiet price to reverse. Volatility below 2% produces SELL.</span></div><div class="pattern"><strong>Arbitrage</strong><span>Looks for a price discrepancy. This demo checks the price remainder and may produce BUY.</span></div></div></section>
                <section class="box"><h2>Patterns used</h2><div class="patterns"><div class="pattern"><strong>MarketFeed</strong><span>Subject publishes ticks</span></div><div class="pattern"><strong>Strategy</strong><span>Produces order signal</span></div><div class="pattern"><strong>Decorator</strong><span>Fees and risk guards</span></div><div class="pattern"><strong>Template Method</strong><span>Fixed execution workflow</span></div><div class="pattern"><strong>SecurityProxy</strong><span>Protects broker calls</span></div><div class="pattern"><strong>Facade</strong><span>Single client entry point</span></div></div></section>
              </main>
              <script>
                const money = value => '$' + Number(value).toFixed(2);
                function previewStrategy() { const strategy = document.querySelector('#strategy-select').value; const volatility = Number(document.querySelector('#volatility-input').value); const price = Number(document.querySelector('#price-input').value); let signal = 'HOLD'; if (strategy === 'Momentum' && volatility > 0.05) signal = 'BUY'; if (strategy === 'Mean Reversion' && volatility < 0.02) signal = 'SELL'; if (strategy === 'Arbitrage' && price % 10 < 5) signal = 'BUY'; document.querySelector('#strategy').textContent = strategy + ' / ' + signal; }
                async function refresh() { const state = await fetch('/api/state').then(response => response.json()); document.querySelector('#symbol').textContent = state.symbol; document.querySelector('#price').textContent = money(state.price); document.querySelector('#strategy').textContent = state.strategy + ' / ' + state.signal; document.querySelector('#volatility').textContent = (state.volatility * 100).toFixed(2) + '%'; document.querySelector('#observers').textContent = state.observers; document.querySelector('#assets').textContent = money(state.assets); document.querySelector('#trades').textContent = state.trades; document.querySelector('#observer-list').textContent = 'Subscribed: ' + (state.observerNames.length ? state.observerNames.join(', ') : 'none'); }
                async function refreshEvents() { const data = await fetch('/api/trace').then(response => response.json()); document.querySelector('#events').innerHTML = data.events.map(event => '<li>' + event + '</li>').join('') || '<li>No events yet</li>'; }
                async function refreshAll() { await refresh(); await refreshEvents(); }
                document.querySelector('#company-select').addEventListener('change', event => { const option = event.target.selectedOptions[0]; document.querySelector('#price-input').value = option.dataset.price; });
                document.querySelector('#strategy-select').addEventListener('change', previewStrategy);
                document.querySelector('#volatility-input').addEventListener('input', previewStrategy);
                document.querySelector('#price-input').addEventListener('input', previewStrategy);
                document.querySelector('#quote-button').addEventListener('click', async () => { const symbol = encodeURIComponent(document.querySelector('#company-select').value); const price = document.querySelector('#price-input').value; const volatility = document.querySelector('#volatility-input').value; const response = await fetch('/api/quote?symbol=' + symbol + '&price=' + price + '&volatility=' + volatility, { method: 'POST' }); const result = await response.json(); document.querySelector('#quote-message').textContent = result.error || 'Quote published. Try 0.01 volatility to see SELL.'; await refreshAll(); });
                document.querySelectorAll('[data-observer]').forEach(button => button.addEventListener('click', async () => { const name = button.dataset.observer; const state = await fetch('/api/state').then(response => response.json()); const subscribed = state.observerNames.includes(name); await fetch('/api/observers?name=' + name + '&action=' + (subscribed ? 'unsubscribe' : 'subscribe'), { method: 'POST' }); await refreshAll(); }));
                document.querySelector('#trade-button').addEventListener('click', async () => { const symbol = encodeURIComponent(document.querySelector('#company-select').value); const strategy = encodeURIComponent(document.querySelector('#strategy-select').value); const response = await fetch('/api/auto-trade?symbol=' + symbol + '&strategy=' + strategy, { method: 'POST' }); const result = await response.json(); const message = document.querySelector('#message'); message.textContent = result.error || (result.status + ' | gross: ' + money(result.gross) + ' | fee: ' + money(result.fee)); message.style.color = result.error ? '#a63232' : '#236b43'; await refreshAll(); });
                refreshAll();
              </script>
            </body>
            </html>
            """;
}
