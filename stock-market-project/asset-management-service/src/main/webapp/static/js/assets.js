"use strict";

(async () => {
    const { h, svg, format } = App;
    const quotes = new Map();
    const rows = new Map();
    const tbody = document.getElementById("holdings");
    let account = null;
    let accountRequest = 0;
    let tradesRequest = 0;

    await App.initLayout();

    function setKpi(id, text, className) {
        const element = document.getElementById(id);
        element.textContent = text;
        element.className = "value" + (className ? " " + className : "");
    }

    async function sell(symbol) {
        const parts = rows.get(symbol);
        const execution = await App.placeOrder({
            symbol,
            side: "SELL",
            quantity: parts.input.value,
            quote: quotes.get(symbol),
            input: parts.input,
            buttons: [parts.sell, parts.max]
        });
        if (!execution) {
            return;
        }
        if (execution.account) {
            account = execution.account;
            buildHoldings();
        } else {
            await loadAccount().catch(error => App.toast(error.message, "error"));
        }
        loadTrades();
    }

    function buildHoldings() {
        rows.clear();
        const portfolio = App.activePortfolio();
        document.getElementById("portfolio-name").textContent = portfolio ? portfolio.portfolioName : "Assets";
        if (!account || !account.holdings.length) {
            const message = portfolio
                ? [h("span", { text: "No holdings yet. " }),
                    h("a", { href: App.base + "/live-market", text: "Buy your first stock" }), "."]
                : [h("span", { text: "You have no portfolio yet. " }),
                    h("a", { href: App.base + "/portfolios", text: "Create one" }), "."];
            tbody.replaceChildren(h("tr", {}, h("td", { class: "empty", colspan: 8 }, message)));
            renderValues();
            return;
        }
        tbody.replaceChildren(...account.holdings.map(holding => {
            const symbol = holding.symbol;
            const input = h("input", {
                class: "input", type: "number", min: "0.000001", max: holding.quantity, step: "any",
                value: holding.quantity, inputmode: "decimal", "aria-label": "Quantity of " + symbol + " to sell"
            });
            const parts = {
                name: h("span", { class: "sub", text: holding.name }),
                price: h("td", { class: "num" }),
                value: h("td", { class: "num sym" }),
                profit: h("span", { class: "change" }),
                weight: h("td", { class: "num" }),
                input,
                max: h("button", { type: "button", class: "btn ghost sm", text: "Max",
                    "aria-label": "Sell all " + symbol }),
                sell: h("button", { type: "button", class: "btn sell sm", text: "Sell", "aria-label": "Sell " + symbol })
            };
            parts.row = h("tr", {},
                h("td", {}, h("div", { class: "stack" }, h("span", { class: "sym", text: symbol }), parts.name)),
                h("td", { class: "num", text: format.quantity(holding.quantity) }),
                h("td", { class: "num", text: format.money(holding.averageCost) }),
                parts.price,
                parts.value,
                h("td", { class: "num" }, parts.profit),
                parts.weight,
                h("td", {}, h("div", { class: "trade-cell" }, input, parts.max, parts.sell)));
            input.addEventListener("input", () => input.removeAttribute("aria-invalid"));
            parts.max.addEventListener("click", () => {
                input.value = holding.quantity;
                input.focus();
            });
            parts.sell.addEventListener("click", () => sell(symbol));
            rows.set(symbol, parts);
            return parts.row;
        }));
        renderValues();
    }

    function renderAllocation(valued) {
        const container = document.getElementById("allocation");
        const legend = document.getElementById("allocation-legend");
        const total = valued ? valued.totalValue : 0;
        if (!valued || !(total > 0)) {
            container.replaceChildren(h("p", { class: "sub", text: "Nothing to show yet." }));
            legend.replaceChildren();
            return;
        }
        const segments = valued.holdings
            .filter(holding => holding.marketValue > 0)
            .sort((a, b) => b.marketValue - a.marketValue)
            .map((holding, index) => ({ label: holding.symbol, value: holding.marketValue, className: "seg-" + (index % 8) }));
        if (valued.cashBalance > 0) {
            segments.push({ label: "Cash", value: valued.cashBalance, className: "seg-cash" });
        }
        const describe = segment => segment.label + " " + format.percent(segment.value / total);
        const bar = svg("svg", {
            class: "alloc", viewBox: "0 0 100 10", preserveAspectRatio: "none", role: "img",
            "aria-label": "Allocation: " + segments.map(describe).join(", ")
        });
        let offset = 0;
        segments.forEach(segment => {
            const width = (segment.value / total) * 100;
            bar.append(svg("rect", { x: offset.toFixed(3), y: 0, width: width.toFixed(3), height: 10,
                class: segment.className }, svg("title", { text: describe(segment) })));
            offset += width;
        });
        container.replaceChildren(bar);
        legend.replaceChildren(...segments.map(segment => h("li", {},
            svg("svg", { width: 10, height: 10, viewBox: "0 0 10 10", "aria-hidden": "true", focusable: "false" },
                svg("circle", { cx: 5, cy: 5, r: 5, class: segment.className })),
            describe(segment) + " \u00B7 " + format.money(segment.value))));
    }

    function renderValues() {
        const valued = App.revalue(account, quotes);
        if (!valued) {
            ["kpi-total", "kpi-cash", "kpi-invested", "kpi-profit"].forEach(id => setKpi(id, "\u2013"));
            document.getElementById("kpi-cost").textContent = "";
            renderAllocation(null);
            return;
        }
        const profit = App.profit(valued.unrealizedProfit, valued.costBasis);
        setKpi("kpi-total", format.money(valued.totalValue));
        setKpi("kpi-cash", format.money(valued.cashBalance));
        setKpi("kpi-invested", format.money(valued.investedValue));
        setKpi("kpi-profit", profit.text, profit.className);
        document.getElementById("kpi-cost").textContent = "Cost basis " + format.money(valued.costBasis);
        valued.holdings.forEach(holding => {
            const parts = rows.get(holding.symbol);
            if (!parts) {
                return;
            }
            const result = App.profit(holding.unrealizedProfit, holding.costBasis);
            parts.name.textContent = holding.name;
            parts.price.textContent = format.money(holding.price);
            parts.value.textContent = format.money(holding.marketValue);
            parts.profit.textContent = result.text;
            parts.profit.className = "change " + result.className;
            parts.weight.textContent = valued.totalValue > 0 && holding.marketValue !== null
                ? format.percent(holding.marketValue / valued.totalValue)
                : "\u2013";
        });
        renderAllocation(valued);
    }

    async function loadAccount() {
        const request = ++accountRequest;
        const portfolio = App.activePortfolio();
        const loaded = portfolio ? await App.api("/api/portfolios/" + portfolio.portfolioId + "/account") : null;
        if (request === accountRequest) {
            account = loaded;
            buildHoldings();
        }
    }

    async function refreshAfterLedgerChange() {
        await loadAccount().catch(error => App.toast(error.message, "error"));
        await loadTrades();
    }

    async function correctTrade(trade) {
        const value = await App.promptDialog({
            title: "Correct " + trade.side.toLowerCase() + " of " + trade.symbol,
            label: "Quantity (cash is settled at " + format.money(trade.price) + " per share)",
            value: String(trade.quantity),
            confirmLabel: "Save correction",
            maxLength: 24
        });
        if (value === null) {
            return;
        }
        if (!(Number(value) > 0)) {
            App.toast("Enter a quantity greater than zero.", "error");
            return;
        }
        try {
            await App.api("/api/trades/" + trade.tradeId, { method: "PUT", body: { quantity: value } });
            App.toast("Trade corrected.", "success");
            await refreshAfterLedgerChange();
        } catch (error) {
            App.toast(error.message, "error");
        }
    }

    async function voidTrade(trade) {
        const confirmed = await App.confirmDialog({
            title: "Void this trade?",
            message: "The trade is removed from the Oracle ledger and its cash effect is reversed.",
            details: [
                ["Trade", trade.side + " " + format.quantity(trade.quantity) + " " + trade.symbol],
                ["Executed", format.dateTime(trade.executedAt)]
            ],
            confirmLabel: "Void trade",
            danger: true
        });
        if (!confirmed) {
            return;
        }
        try {
            await App.api("/api/trades/" + trade.tradeId, { method: "DELETE" });
            App.toast("Trade voided.", "success");
            await refreshAfterLedgerChange();
        } catch (error) {
            App.toast(error.message, "error");
        }
    }

    function tradeActions(trade) {
        return [
            h("button", { type: "button", class: "btn ghost sm", text: "Correct",
                "aria-label": "Correct trade " + trade.tradeId, onclick: () => correctTrade(trade) }),
            h("button", { type: "button", class: "btn danger sm", text: "Void",
                "aria-label": "Void trade " + trade.tradeId, onclick: () => voidTrade(trade) })
        ];
    }

    async function loadTrades() {
        const request = ++tradesRequest;
        const target = document.getElementById("trades");
        const portfolio = App.activePortfolio();
        if (!portfolio) {
            App.renderTrades(target, [], "No portfolio selected.", tradeActions);
            return;
        }
        try {
            const trades = await App.api("/api/portfolios/" + portfolio.portfolioId + "/trades");
            if (request === tradesRequest) {
                App.renderTrades(target, trades, "No trades yet.", tradeActions);
            }
        } catch (error) {
            if (request === tradesRequest) {
                App.renderTrades(target, [], "Trades could not be loaded: " + error.message, tradeActions);
            }
        }
    }

    App.connectMarket({
        onSnapshot: list => {
            quotes.clear();
            list.forEach(quote => quotes.set(quote.symbol, quote));
            renderValues();
        },
        onQuote: quote => {
            quotes.set(quote.symbol, quote);
            renderValues();
            const parts = rows.get(quote.symbol);
            if (parts) {
                App.flash(parts.price, App.change(quote.price, quote.previousPrice).className);
            }
        },
        onStatus: status => App.renderStreamStatus(document.getElementById("stream-status"), status)
    });

    App.onPortfolioChange(() => {
        loadAccount().catch(error => App.toast(error.message, "error"));
        loadTrades();
    });

    try {
        await App.loadPortfolios();
        await loadAccount();
    } catch (error) {
        App.toast(error.message, "error");
    }
    loadTrades();
})();
