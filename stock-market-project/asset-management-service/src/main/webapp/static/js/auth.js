"use strict";

(() => {
    const message = document.getElementById("form-message");
    const tabs = {
        login: [document.getElementById("tab-login"), document.getElementById("panel-login")],
        register: [document.getElementById("tab-register"), document.getElementById("panel-register")]
    };

    function showMessage(text) {
        message.textContent = text;
        message.className = "form-message" + (text ? " error" : "");
    }

    function selectTab(name, focusTab) {
        for (const [key, [tab, panel]] of Object.entries(tabs)) {
            const selected = key === name;
            tab.setAttribute("aria-selected", String(selected));
            tab.tabIndex = selected ? 0 : -1;
            panel.hidden = !selected;
        }
        showMessage("");
        if (focusTab) {
            tabs[name][0].focus();
        } else {
            tabs[name][1].querySelector("input").focus();
        }
    }

    Object.entries(tabs).forEach(([name, [tab]]) => {
        tab.addEventListener("click", () => selectTab(name, false));
        tab.addEventListener("keydown", event => {
            if (event.key === "ArrowRight" || event.key === "ArrowLeft") {
                event.preventDefault();
                selectTab(name === "login" ? "register" : "login", true);
            }
        });
    });

    document.querySelectorAll("[data-toggle-password]").forEach(button => {
        button.addEventListener("click", () => {
            const input = document.getElementById(button.dataset.togglePassword);
            const show = input.type === "password";
            input.type = show ? "text" : "password";
            button.textContent = show ? "Hide" : "Show";
            button.setAttribute("aria-pressed", String(show));
        });
    });

    function describe(input) {
        const label = document.querySelector("label[for='" + input.id + "']");
        const name = label ? label.textContent.trim() : "This field";
        if (input.validity.valueMissing) {
            return name + " is required.";
        }
        if (input.validity.typeMismatch) {
            return "Enter a valid email address.";
        }
        if (input.validity.patternMismatch) {
            return "Username must be 3\u201380 letters, numbers, or underscores.";
        }
        if (input.validity.tooShort) {
            return name + " must be at least " + input.minLength + " characters.";
        }
        return input.validationMessage;
    }

    function validate(form) {
        let firstInvalid = null;
        form.querySelectorAll("input").forEach(input => {
            const valid = input.checkValidity();
            input.setAttribute("aria-invalid", valid ? "false" : "true");
            if (!valid && !firstInvalid) {
                firstInvalid = input;
            }
        });
        if (firstInvalid) {
            showMessage(describe(firstInvalid));
            firstInvalid.focus();
            return false;
        }
        return true;
    }

    function bind(formId, path) {
        const form = document.getElementById(formId);
        const button = form.querySelector("button[type='submit']");
        form.addEventListener("submit", async event => {
            event.preventDefault();
            if (!validate(form)) {
                return;
            }
            button.disabled = true;
            showMessage("");
            try {
                await App.api(path, { method: "POST", body: Object.fromEntries(new FormData(form)), redirectOn401: false });
                window.location.assign(App.base + "/");
            } catch (error) {
                showMessage(error.message);
            } finally {
                button.disabled = false;
            }
        });
    }

    bind("login-form", "/auth/login");
    bind("register-form", "/auth/register");

    App.api("/api/session", { redirectOn401: false })
        .then(() => window.location.replace(App.base + "/"))
        .catch(() => document.getElementById("login-username").focus());
})();
