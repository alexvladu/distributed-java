<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" session="false" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <%@ include file="/WEB-INF/views/fragments/head.jspf" %>
    <title>Assets &middot; Stock Portfolio</title>
    <script src="${pageContext.request.contextPath}/static/js/assets.js" defer></script>
</head>
<body data-base="${pageContext.request.contextPath}">
<%@ include file="/WEB-INF/views/fragments/header.jspf" %>
<main id="main" class="page">
    <div class="page-head">
        <div>
            <h1 id="portfolio-name">Assets</h1>
            <p class="lede">Holdings valued at live prices. Cost basis uses the moving average of your trades.</p>
        </div>
        <span class="pill warn" id="stream-status">Connecting</span>
    </div>

    <section class="kpis" aria-label="Portfolio value">
        <div class="kpi main"><small>Total value</small><span class="value" id="kpi-total">&ndash;</span></div>
        <div class="kpi"><small>Cash</small><span class="value" id="kpi-cash">&ndash;</span></div>
        <div class="kpi">
            <small>Invested</small>
            <span class="value" id="kpi-invested">&ndash;</span>
            <span class="hint" id="kpi-cost"></span>
        </div>
        <div class="kpi"><small>Unrealized P/L</small><span class="value" id="kpi-profit">&ndash;</span></div>
    </section>

    <section class="card section" aria-labelledby="allocation-title">
        <h2 id="allocation-title">Allocation</h2>
        <div class="mt-1" id="allocation"></div>
        <ul class="legend" id="allocation-legend"></ul>
    </section>

    <section class="section" aria-labelledby="holdings-title">
        <div class="section-head">
            <h2 id="holdings-title">Holdings</h2>
            <a class="btn ghost sm" href="${pageContext.request.contextPath}/live-market">Buy more</a>
        </div>
        <div class="panel">
            <table>
                <caption class="sr-only">Holdings in the active portfolio</caption>
                <thead>
                <tr>
                    <th scope="col">Asset</th>
                    <th scope="col" class="num">Quantity</th>
                    <th scope="col" class="num">Avg cost</th>
                    <th scope="col" class="num">Price</th>
                    <th scope="col" class="num">Market value</th>
                    <th scope="col" class="num">Unrealized P/L</th>
                    <th scope="col" class="num">Weight</th>
                    <th scope="col" class="num">Sell</th>
                </tr>
                </thead>
                <tbody id="holdings">
                <tr><td class="empty" colspan="8">Loading&hellip;</td></tr>
                </tbody>
            </table>
        </div>
    </section>

    <section class="section" aria-labelledby="history-title">
        <div class="section-head">
            <h2 id="history-title">Trade history</h2>
            <span class="badge db">Oracle ledger</span>
        </div>
        <p class="lede">Correct a quantity or void a trade; the cash difference is settled at the original
            price, and changes that would leave a later sale without shares are refused.</p>
        <div class="panel mt-1">
            <table>
                <caption class="sr-only">All trades in the active portfolio</caption>
                <thead>
                <tr>
                    <th scope="col">Time</th>
                    <th scope="col">Side</th>
                    <th scope="col">Stock</th>
                    <th scope="col" class="num">Quantity</th>
                    <th scope="col" class="num">Price</th>
                    <th scope="col" class="num">Amount</th>
                    <th scope="col" class="num">Actions</th>
                </tr>
                </thead>
                <tbody id="trades">
                <tr><td class="empty" colspan="7">Loading&hellip;</td></tr>
                </tbody>
            </table>
        </div>
    </section>
</main>
<%@ include file="/WEB-INF/views/fragments/footer.jspf" %>
</body>
</html>
