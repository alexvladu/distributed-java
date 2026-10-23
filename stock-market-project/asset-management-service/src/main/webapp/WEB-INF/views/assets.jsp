<%@ page contentType="text/html; charset=UTF-8" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Portfolio</title>
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
        .headline { align-items: flex-end; display: flex; flex-wrap: wrap; gap: 1rem; justify-content: space-between; }
        .total { text-align: right; }
        .total small { color: var(--muted); display: block; font-size: .9rem; }
        .total strong { display: block; font-size: 2.25rem; font-variant-numeric: tabular-nums; letter-spacing: -.02em; line-height: 1.1; }
        .cash td { background: #f1f8f5; font-weight: 600; }
        @media (max-width: 600px) { .headline { align-items: flex-start; flex-direction: column; } .total { text-align: left; } }
    </style>
</head>
<body>
<header class="top">
    <div class="bar">
        <a class="brand" href="${pageContext.request.contextPath}/">Stock Portfolio</a>
        <nav aria-label="Main">
            <a href="${pageContext.request.contextPath}/">Home</a>
            <a href="${pageContext.request.contextPath}/live-market">Live market</a>
            <a href="${pageContext.request.contextPath}/assets" aria-current="page">Assets</a>
        </nav>
    </div>
</header>
<main>
    <section class="headline">
        <div>
            <h1>Portfolio</h1>
            <p class="lede">Your cash and investments, updated live.</p>
        </div>
        <div class="total"><small>Total balance</small><strong id="totalValue">$0.00</strong></div>
    </section>
    <p id="status" class="status" role="status"></p>
    <div class="panel">
        <table>
            <thead><tr><th>Asset</th><th class="num">Quantity</th><th class="num">Price</th><th class="num">Value</th><th class="num">Sell</th></tr></thead>
            <tbody id="assets">
            <tr><td class="empty" colspan="5">Connecting to live account...</td></tr>
            </tbody>
        </table>
    </div>
</main>
<script>
    const money = new Intl.NumberFormat("en-US", { style: "currency", currency: "USD" });
    let market = [];
    let account = { cashBalance: 0, holdings: [] };
    const drafts = {};

    document.addEventListener("input", event => {
        if (event.target.matches("#assets input")) drafts[event.target.id] = event.target.value;
    });

    function render() {
        document.getElementById("totalValue").textContent = money.format(account.totalValue || 0);
        const rows = ['<tr class="cash"><td>Cash (USD)</td><td class="num">-</td><td class="num">-</td><td class="num">'
            + money.format(account.cashBalance || 0) + '</td><td></td></tr>'];
        account.holdings.forEach(holding => {
            const stock = market.find(item => item.symbol === holding.symbol);
            const price = stock ? stock.price : 0;
            const inputId = "sell-quantity-" + holding.symbol;
            const draft = drafts[inputId] !== undefined ? drafts[inputId] : holding.quantity;
            rows.push("<tr><td><span class=\"sym\">" + escapeHtml(holding.symbol) + "</span> <span class=\"sub\">"
                + escapeHtml(holding.name) + "</span></td><td class=\"num\">" + holding.quantity
                + "</td><td class=\"num\">" + money.format(price)
                + "</td><td class=\"num\">" + money.format(price * holding.quantity)
                + "</td><td class=\"num\"><input id=\"" + escapeHtml(inputId)
                + "\" type=\"number\" aria-label=\"Quantity of " + escapeHtml(holding.symbol) + " to sell\""
                + " min=\"0.000001\" max=\"" + holding.quantity
                + "\" step=\"0.000001\" value=\"" + escapeHtml(String(draft))
                + "\"><button class=\"btn sell\" data-symbol=\"" + escapeHtml(holding.symbol)
                + "\">Sell</button></td></tr>");
        });
        if (!account.holdings.length) {
            rows.push('<tr><td class="empty" colspan="5">No investments yet. Open the live market to buy your first stock.</td></tr>');
        }
        document.getElementById("assets").innerHTML = rows.join("");
        document.querySelectorAll("#assets button").forEach(button =>
            button.addEventListener("click", () => sell(button.dataset.symbol)));
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

    const events = new EventSource("${pageContext.request.contextPath}/assets/events");
    events.onopen = () => {
        document.getElementById("status").textContent = "Live connection active.";
        document.getElementById("status").className = "status";
    };
    events.onmessage = event => {
        const data = JSON.parse(event.data);
        market = data.stocks;
        account = data.account;
        render();
    };
    events.onerror = () => {
        document.getElementById("status").textContent = "Live connection interrupted; reconnecting...";
        document.getElementById("status").className = "status error";
    };

    function escapeHtml(value) {
        return value.replaceAll("&", "&amp;").replaceAll("<", "&lt;")
            .replaceAll(">", "&gt;").replaceAll('"', "&quot;").replaceAll("'", "&#039;");
    }
</script>
</body>
</html>
