<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" session="false" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <%@ include file="/WEB-INF/views/fragments/head.jspf" %>
    <title>Overview &middot; Stock Portfolio</title>
    <script src="${pageContext.request.contextPath}/static/js/home.js" defer></script>
</head>
<body data-base="${pageContext.request.contextPath}">
<%@ include file="/WEB-INF/views/fragments/header.jspf" %>
<main id="main" class="page">
    <section class="hero">
        <div>
            <h1 id="greeting">Your portfolios, live.</h1>
            <p class="lede">Prices stream from the stock-market service through Kafka. Every portfolio is
                revalued as the market moves, and you can trade fractional shares from the live market.</p>
        </div>
        <svg class="art" viewBox="0 0 240 120" fill="none" stroke="currentColor" stroke-width="4"
             stroke-linecap="round" stroke-linejoin="round" aria-hidden="true" focusable="false">
            <path d="M6 96 L40 78 L72 88 L108 52 L140 64 L176 30 L206 40 L234 14"/>
            <circle cx="234" cy="14" r="7" fill="currentColor" stroke="none"/>
        </svg>
    </section>

    <section class="kpis" aria-label="Totals across all portfolios">
        <div class="kpi main">
            <small>Net worth</small>
            <span class="value" id="kpi-total">&ndash;</span>
            <span class="hint" id="kpi-count"></span>
        </div>
        <div class="kpi"><small>Cash</small><span class="value" id="kpi-cash">&ndash;</span></div>
        <div class="kpi"><small>Invested</small><span class="value" id="kpi-invested">&ndash;</span></div>
        <div class="kpi">
            <small>Unrealized P/L</small>
            <span class="value" id="kpi-profit">&ndash;</span>
        </div>
    </section>

    <div class="grid-2 section">
        <section class="card" aria-labelledby="portfolios-title">
            <div class="section-head">
                <h2 id="portfolios-title">Portfolios</h2>
                <a class="btn ghost sm" href="${pageContext.request.contextPath}/portfolios">Manage</a>
            </div>
            <ul class="movers" id="portfolio-list">
                <li class="empty">Loading portfolios&hellip;</li>
            </ul>
        </section>
        <section class="card" aria-labelledby="market-title">
            <div class="section-head">
                <h2 id="market-title">Market</h2>
                <span class="pill warn" id="stream-status">Connecting</span>
            </div>
            <ul class="movers" id="movers">
                <li class="empty">Waiting for the first prices&hellip;</li>
            </ul>
            <div class="mt-1">
                <a class="btn sm" href="${pageContext.request.contextPath}/live-market">Open live market</a>
            </div>
        </section>
    </div>
</main>
<%@ include file="/WEB-INF/views/fragments/footer.jspf" %>
</body>
</html>
