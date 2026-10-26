"use strict";

/**
 * Shared front-end helpers: API calls (with CSRF header), DOM building without innerHTML, formatting,
 * toasts, dialogs, the market event stream, and the active-portfolio selector in the header.
 */
const App = (() => {
    const base = document.body.dataset.base !== undefined
        ? document.body.dataset.base
        : window.location.pathname.replace(/\/[^/]*$/, "");
    const PORTFOLIO_KEY = "stockPortfolio.activePortfolioId";
    const SVG_NS = "http://www.w3.org/2000/svg";

    const moneyFormat = new Intl.NumberFormat("en-US", { style: "currency", currency: "USD" });
    const signedMoneyFormat = new Intl.NumberFormat("en-US",
        { style: "currency", currency: "USD", signDisplay: "exceptZero" });
    const quantityFormat = new Intl.NumberFormat("en-US", { maximumFractionDigits: 6 });
    const percentFormat = new Intl.NumberFormat("en-US",
        { style: "percent", minimumFractionDigits: 2, maximumFractionDigits: 2 });
    const signedPercentFormat = new Intl.NumberFormat("en-US",
        { style: "percent", minimumFractionDigits: 2, maximumFractionDigits: 2, signDisplay: "exceptZero" });
    const timeFormat = new Intl.DateTimeFormat(undefined, { hour: "2-digit", minute: "2-digit", second: "2-digit" });
    const dateTimeFormat = new Intl.DateTimeFormat(undefined, { dateStyle: "medium", timeStyle: "short" });
    const dateFormat = new Intl.DateTimeFormat(undefined, { dateStyle: "medium" });

    const isMissing = value => value === null || value === undefined || value === "";
    // Java emits up to nanosecond precision; trim to milliseconds so every browser parses it.
    const toDate = value => new Date(String(value).replace(/(\.\d{3})\d+/, "$1"));
    const formatDate = (formatter, value) => {
        if (isMissing(value)) {
            return "\u2013";
        }
        const date = toDate(value);
        return Number.isNaN(date.getTime()) ? "\u2013" : formatter.format(date);
    };
    const format = {
        money: value => isMissing(value) ? "\u2013" : moneyFormat.format(Number(value)),
        signedMoney: value => isMissing(value) ? "\u2013" : signedMoneyFormat.format(Number(value)),
        quantity: value => isMissing(value) ? "\u2013" : quantityFormat.format(Number(value)),
        percent: value => isMissing(value) || !Number.isFinite(Number(value))
            ? "\u2013" : percentFormat.format(Number(value)),
        signedPercent: value => isMissing(value) || !Number.isFinite(Number(value))
            ? "\u2013" : signedPercentFormat.format(Number(value)),
        time: value => formatDate(timeFormat, value),
        dateTime: value => formatDate(dateTimeFormat, value),
        date: value => formatDate(dateFormat, value)
    };

    class ApiError extends Error {
        constructor(message, status) {
            super(message);
            this.status = status;
        }
    }

    function redirectToLogin() {
        window.location.assign(base + "/auth.html");
    }

    async function api(path, { method = "GET", body, redirectOn401 = true } = {}) {
        const headers = { "Accept": "application/json", "X-Requested-With": "XMLHttpRequest" };
        if (body !== undefined) {
            headers["Content-Type"] = "application/json";
        }
        let response;
        try {
            response = await fetch(base + path, {
                method,
                headers,
                body: body === undefined ? undefined : JSON.stringify(body),
                credentials: "same-origin",
                cache: "no-store"
            });
        } catch (error) {
            throw new ApiError("Cannot reach the server. Check that the service is running.", 0);
        }
        if (response.status === 401 && redirectOn401) {
            redirectToLogin();
            throw new ApiError("Your session has expired. Please log in again.", 401);
        }
        if (response.status === 204) {
            return null;
        }
        const text = await response.text();
        let data = null;
        if (text) {
            try {
                data = JSON.parse(text);
            } catch (error) {
                data = null;
            }
        }
        if (!response.ok) {
            throw new ApiError((data && data.error) || "Request failed (" + response.status + ")", response.status);
        }
        return data;
    }

    function setAttributes(element, attributes) {
        for (const [key, value] of Object.entries(attributes || {})) {
            if (value === null || value === undefined || value === false) {
                continue;
            }
            if (key === "text") {
                element.textContent = String(value);
            } else if (key.startsWith("on") && typeof value === "function") {
                element.addEventListener(key.slice(2), value);
            } else {
                element.setAttribute(key, value === true ? "" : String(value));
            }
        }
    }

    function appendChildren(element, children) {
        for (const child of children.flat(Infinity)) {
            if (child === null || child === undefined || child === false) {
                continue;
            }
            element.append(child instanceof Node ? child : document.createTextNode(String(child)));
        }
    }

    /** Builds an HTML element; text is always inserted as text, never parsed as HTML. */
    function h(tag, attributes, ...children) {
        const element = document.createElement(tag);
        setAttributes(element, attributes);
        appendChildren(element, children);
        return element;
    }

    function svg(tag, attributes, ...children) {
        const element = document.createElementNS(SVG_NS, tag);
        setAttributes(element, attributes);
        appendChildren(element, children);
        return element;
    }

    function trendClass(current, previous) {
        if (isMissing(previous) || isMissing(current)) {
            return "flat";
        }
        const diff = Number(current) - Number(previous);
        return diff > 0 ? "up" : diff < 0 ? "down" : "flat";
    }

    function sparkline(values, width = 96, height = 28) {
        const element = svg("svg", { class: "spark", viewBox: "0 0 " + width + " " + height,
            "aria-hidden": "true", focusable: "false" });
        const numbers = (values || []).map(Number).filter(Number.isFinite);
        if (numbers.length < 2) {
            element.classList.add("flat");
            return element;
        }
        const min = Math.min(...numbers);
        const max = Math.max(...numbers);
        const range = max - min || 1;
        const step = width / (numbers.length - 1);
        const points = numbers.map((value, index) => {
            const x = (index * step).toFixed(1);
            const y = (height - 2 - ((value - min) / range) * (height - 4)).toFixed(1);
            return x + "," + y;
        }).join(" ");
        element.append(svg("polyline", { points }));
        element.classList.add(trendClass(numbers[numbers.length - 1], numbers[0]));
        return element;
    }

    /** Text and CSS class describing the move from the previous price. */
    function change(price, previousPrice) {
        if (isMissing(previousPrice) || Number(previousPrice) === 0) {
            return { text: "\u2013", className: "flat" };
        }
        const diff = Number(price) - Number(previousPrice);
        const arrow = diff > 0 ? "\u25B2" : diff < 0 ? "\u25BC" : "\u2022";
        return {
            text: arrow + " " + format.signedMoney(diff) + " (" + format.signedPercent(diff / Number(previousPrice)) + ")",
            className: trendClass(price, previousPrice)
        };
    }

    /** Profit text with percentage of cost basis, plus a CSS class. */
    function profit(value, costBasis) {
        if (isMissing(value)) {
            return { text: "\u2013", className: "flat" };
        }
        const ratio = Number(costBasis) > 0 ? Number(value) / Number(costBasis) : null;
        const suffix = ratio === null ? "" : " (" + format.signedPercent(ratio) + ")";
        return { text: format.signedMoney(value) + suffix, className: trendClass(value, 0) };
    }

    function flash(element, direction) {
        if (direction !== "up" && direction !== "down") {
            return;
        }
        element.classList.remove("flash-up", "flash-down");
        // Force a reflow so the animation restarts on consecutive updates.
        void element.offsetWidth;
        element.classList.add(direction === "up" ? "flash-up" : "flash-down");
    }

    function toast(message, type = "info") {
        let region = document.getElementById("toast-region");
        if (!region) {
            region = h("div", { id: "toast-region", class: "toast-region", role: "status", "aria-live": "polite" });
            document.body.append(region);
        }
        const item = h("div", { class: "toast " + type, text: message });
        region.append(item);
        window.setTimeout(() => item.remove(), type === "error" ? 7000 : 4000);
    }

    function confirmDialog({ title, message, details = [], confirmLabel = "Confirm", danger = false }) {
        return new Promise(resolve => {
            const dialog = h("dialog", { "aria-labelledby": "dialog-title" },
                h("h2", { id: "dialog-title", text: title }),
                message ? h("p", { text: message }) : null,
                details.length
                    ? h("dl", { class: "summary-list" },
                        details.map(([label, value]) => [h("dt", { text: label }), h("dd", { text: value })]))
                    : null,
                h("div", { class: "actions" },
                    h("button", { type: "button", class: "btn ghost", "data-action": "cancel", text: "Cancel" }),
                    h("button", { type: "button", class: danger ? "btn danger solid" : "btn",
                        "data-action": "confirm", text: confirmLabel })));
            dialog.addEventListener("click", event => {
                const button = event.target.closest("[data-action]");
                if (button) {
                    dialog.close(button.dataset.action);
                }
            });
            dialog.addEventListener("close", () => {
                resolve(dialog.returnValue === "confirm");
                dialog.remove();
            });
            document.body.append(dialog);
            dialog.showModal();
        });
    }

    function promptDialog({ title, label, value = "", confirmLabel = "Save", maxLength = 120 }) {
        return new Promise(resolve => {
            let result = null;
            const input = h("input", { id: "dialog-input", class: "input", value, maxlength: maxLength,
                required: true, autocomplete: "off" });
            const form = h("form", { class: "stack-form", novalidate: true },
                h("div", { class: "field" }, h("label", { for: "dialog-input", text: label }), input),
                h("div", { class: "actions" },
                    h("button", { type: "button", class: "btn ghost", "data-action": "cancel", text: "Cancel" }),
                    h("button", { type: "submit", class: "btn", text: confirmLabel })));
            const dialog = h("dialog", { "aria-labelledby": "dialog-title" },
                h("h2", { id: "dialog-title", text: title }), form);
            form.addEventListener("submit", event => {
                event.preventDefault();
                const trimmed = input.value.trim();
                if (!trimmed) {
                    input.setAttribute("aria-invalid", "true");
                    input.focus();
                    return;
                }
                result = trimmed;
                dialog.close();
            });
            dialog.addEventListener("click", event => {
                if (event.target.closest("[data-action='cancel']")) {
                    dialog.close();
                }
            });
            dialog.addEventListener("close", () => {
                resolve(result);
                dialog.remove();
            });
            document.body.append(dialog);
            dialog.showModal();
            input.select();
        });
    }

    /** Opens the market Server-Sent Events stream; EventSource reconnects by itself after network errors. */
    function connectMarket({ onSnapshot, onQuote, onStatus }) {
        let source = null;
        let stopped = false;
        const status = value => onStatus && onStatus(value);

        function open() {
            source = new EventSource(base + "/api/market/stream");
            status("connecting");
            source.addEventListener("open", () => status("live"));
            source.addEventListener("snapshot", event => onSnapshot && onSnapshot(JSON.parse(event.data)));
            source.addEventListener("quote", event => onQuote && onQuote(JSON.parse(event.data)));
            source.addEventListener("error", async () => {
                if (stopped) {
                    return;
                }
                status("reconnecting");
                if (source.readyState === EventSource.CLOSED) {
                    // The server refused the stream (for example an expired session); check before retrying.
                    try {
                        await api("/api/session");
                    } catch (error) {
                        status("offline");
                        return;
                    }
                    window.setTimeout(() => { if (!stopped) open(); }, 5000);
                }
            });
        }

        open();
        window.addEventListener("pagehide", () => {
            stopped = true;
            source.close();
        });
        return { close() { stopped = true; source.close(); } };
    }

    function renderStreamStatus(element, value) {
        if (!element) {
            return;
        }
        const states = {
            connecting: ["warn", "Connecting"],
            live: ["live", "Live"],
            reconnecting: ["warn", "Reconnecting"],
            offline: ["error", "Offline"]
        };
        const [className, label] = states[value] || states.connecting;
        element.className = "pill " + className;
        element.textContent = label;
    }

    /** Recomputes an account snapshot with the latest quotes so values move with the market. */
    function revalue(account, quotes) {
        if (!account) {
            return null;
        }
        let invested = 0;
        let cost = 0;
        const holdings = account.holdings.map(holding => {
            const quote = quotes.get(holding.symbol);
            const price = quote ? Number(quote.price) : (isMissing(holding.price) ? null : Number(holding.price));
            const marketValue = price === null ? null : price * Number(holding.quantity);
            const unrealizedProfit = marketValue === null ? null : marketValue - Number(holding.costBasis);
            if (marketValue !== null) {
                invested += marketValue;
                cost += Number(holding.costBasis);
            }
            return Object.assign({}, holding, {
                name: quote ? quote.name : holding.name,
                price,
                marketValue,
                unrealizedProfit
            });
        });
        const cash = Number(account.cashBalance);
        return Object.assign({}, account, {
            holdings,
            cashBalance: cash,
            investedValue: invested,
            costBasis: cost,
            unrealizedProfit: invested - cost,
            totalValue: cash + invested
        });
    }

    function renderTrades(tbody, trades, emptyText, actionsFor) {
        const columns = actionsFor ? 7 : 6;
        if (!trades.length) {
            tbody.replaceChildren(h("tr", {}, h("td", { class: "empty", colspan: columns, text: emptyText })));
            return;
        }
        tbody.replaceChildren(...trades.map(trade => {
            const amount = isMissing(trade.price) ? null : Number(trade.price) * Number(trade.quantity);
            return h("tr", {},
                h("td", { class: "sub", text: format.dateTime(trade.executedAt) }),
                h("td", {}, h("span", { class: "badge " + String(trade.side).toLowerCase(), text: trade.side })),
                h("td", { class: "sym", text: trade.symbol }),
                h("td", { class: "num", text: format.quantity(trade.quantity) }),
                h("td", { class: "num", text: format.money(trade.price) }),
                h("td", { class: "num", text: format.money(amount) }),
                actionsFor ? h("td", {}, h("div", { class: "row-actions" }, actionsFor(trade))) : null);
        }));
    }

    /** Confirms and sends a market order for the active portfolio; resolves to the execution or null. */
    async function placeOrder({ symbol, side, quantity, quote, input, buttons = [] }) {
        const portfolio = activePortfolio();
        if (!portfolio) {
            toast("Create a portfolio on the Portfolios page before trading.", "error");
            return null;
        }
        const amount = Number(quantity);
        if (!(amount > 0)) {
            if (input) {
                input.setAttribute("aria-invalid", "true");
                input.focus();
            }
            toast("Enter a quantity greater than zero.", "error");
            return null;
        }
        const buying = side === "BUY";
        const confirmed = await confirmDialog({
            title: (buying ? "Buy " : "Sell ") + symbol,
            message: "Market orders execute at the latest price when the server receives them.",
            details: [
                ["Portfolio", portfolio.portfolioName],
                ["Quantity", format.quantity(amount)],
                ["Last price", format.money(quote ? quote.price : null)],
                [buying ? "Estimated cost" : "Estimated proceeds",
                    format.money(quote ? Number(quote.price) * amount : null)]
            ],
            confirmLabel: buying ? "Buy" : "Sell",
            danger: !buying
        });
        if (!confirmed) {
            return null;
        }
        buttons.forEach(button => { button.disabled = true; });
        try {
            const execution = await api("/api/portfolios/" + portfolio.portfolioId + "/trades",
                { method: "POST", body: { symbol, side, quantity: String(quantity).trim() } });
            const trade = execution.trade;
            toast((buying ? "Bought " : "Sold ") + format.quantity(trade.quantity) + " " + trade.symbol
                + " at " + format.money(trade.price) + ".", "success");
            return execution;
        } catch (error) {
            toast(error.message, "error");
            return null;
        } finally {
            buttons.forEach(button => { button.disabled = false; });
        }
    }

    // Header: current user, logout, navigation state, and the active portfolio selector.

    const state = { user: null, portfolios: [], activePortfolioId: null };
    const portfolioListeners = new Set();

    function readStoredPortfolioId() {
        try {
            return Number(window.localStorage.getItem(PORTFOLIO_KEY));
        } catch (error) {
            return NaN;
        }
    }

    function storePortfolioId(id) {
        try {
            window.localStorage.setItem(PORTFOLIO_KEY, String(id));
        } catch (error) {
            // Storage can be unavailable in private browsing; the selection then lasts for this page only.
        }
    }

    function activePortfolio() {
        return state.portfolios.find(portfolio => portfolio.portfolioId === state.activePortfolioId) || null;
    }

    function renderPortfolioSelect() {
        const select = document.getElementById("portfolio-select");
        if (!select) {
            return;
        }
        if (!state.portfolios.length) {
            select.replaceChildren(h("option", { value: "", text: "No portfolio" }));
            select.disabled = true;
            return;
        }
        select.replaceChildren(...state.portfolios.map(portfolio => h("option", {
            value: portfolio.portfolioId,
            text: portfolio.portfolioName,
            selected: portfolio.portfolioId === state.activePortfolioId
        })));
        select.disabled = false;
    }

    async function loadPortfolios() {
        state.portfolios = await api("/api/portfolios");
        const stored = readStoredPortfolioId();
        const active = state.portfolios.find(portfolio => portfolio.portfolioId === stored) || state.portfolios[0];
        state.activePortfolioId = active ? active.portfolioId : null;
        renderPortfolioSelect();
        return state.portfolios;
    }

    function setActivePortfolio(id) {
        state.activePortfolioId = id;
        storePortfolioId(id);
        renderPortfolioSelect();
        const active = activePortfolio();
        portfolioListeners.forEach(listener => listener(active));
    }

    function onPortfolioChange(listener) {
        portfolioListeners.add(listener);
    }

    async function logout() {
        try {
            await api("/auth/logout", { method: "POST", redirectOn401: false });
        } finally {
            redirectToLogin();
        }
    }

    async function initLayout() {
        const path = window.location.pathname.slice(base.length) || "/";
        document.querySelectorAll(".nav a[data-path]").forEach(link => {
            if (link.dataset.path === path) {
                link.setAttribute("aria-current", "page");
            }
        });
        const logoutButton = document.getElementById("logout");
        if (logoutButton) {
            logoutButton.addEventListener("click", logout);
        }
        const select = document.getElementById("portfolio-select");
        if (select) {
            select.addEventListener("change", () => setActivePortfolio(Number(select.value)));
        }
        try {
            state.user = await api("/api/session");
            const userElement = document.getElementById("current-user");
            if (userElement) {
                userElement.textContent = state.user.username;
            }
        } catch (error) {
            if (error.status !== 401) {
                toast(error.message, "error");
            }
        }
        return state.user;
    }

    return {
        base,
        api,
        ApiError,
        h,
        svg,
        format,
        sparkline,
        change,
        profit,
        trendClass,
        flash,
        toast,
        confirmDialog,
        promptDialog,
        connectMarket,
        renderStreamStatus,
        revalue,
        renderTrades,
        placeOrder,
        state,
        activePortfolio,
        loadPortfolios,
        setActivePortfolio,
        onPortfolioChange,
        initLayout,
        redirectToLogin
    };
})();
