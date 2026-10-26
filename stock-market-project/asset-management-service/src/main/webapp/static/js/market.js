"use strict";

(async () => {
    const { h, format } = App;
    const quotes = new Map();
    const rows = new Map();
    const tbody = document.getElementById("quotes");
    const search = document.getElementById("search");
    const noMatch = h("tr", { hidden: true }, h("td", { class: "empty", colspan: 7, text: "No stocks match your search." }));
    let account = null;
    let accountRequest = 0;
    let tradesRequest = 0;

    await App.initLayout();

    function setKpi(id, text, className) {
        const element = document.getElementById(id);
        element.textContent = text;
        element.className = "value" + (className ? " " + className : "");
    }

    function ownedQuantity(symbol) {
        const holding = account ? account.holdings.find(item => item.symbol === symbol) : null;
        return holding ? Number(holding.quantity) : 0;
    }

    function updateOwned(symbol) {
        const parts = rows.get(symbol);
        const owned = ownedQuantity(symbol);
        parts.owned.textContent = owned > 0 ? format.quantity(owned) : "\u2013";
        parts.sell.disabled = owned <= 0;
    }

    function renderAccount() {
        const portfolio = App.activePortfolio();
        document.getElementById("portfolio-name").textContent = portfolio
            ? portfolio.portfolioName
            : "a portfolio (create one on the Portfolios page)";
        const valued = App.revalue(account, quotes);
        if (!valued) {
            ["kpi-total", "kpi-cash", "kpi-invested", "kpi-profit"].forEach(id => setKpi(id, "\u2013"));
        } else {
            const profit = App.profit(valued.unrealizedProfit, valued.costBasis);
            setKpi("kpi-total", format.money(valued.totalValue));
            setKpi("kpi-cash", format.money(valued.cashBalance));
            setKpi("kpi-invested", format.money(valued.investedValue));
            setKpi("kpi-profit", profit.text, profit.className);
        }
        rows.forEach((parts, symbol) => updateOwned(symbol));
    }

    function updateEstimate(symbol) {
        const parts = rows.get(symbol);
        const quote = quotes.get(symbol);
        const quantity = Number(parts.input.value);
        parts.estimate.textContent = quote && quantity > 0
            ? "\u2248 " + format.money(Number(quote.price) * quantity)
            : "";
    }

    async function order(symbol, side) {
        const parts = rows.get(symbol);
        const execution = await App.placeOrder({
            symbol,
            side,
            quantity: parts.input.value,
            quote: quotes.get(symbol),
            input: parts.input,
            buttons: [parts.buy, parts.sell]
        });
        if (execution) {
            if (execution.account) {
                account = execution.account;
            } else {
                await loadAccount().catch(error => App.toast(error.message, "error"));
            }
            loadTrades();
        }
        renderAccount();
    }

    function buildRow(quote) {
        const symbol = quote.symbol;
        const input = h("input", {
            class: "input", type: "number", min: "0.000001", step: "any", value: "1", inputmode: "decimal",
            "aria-label": "Quantity of " + symbol
        });
        const parts = {
            name: h("span", { class: "sub" }),
            spark: h("td", {}),
            price: h("td", { class: "num sym" }),
            change: h("span", { class: "change" }),
            updated: h("td", { class: "num sub" }),
            owned: h("td", { class: "num" }),
            estimate: h("span", { class: "estimate", "aria-live": "off" }),
            input,
            buy: h("button", { type: "button", class: "btn buy sm", text: "Buy", "aria-label": "Buy " + symbol }),
            sell: h("button", { type: "button", class: "btn sell sm", text: "Sell", "aria-label": "Sell " + symbol })
        };
        parts.row = h("tr", { "data-symbol": symbol },
            h("td", {}, h("div", { class: "stack" }, h("span", { class: "sym", text: symbol }), parts.name)),
            parts.spark,
            parts.price,
            h("td", { class: "num" }, parts.change),
            parts.updated,
            parts.owned,
            h("td", {}, h("div", { class: "trade-cell" }, parts.estimate, input, parts.buy, parts.sell)));
        input.addEventListener("input", () => {
            input.removeAttribute("aria-invalid");
            updateEstimate(symbol);
        });
        input.addEventListener("keydown", event => {
            if (event.key === "Enter") {
                order(symbol, "BUY");
            }
        });
        parts.buy.addEventListener("click", () => order(symbol, "BUY"));
        parts.sell.addEventListener("click", () => order(symbol, "SELL"));
        rows.set(symbol, parts);
        return parts.row;
    }

    function updateRow(quote, animate) {
        const parts = rows.get(quote.symbol);
        const move = App.change(quote.price, quote.previousPrice);
        parts.name.textContent = quote.name;
        parts.spark.replaceChildren(App.sparkline(quote.history));
        parts.price.textContent = format.money(quote.price);
        parts.change.textContent = move.text;
        parts.change.className = "change " + move.className;
        parts.updated.textContent = format.time(quote.timestamp);
        updateEstimate(quote.symbol);
        updateOwned(quote.symbol);
        if (animate) {
            App.flash(parts.price, move.className);
        }
    }

    function applyFilter() {
        const query = search.value.trim().toLowerCase();
        let visible = 0;
        rows.forEach((parts, symbol) => {
            const quote = quotes.get(symbol);
            const matches = !query || symbol.toLowerCase().includes(query)
                || String(quote.name).toLowerCase().includes(query);
            parts.row.hidden = !matches;
            if (matches) {
                visible++;
            }
        });
        noMatch.hidden = visible > 0 || rows.size === 0;
    }

    /** Rebuilds row order while reusing existing rows so typed quantities and focus survive. */
    function renderTable() {
        for (const symbol of [...rows.keys()]) {
            if (!quotes.has(symbol)) {
                rows.delete(symbol);
            }
        }
        const sorted = [...quotes.values()].sort((a, b) => a.symbol.localeCompare(b.symbol));
        if (!sorted.length) {
            tbody.replaceChildren(h("tr", {}, h("td", { class: "empty", colspan: 7,
                text: "Waiting for the first prices\u2026" })));
            return;
        }
        tbody.replaceChildren(...sorted.map(quote => rows.has(quote.symbol) ? rows.get(quote.symbol).row : buildRow(quote)),
            noMatch);
        sorted.forEach(quote => updateRow(quote, false));
        applyFilter();
    }

    async function loadAccount() {
        const request = ++accountRequest;
        const portfolio = App.activePortfolio();
        const loaded = portfolio ? await App.api("/api/portfolios/" + portfolio.portfolioId + "/account") : null;
        if (request === accountRequest) {
            account = loaded;
            renderAccount();
        }
    }

    async function loadTrades() {
        const request = ++tradesRequest;
        const target = document.getElementById("recent-trades");
        const portfolio = App.activePortfolio();
        if (!portfolio) {
            App.renderTrades(target, [], "Create a portfolio to start trading.");
            return;
        }
        try {
            const trades = await App.api("/api/portfolios/" + portfolio.portfolioId + "/trades");
            if (request === tradesRequest) {
                App.renderTrades(target, trades.slice(0, 8), "No trades yet. Buy your first stock above.");
            }
        } catch (error) {
            if (request === tradesRequest) {
                App.renderTrades(target, [], "Trades could not be loaded: " + error.message);
            }
        }
    }

    search.addEventListener("input", applyFilter);

    App.connectMarket({
        onSnapshot: list => {
            quotes.clear();
            list.forEach(quote => quotes.set(quote.symbol, quote));
            renderTable();
            renderAccount();
        },
        onQuote: quote => {
            const isNew = !quotes.has(quote.symbol);
            quotes.set(quote.symbol, quote);
            if (isNew) {
                renderTable();
            } else {
                updateRow(quote, true);
            }
            renderAccount();
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
