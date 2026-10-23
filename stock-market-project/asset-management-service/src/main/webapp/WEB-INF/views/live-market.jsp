<%@ page contentType="text/html; charset=UTF-8" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <title>Live Market</title>
    <style>
        body { background: #f6f8fb; color: #172033; font-family: sans-serif; margin: 0; }
        main { margin: 2rem auto; max-width: 72rem; padding: 0 1rem; }
        .summary { display: grid; gap: 1rem; grid-template-columns: repeat(3, 1fr); margin: 1.5rem 0; }
        .card { background: white; border: 1px solid #dfe5ef; border-radius: .6rem; padding: 1rem; }
        .value { display: block; font-size: 1.5rem; font-weight: bold; margin-top: .35rem; }
        input { border: 1px solid #bcc7d8; border-radius: .3rem; padding: .5rem; }
        #search { margin: 1rem 0; width: min(24rem, 100%); }
        table { background: white; border-collapse: collapse; width: 100%; }
        th, td { border-bottom: 1px solid #e5e9f0; padding: .7rem; text-align: left; }
        th { background: #eef2f7; }
        button { background: #1769e0; border: 0; border-radius: .3rem; color: white; cursor: pointer; padding: .5rem .8rem; }
        button:hover { background: #0d55bc; }
        .status { min-height: 1.5rem; }
        .error { color: #b42318; }
        .empty { color: #667085; text-align: center; }
        @media (max-width: 700px) { .summary { grid-template-columns: 1fr; } table { font-size: .85rem; } }
    </style>
</head>
<body>
<main>
    <h1>Live Market</h1>
    <p>Prices are received from Kafka and refreshed automatically.</p>

    <section class="summary" aria-label="Account summary">
        <div class="card">Cash balance<span class="value" id="cashBalance">$0.00</span></div>
        <div class="card">Invested value<span class="value" id="investedValue">$0.00</span></div>
        <div class="card">Total account value<span class="value" id="totalValue">$0.00</span></div>
    </section>

    <label for="search">Search stocks to buy</label>
    <input id="search" type="search" placeholder="Search by symbol or company">
    <p id="status" class="status" role="status"></p>

    <table>
        <thead>
        <tr>
            <th>Symbol</th>
            <th>Company</th>
            <th>Price</th>
            <th>Last update</th>
            <th>Quantity</th>
            <th>Action</th>
        </tr>
        </thead>
        <tbody id="stocks"></tbody>
    </table>

    <h2>Holdings</h2>
    <table>
        <thead>
        <tr><th>Symbol</th><th>Quantity</th><th>Average price</th><th>Current value</th></tr>
        </thead>
        <tbody id="holdings"></tbody>
    </table>
    <p><a href="${pageContext.request.contextPath}/assets">View assets</a></p>
</main>
<script>
    const money = new Intl.NumberFormat("en-US", { style: "currency", currency: "USD" });
    let market = [];
    let account = { holdings: [] };

    function render() {
        const query = document.getElementById("search").value.toLowerCase();
        const stocks = market.filter(stock =>
            stock.symbol.toLowerCase().includes(query) || stock.name.toLowerCase().includes(query));
        document.getElementById("stocks").innerHTML = stocks.length
            ? stocks.map(stock => "<tr>"
                + "<td>" + escapeHtml(stock.symbol) + "</td>"
                + "<td>" + escapeHtml(stock.name) + "</td>"
                + "<td>" + money.format(stock.price) + "</td>"
                + "<td>" + formatDate(stock.updatedAt) + "</td>"
                + "<td><input id=\"quantity-" + escapeHtml(stock.symbol)
                + "\" type=\"number\" min=\"0.000001\" step=\"0.000001\" value=\"1\"></td>"
                + "<td><button data-symbol=\"" + escapeHtml(stock.symbol) + "\">Buy</button></td>"
                + "</tr>").join("")
            : '<tr><td class="empty" colspan="6">No stocks received from Kafka yet.</td></tr>';
        document.querySelectorAll("#stocks button").forEach(button =>
            button.addEventListener("click", () => buy(button.dataset.symbol)));

        document.getElementById("cashBalance").textContent = money.format(account.cashBalance || 0);
        document.getElementById("investedValue").textContent = money.format(account.investedValue || 0);
        document.getElementById("totalValue").textContent = money.format(account.totalValue || 0);
        document.getElementById("holdings").innerHTML = account.holdings.length
            ? account.holdings.map(holding => {
                const stock = market.find(item => item.symbol === holding.symbol);
                const value = stock ? stock.price * holding.quantity : 0;
                return "<tr><td>" + escapeHtml(holding.symbol) + "</td>"
                    + "<td>" + holding.quantity + "</td><td>"
                    + money.format(holding.averagePrice) + "</td><td>"
                    + money.format(value) + "</td></tr>";
            }).join("")
            : '<tr><td class="empty" colspan="4">No stocks owned.</td></tr>';
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
        document.getElementById("status").textContent = "Bought " + quantity + " " + symbol + ".";
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
