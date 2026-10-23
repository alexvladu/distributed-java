<%@ page contentType="text/html; charset=UTF-8" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Portfolio</title>
    <style>
        body { background: #f6f8fb; color: #172033; font-family: sans-serif; margin: 0; }
        main { margin: 2rem auto; max-width: 72rem; padding: 0 1rem; }
        nav { display: flex; gap: 1rem; margin-bottom: 2rem; }
        nav a { color: #1769e0; font-weight: bold; text-decoration: none; }
        .headline { align-items: end; display: flex; justify-content: space-between; gap: 1rem; }
        h1 { margin-bottom: .35rem; }
        .total { color: #1769e0; font-size: 2rem; font-weight: bold; }
        table { background: white; border-collapse: collapse; margin-top: 1.5rem; width: 100%; }
        th, td { border-bottom: 1px solid #e5e9f0; padding: .8rem; text-align: left; }
        th { background: #eef2f7; }
        .cash { background: #f0fdf4; font-weight: bold; }
        .symbol { font-weight: bold; }
        .empty { color: #667085; text-align: center; }
        .status { min-height: 1.5rem; }
        .error { color: #b42318; }
        @media (max-width: 600px) {
            .headline { align-items: start; flex-direction: column; }
            table { font-size: .85rem; }
        }
    </style>
</head>
<body>
<main>
    <nav>
        <a href="${pageContext.request.contextPath}/">Home</a>
        <a href="${pageContext.request.contextPath}/live-market">Live market</a>
        <a href="${pageContext.request.contextPath}/assets">Assets</a>
    </nav>
    <section class="headline">
        <div>
            <h1>Portfolio</h1>
            <p>Cash and investments in your account. Updates arrive live from Kafka.</p>
        </div>
        <div class="total">Total balance: <span id="totalValue">$0.00</span></div>
    </section>
    <p id="status" class="status" role="status"></p>
    <table>
        <thead><tr><th>Asset</th><th>Quantity</th><th>Price</th><th>Value</th></tr></thead>
        <tbody id="assets">
        <tr><td class="empty" colspan="4">Connecting to live account...</td></tr>
        </tbody>
    </table>
</main>
<script>
    const money = new Intl.NumberFormat("en-US", { style: "currency", currency: "USD" });
    let market = [];
    let account = { cashBalance: 0, holdings: [] };

    function render() {
        document.getElementById("totalValue").textContent = money.format(account.totalValue || 0);
        const rows = ['<tr class="cash"><td>Cash (USD)</td><td>-</td><td>-</td><td>'
            + money.format(account.cashBalance || 0) + "</td></tr>"];
        account.holdings.forEach(holding => {
            const stock = market.find(item => item.symbol === holding.symbol);
            const price = stock ? stock.price : holding.averagePrice;
            rows.push("<tr><td class=\"symbol\">" + escapeHtml(holding.symbol) + " - "
                + escapeHtml(holding.name) + "</td><td>" + holding.quantity + "</td><td>"
                + money.format(price) + "</td><td>" + money.format(price * holding.quantity)
                + "</td></tr>");
        });
        if (!account.holdings.length) {
            rows.push('<tr><td class="empty" colspan="4">No investments yet. Visit the live market to buy stocks.</td></tr>');
        }
        document.getElementById("assets").innerHTML = rows.join("");
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
