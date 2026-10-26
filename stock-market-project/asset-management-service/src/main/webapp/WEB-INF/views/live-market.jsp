<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" session="false" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <%@ include file="/WEB-INF/views/fragments/head.jspf" %>
    <title>Live market &middot; Stock Portfolio</title>
    <script src="${pageContext.request.contextPath}/static/js/market.js" defer></script>
</head>
<body data-base="${pageContext.request.contextPath}">
<%@ include file="/WEB-INF/views/fragments/header.jspf" %>
<main id="main" class="page">
    <div class="page-head">
        <div>
            <h1>Live market</h1>
            <p class="lede">Orders go to <strong id="portfolio-name">your active portfolio</strong> and execute at the
                latest streamed price.</p>
        </div>
        <span class="pill warn" id="stream-status">Connecting</span>
    </div>

    <section class="kpis" aria-label="Active portfolio">
        <div class="kpi main"><small>Total value</small><span class="value" id="kpi-total">&ndash;</span></div>
        <div class="kpi"><small>Cash available</small><span class="value" id="kpi-cash">&ndash;</span></div>
        <div class="kpi"><small>Invested</small><span class="value" id="kpi-invested">&ndash;</span></div>
        <div class="kpi"><small>Unrealized P/L</small><span class="value" id="kpi-profit">&ndash;</span></div>
    </section>

    <section class="section" aria-labelledby="quotes-title">
        <div class="section-head">
            <h2 id="quotes-title">Stocks</h2>
            <div>
                <label class="sr-only" for="search">Search stocks</label>
                <input id="search" class="input search" type="search" placeholder="Search symbol or company"
                       autocomplete="off">
            </div>
        </div>
        <div class="panel">
            <table>
                <caption class="sr-only">Live stock quotes</caption>
                <thead>
                <tr>
                    <th scope="col">Stock</th>
                    <th scope="col">Trend</th>
                    <th scope="col" class="num">Price</th>
                    <th scope="col" class="num">Change</th>
                    <th scope="col" class="num">Updated</th>
                    <th scope="col" class="num">Owned</th>
                    <th scope="col" class="num">Trade</th>
                </tr>
                </thead>
                <tbody id="quotes">
                <tr><td class="empty" colspan="7">Waiting for the first prices&hellip;</td></tr>
                </tbody>
            </table>
        </div>
    </section>

    <section class="section" aria-labelledby="recent-title">
        <div class="section-head">
            <h2 id="recent-title">Recent trades</h2>
            <a class="btn ghost sm" href="${pageContext.request.contextPath}/assets">All holdings &amp; history</a>
        </div>
        <div class="panel">
            <table>
                <caption class="sr-only">Most recent trades in the active portfolio</caption>
                <thead>
                <tr>
                    <th scope="col">Time</th>
                    <th scope="col">Side</th>
                    <th scope="col">Stock</th>
                    <th scope="col" class="num">Quantity</th>
                    <th scope="col" class="num">Price</th>
                    <th scope="col" class="num">Amount</th>
                </tr>
                </thead>
                <tbody id="recent-trades">
                <tr><td class="empty" colspan="6">Loading&hellip;</td></tr>
                </tbody>
            </table>
        </div>
    </section>
</main>
<%@ include file="/WEB-INF/views/fragments/footer.jspf" %>
</body>
</html>
