"use strict";

(async () => {
    const { h, format } = App;
    const quotes = new Map();
    const portfolioRows = new Map();
    const quoteRows = new Map();
    let accounts = [];
    let accountsLoaded = false;

    const user = await App.initLayout();
    if (user) {
        document.getElementById("greeting").textContent = "Welcome back, " + user.username + ".";
    }

    function setKpi(id, text, className) {
        const element = document.getElementById(id);
        element.textContent = text;
        element.className = "value" + (className ? " " + className : "");
    }

    function renderTotals(valued) {
        const sum = key => valued.reduce((total, account) => total + Number(account[key] || 0), 0);
        const profit = App.profit(sum("unrealizedProfit"), sum("costBasis"));
        setKpi("kpi-total", format.money(sum("totalValue")));
        setKpi("kpi-cash", format.money(sum("cashBalance")));
        setKpi("kpi-invested", format.money(sum("investedValue")));
        setKpi("kpi-profit", profit.text, profit.className);
        document.getElementById("kpi-count").textContent =
            valued.length === 1 ? "Across 1 portfolio" : "Across " + valued.length + " portfolios";
    }

    function buildPortfolioList() {
        const list = document.getElementById("portfolio-list");
        portfolioRows.clear();
        if (!accounts.length) {
            list.replaceChildren(h("li", { class: "empty" }, "No portfolios yet. ",
                h("a", { href: App.base + "/portfolios", text: "Create one" }), "."));
            return;
        }
        list.replaceChildren(...accounts.map(account => {
            const detail = h("span", { class: "sub" });
            const profit = h("span", { class: "change" });
            const total = h("span", { class: "num sym" });
            portfolioRows.set(account.portfolioId, { detail, profit, total });
            return h("li", {},
                h("div", { class: "stack" },
                    h("a", {
                        href: App.base + "/assets",
                        class: "sym",
                        text: account.portfolioName,
                        onclick: () => App.setActivePortfolio(account.portfolioId)
                    }),
                    detail),
                profit,
                total);
        }));
    }

    function updatePortfolioList(valued) {
        valued.forEach(account => {
            const row = portfolioRows.get(account.portfolioId);
            if (!row) {
                return;
            }
            const profit = App.profit(account.unrealizedProfit, account.costBasis);
            const holdings = account.holdings.length === 1 ? "1 holding" : account.holdings.length + " holdings";
            row.detail.textContent = "Cash " + format.money(account.cashBalance) + " \u00B7 " + holdings;
            row.profit.textContent = profit.text;
            row.profit.className = "change " + profit.className;
            row.total.textContent = format.money(account.totalValue);
        });
    }

    function render() {
        if (!accountsLoaded) {
            return;
        }
        const valued = accounts.map(account => App.revalue(account, quotes));
        renderTotals(valued);
        updatePortfolioList(valued);
    }

    function buildQuoteRow(quote) {
        const spark = h("span", {});
        const price = h("span", { class: "num sym" });
        const change = h("span", { class: "change" });
        const row = h("li", {},
            h("div", { class: "stack" }, h("span", { class: "sym", text: quote.symbol }),
                h("span", { class: "sub", text: quote.name })),
            spark,
            h("div", { class: "stack num" }, price, change));
        quoteRows.set(quote.symbol, { row, spark, price, change });
        return row;
    }

    function updateQuoteRow(quote, animate) {
        const parts = quoteRows.get(quote.symbol);
        const move = App.change(quote.price, quote.previousPrice);
        parts.spark.replaceChildren(App.sparkline(quote.history));
        parts.price.textContent = format.money(quote.price);
        parts.change.textContent = move.text;
        parts.change.className = "change " + move.className;
        if (animate) {
            App.flash(parts.row, move.className);
        }
    }

    function renderMarket(list) {
        const container = document.getElementById("movers");
        quoteRows.clear();
        if (!list.length) {
            container.replaceChildren(h("li", { class: "empty", text: "Waiting for the first prices\u2026" }));
            return;
        }
        container.replaceChildren(...list.map(buildQuoteRow));
        list.forEach(quote => updateQuoteRow(quote, false));
    }

    App.connectMarket({
        onSnapshot: list => {
            quotes.clear();
            list.forEach(quote => quotes.set(quote.symbol, quote));
            renderMarket(list);
            render();
        },
        onQuote: quote => {
            const isNew = !quotes.has(quote.symbol);
            quotes.set(quote.symbol, quote);
            if (isNew) {
                renderMarket([...quotes.values()].sort((a, b) => a.symbol.localeCompare(b.symbol)));
            } else {
                updateQuoteRow(quote, true);
            }
            render();
        },
        onStatus: status => App.renderStreamStatus(document.getElementById("stream-status"), status)
    });

    try {
        const portfolios = await App.loadPortfolios();
        accounts = await Promise.all(portfolios.map(portfolio =>
            App.api("/api/portfolios/" + portfolio.portfolioId + "/account")));
        buildPortfolioList();
        accountsLoaded = true;
        render();
    } catch (error) {
        document.getElementById("portfolio-list").replaceChildren(
            h("li", { class: "empty", text: "Portfolios could not be loaded: " + error.message }));
    }
})();
