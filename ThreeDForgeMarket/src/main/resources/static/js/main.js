document.addEventListener("DOMContentLoaded", function () {
    var toggle = document.querySelector(".nav-toggle");
    var nav = document.querySelector("#primary-nav");

    if (toggle && nav) {
        toggle.addEventListener("click", function () {
            var expanded = toggle.getAttribute("aria-expanded") === "true";
            toggle.setAttribute("aria-expanded", String(!expanded));
            nav.classList.toggle("is-open");
        });
    }

    var filterToggle = document.querySelector(".btn-filter");
    var filterPanel = document.querySelector("#catalog-filter-panel");

    if (filterToggle && filterPanel) {
        filterToggle.addEventListener("click", function () {
            var expanded = filterToggle.getAttribute("aria-expanded") === "true";
            filterToggle.setAttribute("aria-expanded", String(!expanded));
            filterPanel.classList.toggle("is-collapsed", expanded);
            filterToggle.classList.toggle("is-active", !expanded);
        });
    }

    document.querySelectorAll("form[data-confirm]").forEach(function (form) {
        form.addEventListener("submit", function (event) {
            var message = form.getAttribute("data-confirm") || "Confirm this action?";
            if (!window.confirm(message)) {
                event.preventDefault();
            }
        });
    });

    var orderForm = document.querySelector("form[data-unit-price]");
    if (!orderForm) {
        return;
    }

    var quantity = orderForm.querySelector("[name='quantity']");
    var total = document.querySelector("#order-total");
    var unitPrice = Number(orderForm.getAttribute("data-unit-price"));
    var euroFormatter = new Intl.NumberFormat("en-IE", {
        style: "currency",
        currency: "EUR",
        currencyDisplay: "symbol"
    });

    function updateTotal() {
        var count = Number(quantity && quantity.value);

        if (!total || !Number.isFinite(unitPrice) || !Number.isFinite(count) || count < 1) {
            return;
        }

        total.textContent = euroFormatter.format(unitPrice * count);
    }

    if (quantity) {
        quantity.addEventListener("input", updateTotal);
        quantity.addEventListener("change", updateTotal);
        updateTotal();
    }
});
