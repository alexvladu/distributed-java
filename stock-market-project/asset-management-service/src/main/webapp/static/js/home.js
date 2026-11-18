"use strict";

(async () => {
    const { h, format } = App;

    const user = await App.initLayout();
    if (user) {
        document.getElementById("greeting").textContent = "Welcome back, " + user.username + ".";
    }

    function setKpi(id, text, className) {
        const element = document.getElementById(id);
        element.textContent = text;
        element.className = "value" + (className ? " " + className : "");
    }

    function renderTotals(accounts) {
        const sum = key => accounts.reduce((total, account) => total + Number(account[key] || 0), 0);
        const profit = App.profit(sum("unrealizedProfit"), sum("costBasis"));
        setKpi("kpi-total", format.money(sum("totalValue")));
        setKpi("kpi-cash", format.money(sum("cashBalance")));
        setKpi("kpi-invested", format.money(sum("investedValue")));
        setKpi("kpi-profit", profit.text, profit.className);
        document.getElementById("kpi-count").textContent =
            accounts.length === 1 ? "Across 1 portfolio" : "Across " + accounts.length + " portfolios";
    }

    function renderPortfolioList(accounts) {
        const list = document.getElementById("portfolio-list");
        if (!accounts.length) {
            list.replaceChildren(h("li", { class: "empty" }, "No portfolios yet. ",
                h("a", { href: App.base + "/portfolios", text: "Create one" }), "."));
            return;
        }
        list.replaceChildren(...accounts.map(account => {
            const profit = App.profit(account.unrealizedProfit, account.costBasis);
            const holdings = account.holdings.length === 1 ? "1 holding" : account.holdings.length + " holdings";
            return h("li", {},
                h("div", { class: "stack" },
                    h("a", {
                        href: App.base + "/assets",
                        class: "sym",
                        text: account.portfolioName,
                        onclick: () => App.setActivePortfolio(account.portfolioId)
                    }),
                    h("span", { class: "sub", text: "Cash " + format.money(account.cashBalance) + " \u00B7 " + holdings })),
                h("span", { class: "change " + profit.className, text: profit.text }),
                h("span", { class: "num sym", text: format.money(account.totalValue) }));
        }));
    }

    function renderMarket(quotes) {
        const container = document.getElementById("stocks");
        if (!quotes.length) {
            container.replaceChildren(h("li", { class: "empty", text: "No stocks are available." }));
            return;
        }
        container.replaceChildren(...quotes.map(quote => h("li", {},
            h("div", { class: "stack" }, h("span", { class: "sym", text: quote.symbol }),
                h("span", { class: "sub", text: quote.name })),
            h("span", { class: "num sym", text: format.money(quote.price) }))));
    }

    try {
        renderMarket(await App.api("/api/market"));
    } catch (error) {
        document.getElementById("stocks").replaceChildren(
            h("li", { class: "empty", text: "Stocks could not be loaded: " + error.message }));
    }

    try {
        const portfolios = await App.loadPortfolios();
        const accounts = await Promise.all(portfolios.map(portfolio =>
            App.api("/api/portfolios/" + portfolio.portfolioId + "/account")));
        renderPortfolioList(accounts);
        renderTotals(accounts);
    } catch (error) {
        document.getElementById("portfolio-list").replaceChildren(
            h("li", { class: "empty", text: "Portfolios could not be loaded: " + error.message }));
    }
})();
