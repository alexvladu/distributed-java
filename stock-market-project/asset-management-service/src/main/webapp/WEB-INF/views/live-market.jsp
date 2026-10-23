<%@ page contentType="text/html; charset=UTF-8" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Live Market</title>
    <style>
        :root { --bg:#f3f5f8; --surface:#fff; --ink:#0f1b2d; --muted:#5d6b80; --line:#dde3ea; --head:#eef1f5;
                --accent:#1458c4; --ok:#0b7a5a; --bad:#b3261e; }
        * { box-sizing: border-box; }
        body { background: var(--bg); color: var(--ink); margin: 0;
               font: 16px/1.5 ui-sans-serif, system-ui, -apple-system, "Segoe UI", Roboto, sans-serif; }
        a { color: var(--accent); }
        :focus-visible { outline: 2px solid var(--accent); outline-offset: 2px; }
        .top { background: var(--surface); border-bottom: 1px solid var(--line); }
        .bar { align-items: center; display: flex; gap: .25rem; height: 3.5rem; margin: 0 auto; max-width: 72rem; padding: 0 1rem; }
        .brand { color: var(--ink); font-weight: 700; margin-right: auto; text-decoration: none; }
        .bar nav a { border-radius: .4rem; color: var(--muted); font-weight: 600; padding: .4rem .7rem; text-decoration: none; }
        .bar nav a:hover { color: var(--ink); }
        .bar nav a[aria-current=page] { background: var(--bg); color: var(--ink); }
        main { margin: 0 auto; max-width: 72rem; padding: 2rem 1rem 4rem; }
        h1 { font-size: 1.75rem; letter-spacing: -.02em; line-height: 1.2; margin: 0 0 .25rem; }
        h2 { font-size: 1.15rem; margin: 2.5rem 0 .75rem; }
        .lede { color: var(--muted); margin: 0; }
        .panel { background: var(--surface); border: 1px solid var(--line); border-radius: .75rem; overflow-x: auto; }
        table { border-collapse: collapse; width: 100%; }
        th, td { border-bottom: 1px solid var(--line); padding: .75rem 1rem; text-align: left; white-space: nowrap; }
        tr:last-child td { border-bottom: 0; }
        th { background: var(--head); color: var(--muted); font-size: .85rem; font-weight: 600; }
        .num, th.num { font-variant-numeric: tabular-nums; text-align: right; }
        .sym { font-weight: 700; }
        .sub { color: var(--muted); font-weight: 400; }
        .empty { color: var(--muted); padding: 2rem 1rem; text-align: center; }
        input[type=number], input[type=search] { background: #fff; border: 1px solid #c3ccd8; border-radius: .4rem; font: inherit; padding: .4rem .6rem; }
        input[type=number] { text-align: right; width: 7rem; }
        .btn { background: var(--accent); border: 1px solid var(--accent); border-radius: .4rem; color: #fff; cursor: pointer;
               font: inherit; font-weight: 600; padding: .4rem .9rem; }
        .btn:hover { filter: brightness(.92); }
        .btn.sell { background: #fff; border-color: var(--bad); color: var(--bad); margin-left: .4rem; }
        .btn.sell:hover { background: var(--bad); color: #fff; filter: none; }
        .status { align-items: center; color: var(--muted); display: flex; gap: .5rem; margin: 1rem 0; min-height: 1.5rem; }
        .status::before { background: var(--ok); border-radius: 50%; content: ""; height: .5rem; width: .5rem; }
        .status:empty::before { display: none; }
        .status.error { color: var(--bad); }
        .status.error::before { background: var(--bad); }
        @media (prefers-reduced-motion: no-preference) { .btn { transition: background .15s, color .15s; } }
        .summary { background: var(--surface); border: 1px solid var(--line); border-radius: .75rem; display: grid;
                   grid-template-columns: 1.3fr 1fr 1fr; margin: 1.5rem 0; }
        .summary div { padding: 1.1rem 1.4rem; }
        .summary div + div { border-left: 1px solid var(--line); }
        .summary small { color: var(--muted); display: block; font-size: .9rem; }
        .summary .value { display: block; font-size: 1.4rem; font-variant-numeric: tabular-nums; font-weight: 700; }
        .summary .main .value { font-size: 2rem; letter-spacing: -.02em; }
        .tools { align-items: center; display: flex; flex-wrap: wrap; gap: .75rem; margin-top: 1.5rem; }
        .tools label { font-weight: 600; }
        #search { width: min(24rem, 100%); }
        .tools + .status { margin-top: .75rem; }
        @media (max-width: 700px) {
            .summary { grid-template-columns: 1fr; }
            .summary div + div { border-left: 0; border-top: 1px solid var(--line); }
        }
    </style>
</head>
<body>
<header class="top">
    <div class="bar">
        <a class="brand" href="${pageContext.request.contextPath}/">Stock Portfolio</a>
        <nav aria-label="Main">
            <a href="${pageContext.request.contextPath}/">Home</a>
            <a href="${pageContext.request.contextPath}/live-market" aria-current="page">Live market</a>
            <a href="${pageContext.request.contextPath}/assets">Assets</a>
        </nav>
    </div>
</header>
<main>
    <h1>Live market</h1>
    <p class="lede">Prices refresh automatically. Pick a quantity and buy.</p>

    <section class="summary" aria-label="Account summary">
        <div class="main"><small>Total account value</small><span class="value" id="totalValue">$0.00</span></div>
        <div><small>Cash balance</small><span class="value" id="cashBalance">$0.00</span></div>
        <div><small>Invested value</small><span class="value" id="investedValue">$0.00</span></div>
    </section>

    <div class="tools">
        <label for="search">Search stocks</label>
        <input id="search" type="search" placeholder="Symbol or company">
    </div>
    <p id="status" class="status" role="status"></p>

    <div class="panel">
        <table>
            <thead>
            <tr>
                <th>Symbol</th>
                <th>Company</th>
                <th class="num">Price</th>
                <th class="num">Last update</th>
                <th class="num">Quantity</th>
                <th></th>
            </tr>
            </thead>
            <tbody id="stocks"></tbody>
        </table>
    </div>

    <h2>Holdings</h2>
    <div class="panel">
        <table>
            <thead>
            <tr><th>Symbol</th><th class="num">Quantity</th><th class="num">Current value</th><th class="num">Sell</th></tr>
            </thead>
            <tbody id="holdings"></tbody>
        </table>
    </div>
</main>
<script>
    const money = new Intl.NumberFormat("en-US", { style: "currency", currency: "USD" });
    let market = [];
    let account = { holdings: [] };
    const drafts = {};

    document.addEventListener("input", event => {
        if (event.target.matches("#stocks input, #holdings input")) drafts[event.target.id] = event.target.value;
    });

    function render() {
        const query = document.getElementById("search").value.toLowerCase();
        const stocks = market.filter(stock =>
            stock.symbol.toLowerCase().includes(query) || stock.name.toLowerCase().includes(query));
        document.getElementById("stocks").innerHTML = stocks.length
            ? stocks.map(stock => {
                const inputId = "quantity-" + stock.symbol;
                const draft = drafts[inputId] !== undefined ? drafts[inputId] : "1";
                return "<tr>"
                + "<td class=\"sym\">" + escapeHtml(stock.symbol) + "</td>"
                + "<td>" + escapeHtml(stock.name) + "</td>"
                + "<td class=\"num\">" + money.format(stock.price) + "</td>"
                + "<td class=\"num sub\">" + formatDate(stock.updatedAt) + "</td>"
                + "<td class=\"num\"><input id=\"" + escapeHtml(inputId)
                + "\" type=\"number\" aria-label=\"Quantity of " + escapeHtml(stock.symbol) + " to buy\""
                + " min=\"0.000001\" step=\"0.000001\" value=\"" + escapeHtml(String(draft)) + "\"></td>"
                + "<td class=\"num\"><button class=\"btn\" data-symbol=\"" + escapeHtml(stock.symbol) + "\">Buy</button></td>"
                + "</tr>";
            }).join("")
            : '<tr><td class="empty" colspan="6">' + (market.length
                ? "No stocks match your search."
                : "Waiting for the first price updates...") + '</td></tr>';
        document.querySelectorAll("#stocks button").forEach(button =>
            button.addEventListener("click", () => buy(button.dataset.symbol)));

        document.getElementById("cashBalance").textContent = money.format(account.cashBalance || 0);
        document.getElementById("investedValue").textContent = money.format(account.investedValue || 0);
        document.getElementById("totalValue").textContent = money.format(account.totalValue || 0);
        document.getElementById("holdings").innerHTML = account.holdings.length
            ? account.holdings.map(holding => {
                const stock = market.find(item => item.symbol === holding.symbol);
                const value = stock ? stock.price * holding.quantity : 0;
                const inputId = "sell-quantity-" + holding.symbol;
                const draft = drafts[inputId] !== undefined ? drafts[inputId] : holding.quantity;
                return "<tr><td class=\"sym\">" + escapeHtml(holding.symbol) + "</td>"
                    + "<td class=\"num\">" + holding.quantity + "</td><td class=\"num\">" + money.format(value) + "</td>"
                    + "<td class=\"num\"><input id=\"" + escapeHtml(inputId)
                    + "\" type=\"number\" aria-label=\"Quantity of " + escapeHtml(holding.symbol) + " to sell\""
                    + " min=\"0.000001\" max=\"" + holding.quantity
                    + "\" step=\"0.000001\" value=\"" + escapeHtml(String(draft)) + "\">"
                    + "<button class=\"btn sell\" data-symbol=\"" + escapeHtml(holding.symbol)
                    + "\">Sell</button></td></tr>";
            }).join("")
            : '<tr><td class="empty" colspan="4">You don\'t own any stocks yet. Buy one from the list above.</td></tr>';
        document.querySelectorAll("#holdings button.sell").forEach(button =>
            button.addEventListener("click", () => sell(button.dataset.symbol)));
    }

    async function refresh() {
        try {
            const response = await fetch("${pageContext.request.contextPath}/live-market/data", {
                cache: "no-store"
            });
            if (!response.ok) throw new Error("Unable to load market data");
            const data = await response.json();
            market = data.stocks;
            account = data.account;
            render();
        } catch (error) {
            document.getElementById("status").textContent = error.message;
            document.getElementById("status").className = "status error";
        }
    }

    async function buy(symbol) {
        const quantity = document.getElementById("quantity-" + symbol).value;
        const body = new URLSearchParams({ symbol, quantity });
        const response = await fetch("${pageContext.request.contextPath}/live-market/buy", {
            method: "POST",
            headers: { "Content-Type": "application/x-www-form-urlencoded" },
            body
        });
        const data = await response.json();
        if (!response.ok) {
            document.getElementById("status").textContent = data.error;
            document.getElementById("status").className = "status error";
            return;
        }
        account = data;
        delete drafts["quantity-" + symbol];
        document.getElementById("status").textContent = "Bought " + quantity + " " + symbol + ".";
        document.getElementById("status").className = "status";
        render();
    }

    async function sell(symbol) {
        const quantity = document.getElementById("sell-quantity-" + symbol).value;
        const body = new URLSearchParams({ symbol, quantity });
        const response = await fetch("${pageContext.request.contextPath}/live-market/sell", {
            method: "POST",
            headers: { "Content-Type": "application/x-www-form-urlencoded" },
            body
        });
        const data = await response.json();
        if (!response.ok) {
            document.getElementById("status").textContent = data.error;
            document.getElementById("status").className = "status error";
            return;
        }
        account = data;
        delete drafts["sell-quantity-" + symbol];
        document.getElementById("status").textContent = "Sold " + quantity + " " + symbol + ".";
        document.getElementById("status").className = "status";
        render();
    }

    function formatDate(value) {
        return value ? new Date(value).toLocaleTimeString() : "Waiting";
    }

    function escapeHtml(value) {
        return value.replaceAll("&", "&amp;").replaceAll("<", "&lt;")
            .replaceAll(">", "&gt;").replaceAll('"', "&quot;").replaceAll("'", "&#039;");
    }

    document.getElementById("search").addEventListener("input", render);
    refresh();
    const events = new EventSource("${pageContext.request.contextPath}/live-market/events");
    events.onopen = () => {
        document.getElementById("status").textContent = "Live connection active.";
        document.getElementById("status").className = "status";
    };
    events.onmessage = event => {
        const update = JSON.parse(event.data);
        const index = market.findIndex(stock => stock.symbol === update.stock.symbol);
        if (index >= 0) {
            market[index] = update.stock;
        } else {
            market.push(update.stock);
        }
        account = update.account;
        render();
    };
    events.onerror = () => {
        document.getElementById("status").textContent = "Live connection interrupted; reconnecting...";
        document.getElementById("status").className = "status error";
    };
</script>
</body>
</html>
