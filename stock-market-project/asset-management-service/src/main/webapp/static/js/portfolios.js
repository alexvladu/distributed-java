"use strict";

(async () => {
    const { h, format } = App;
    const rowsBody = document.getElementById("portfolio-rows");
    const profileForm = document.getElementById("profile-form");
    const usernameInput = document.getElementById("profile-username");
    const emailInput = document.getElementById("profile-email");

    const user = await App.initLayout();
    if (user) {
        usernameInput.value = user.username;
        emailInput.value = user.email;
    }

    function emptyRow(text) {
        return h("tr", {}, h("td", { class: "empty", colspan: 5, text }));
    }

    async function renamePortfolio(portfolio) {
        const name = await App.promptDialog({
            title: "Rename portfolio",
            label: "Portfolio name",
            value: portfolio.portfolioName,
            confirmLabel: "Rename"
        });
        if (name === null || name === portfolio.portfolioName) {
            return;
        }
        try {
            await App.api("/api/portfolios/" + portfolio.portfolioId, { method: "PUT", body: { portfolioName: name } });
            App.toast("Renamed to " + name + ".", "success");
            await loadDashboard();
        } catch (error) {
            App.toast(error.message, "error");
        }
    }

    async function deletePortfolio(portfolio, tradeCount) {
        const confirmed = await App.confirmDialog({
            title: "Delete " + portfolio.portfolioName + "?",
            message: "Its cash balance and " + tradeCount + (tradeCount === 1 ? " trade" : " trades")
                + " will be permanently removed.",
            confirmLabel: "Delete portfolio",
            danger: true
        });
        if (!confirmed) {
            return;
        }
        try {
            await App.api("/api/portfolios/" + portfolio.portfolioId, { method: "DELETE" });
            App.toast("Deleted " + portfolio.portfolioName + ".", "success");
            await loadDashboard();
        } catch (error) {
            App.toast(error.message, "error");
        }
    }

    function renderPortfolios(dashboard) {
        const portfolios = dashboard.portfolios;
        if (!portfolios.length) {
            rowsBody.replaceChildren(emptyRow("No portfolios yet. Create one above to start trading."));
            return;
        }
        rowsBody.replaceChildren(...portfolios.map(portfolio => {
            const tradeCount = (dashboard.trades[portfolio.portfolioId] || []).length;
            const isActive = portfolio.portfolioId === App.state.activePortfolioId;
            const activate = h("button", {
                type: "button", class: "btn ghost sm", text: isActive ? "Active" : "Make active",
                disabled: isActive, "aria-label": "Make " + portfolio.portfolioName + " the active portfolio"
            });
            const rename = h("button", { type: "button", class: "btn ghost sm", text: "Rename",
                "aria-label": "Rename " + portfolio.portfolioName });
            const remove = h("button", { type: "button", class: "btn danger sm", text: "Delete",
                "aria-label": "Delete " + portfolio.portfolioName });
            activate.addEventListener("click", () => App.setActivePortfolio(portfolio.portfolioId));
            rename.addEventListener("click", () => renamePortfolio(portfolio));
            remove.addEventListener("click", () => deletePortfolio(portfolio, tradeCount));
            return h("tr", {},
                h("td", {}, h("div", { class: "stack" },
                    h("span", { class: "sym", text: portfolio.portfolioName }),
                    isActive ? h("span", { class: "sub", text: "Active portfolio" }) : null)),
                h("td", { class: "num", text: format.money(portfolio.cashBalance) }),
                h("td", { class: "num", text: String(tradeCount) }),
                h("td", { class: "sub", text: format.date(portfolio.createdAt) }),
                h("td", {}, h("div", { class: "row-actions" }, activate, rename, remove)));
        }));
    }

    async function loadDashboard() {
        try {
            const dashboard = await App.api("/dashboard");
            await App.loadPortfolios();
            renderPortfolios(dashboard);
        } catch (error) {
            rowsBody.replaceChildren(emptyRow("Portfolios could not be loaded: " + error.message));
        }
    }

    document.getElementById("create-form").addEventListener("submit", async event => {
        event.preventDefault();
        const input = document.getElementById("new-portfolio-name");
        const name = input.value.trim();
        if (!name) {
            input.setAttribute("aria-invalid", "true");
            input.focus();
            return;
        }
        input.removeAttribute("aria-invalid");
        try {
            const created = await App.api("/api/portfolios", { method: "POST", body: { portfolioName: name } });
            input.value = "";
            App.toast("Created " + created.portfolioName + " with " + format.money(created.cashBalance) + ".",
                "success");
            await loadDashboard();
        } catch (error) {
            App.toast(error.message, "error");
        }
    });

    profileForm.addEventListener("submit", async event => {
        event.preventDefault();
        if (!App.state.user) {
            return;
        }
        let valid = true;
        [usernameInput, emailInput].forEach(input => {
            const ok = input.checkValidity();
            input.setAttribute("aria-invalid", ok ? "false" : "true");
            valid = valid && ok;
        });
        if (!valid) {
            App.toast("Check the highlighted fields.", "error");
            return;
        }
        try {
            const updated = await App.api("/api/users/" + App.state.user.userId, {
                method: "PUT",
                body: { username: usernameInput.value.trim(), email: emailInput.value.trim() }
            });
            App.state.user = updated;
            document.getElementById("current-user").textContent = updated.username;
            App.toast("Profile saved.", "success");
        } catch (error) {
            App.toast(error.message, "error");
        }
    });

    document.getElementById("delete-account").addEventListener("click", async () => {
        if (!App.state.user) {
            return;
        }
        const confirmed = await App.confirmDialog({
            title: "Delete your account?",
            message: "Your profile, every portfolio, and every trade will be permanently deleted.",
            confirmLabel: "Delete everything",
            danger: true
        });
        if (!confirmed) {
            return;
        }
        try {
            await App.api("/api/users/" + App.state.user.userId, { method: "DELETE" });
            App.redirectToLogin();
        } catch (error) {
            App.toast(error.message, "error");
        }
    });

    App.onPortfolioChange(() => loadDashboard());

    await loadDashboard();
})();
