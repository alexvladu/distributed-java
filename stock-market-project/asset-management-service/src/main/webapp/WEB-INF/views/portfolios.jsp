<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" session="false" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <%@ include file="/WEB-INF/views/fragments/head.jspf" %>
    <title>Portfolios &middot; Stock Portfolio</title>
    <script src="${pageContext.request.contextPath}/static/js/portfolios.js" defer></script>
</head>
<body data-base="${pageContext.request.contextPath}">
<%@ include file="/WEB-INF/views/fragments/header.jspf" %>
<main id="main" class="page">
    <div class="page-head">
        <div>
            <h1>Portfolios &amp; account</h1>
            <p class="lede">Your profile lives in PostgreSQL, portfolios and cash in SQL Server, and every trade in
                Oracle. This page joins all three.</p>
        </div>
    </div>

    <section class="card" aria-labelledby="create-title">
        <div class="section-head">
            <h2 id="create-title">New portfolio</h2>
            <span class="badge db">SQL Server</span>
        </div>
        <form id="create-form" class="inline-form" novalidate>
            <div class="field">
                <label for="new-portfolio-name">Name</label>
                <input id="new-portfolio-name" class="input" name="portfolioName" maxlength="120" required
                       placeholder="e.g. Long term" autocomplete="off">
            </div>
            <button class="btn" type="submit">Create with $1,000.00 cash</button>
        </form>
    </section>

    <section class="section" aria-labelledby="list-title">
        <div class="section-head">
            <h2 id="list-title">Your portfolios</h2>
            <span class="badges"><span class="badge db">SQL Server</span><span class="badge db">Oracle</span></span>
        </div>
        <div class="panel">
            <table>
                <caption class="sr-only">Portfolios with their cash balance and number of trades</caption>
                <thead>
                <tr>
                    <th scope="col">Name</th>
                    <th scope="col" class="num">Cash</th>
                    <th scope="col" class="num">Trades</th>
                    <th scope="col">Created</th>
                    <th scope="col" class="num">Actions</th>
                </tr>
                </thead>
                <tbody id="portfolio-rows">
                <tr><td class="empty" colspan="5">Loading&hellip;</td></tr>
                </tbody>
            </table>
        </div>
    </section>

    <div class="grid-2 section">
        <section class="card" aria-labelledby="profile-title">
            <div class="section-head">
                <h2 id="profile-title">Profile</h2>
                <span class="badge db">PostgreSQL</span>
            </div>
            <form id="profile-form" class="stack-form" novalidate>
                <div class="field">
                    <label for="profile-username">Username</label>
                    <input id="profile-username" class="input" name="username" autocomplete="username" required
                           minlength="3" maxlength="80" pattern="[A-Za-z0-9_]{3,80}"
                           aria-describedby="profile-username-help">
                    <span class="help" id="profile-username-help">3&ndash;80 letters, numbers, or underscores.</span>
                </div>
                <div class="field">
                    <label for="profile-email">Email</label>
                    <input id="profile-email" class="input" name="email" type="email" autocomplete="email" required
                           maxlength="255">
                </div>
                <div>
                    <button class="btn" type="submit">Save profile</button>
                </div>
            </form>
        </section>
        <section class="card" aria-labelledby="danger-title">
            <h2 id="danger-title">Delete account</h2>
            <p class="lede">Permanently removes your user (PostgreSQL), all portfolios (SQL Server), and all
                trades (Oracle). This cannot be undone.</p>
            <div class="mt-1">
                <button class="btn danger" type="button" id="delete-account">Delete my account</button>
            </div>
        </section>
    </div>
</main>
<%@ include file="/WEB-INF/views/fragments/footer.jspf" %>
</body>
</html>
