const ORDER_STATUSES = ["CONFIRMED", "PACKED", "SHIPPED", "OUT_FOR_DELIVERY", "DELIVERED", "CANCELLED"];
let dashboardProducts = [];

function money(value) {
    return new Intl.NumberFormat("en-IN", {
        style: "currency", currency: "INR", maximumFractionDigits: 0
    }).format(value || 0);
}

function prettyStatus(status) {
    return (status || "CONFIRMED").replaceAll("_", " ").toLowerCase()
        .replace(/\b\w/g, letter => letter.toUpperCase());
}

function displayDate(value) {
    return value ? new Date(value).toLocaleString("en-IN", { dateStyle: "medium", timeStyle: "short" }) : "—";
}

function orderStatusSelect(order) {
    return `<select class="status-select" data-order-id="${order.id}">${ORDER_STATUSES.map(status =>
        `<option value="${status}" ${status === order.status ? "selected" : ""}>${prettyStatus(status)}</option>`
    ).join("")}</select>`;
}

function renderOrders(orders) {
    const body = document.getElementById("orders-table-body");
    if (!orders.length) {
        body.innerHTML = '<tr><td colspan="7">No orders yet.</td></tr>';
        return;
    }
    body.innerHTML = orders.map(order => `
        <tr>
            <td>#${order.id}</td>
            <td>${order.customerName || "Customer"}<small>${order.customerEmail || ""}</small></td>
            <td>${order.deliveryCity || "—"}</td>
            <td>${money(order.totalAmount)}</td>
            <td>${order.paymentMethod || "—"}<small>${order.paymentStatus || ""}</small></td>
            <td>${orderStatusSelect(order)}</td>
            <td>${displayDate(order.createdAt)}</td>
        </tr>`).join("");
}

function renderProducts(products) {
    const body = document.getElementById("products-table-body");
    if (!products.length) {
        body.innerHTML = '<tr><td colspan="5">No products found.</td></tr>';
        return;
    }
    body.innerHTML = products.map(product => `
        <tr>
            <td>${product.name}</td>
            <td>${product.category}</td>
            <td>${money(product.price)}</td>
            <td class="${product.quantity <= 5 ? "low-stock" : ""}">${product.quantity}</td>
            <td><button class="table-action" data-edit-product="${product.id}" type="button">Edit</button><button class="table-action delete" data-delete-product="${product.id}" type="button">Delete</button></td>
        </tr>`).join("");
}

async function loadDashboard() {
    const dashboard = await apiFetch("/admin/dashboard");
    document.getElementById("stat-users").textContent = dashboard.totalUsers;
    document.getElementById("stat-products").textContent = dashboard.totalProducts;
    document.getElementById("stat-orders").textContent = dashboard.totalOrders;
    document.getElementById("stat-revenue").textContent = money(dashboard.totalRevenue);
    document.getElementById("stat-processing").textContent = dashboard.ordersToProcess;
    document.getElementById("stat-low-stock").textContent = dashboard.lowStockProducts;
    renderOrders(dashboard.recentOrders || []);
}

async function loadProducts() {
    dashboardProducts = await apiFetch("/products");
    renderProducts(dashboardProducts);
}

function clearProductForm() {
    document.getElementById("product-form").reset();
    document.getElementById("product-id").value = "";
    document.getElementById("save-product").textContent = "Save product";
}

function editProduct(id) {
    const product = dashboardProducts.find(item => item.id === Number(id));
    if (!product) return;
    document.getElementById("product-id").value = product.id;
    document.getElementById("product-name").value = product.name;
    document.getElementById("product-category").value = product.category;
    document.getElementById("product-price").value = product.price;
    document.getElementById("product-quantity").value = product.quantity;
    document.getElementById("product-image-url").value = product.imageUrl;
    document.getElementById("save-product").textContent = "Update product";
    document.getElementById("product-name").focus();
}

async function refreshAll() {
    try {
        await Promise.all([loadDashboard(), loadProducts()]);
    } catch (error) {
        showToast(error.message || "Dashboard refresh nahi ho paaya");
    }
}

async function initialiseDashboard() {
    const accessPanel = document.getElementById("access-panel");
    const dashboard = document.getElementById("dashboard-content");
    try {
        const profile = await apiFetch("/users/profile");
        if ((profile.role || "").toUpperCase() !== "ADMIN") {
            throw new Error("This account does not have administrator access.");
        }
        document.getElementById("admin-name").textContent = profile.name || "Administrator";
        accessPanel.hidden = true;
        dashboard.hidden = false;
        await refreshAll();
    } catch (error) {
        accessPanel.innerHTML = `<i class="fa-solid fa-lock"></i><h1>Administrator access required</h1><p>${error.message || "Please sign in with an administrator account."}</p>`;
    }
}

document.getElementById("refresh-dashboard").addEventListener("click", refreshAll);
document.getElementById("clear-product").addEventListener("click", clearProductForm);

document.getElementById("orders-table-body").addEventListener("change", async event => {
    const select = event.target.closest(".status-select");
    if (!select) return;
    try {
        select.disabled = true;
        await apiFetch(`/admin/orders/${select.dataset.orderId}/status`, {
            method: "PATCH", body: JSON.stringify({ status: select.value })
        });
        showToast("Order status updated");
        await loadDashboard();
    } catch (error) {
        showToast(error.message || "Status update nahi ho paaya");
        await loadDashboard();
    } finally {
        select.disabled = false;
    }
});

document.getElementById("product-form").addEventListener("submit", async event => {
    event.preventDefault();
    const id = document.getElementById("product-id").value;
    const product = {
        name: document.getElementById("product-name").value.trim(),
        category: document.getElementById("product-category").value.trim(),
        price: Number(document.getElementById("product-price").value),
        quantity: Number(document.getElementById("product-quantity").value),
        imageUrl: document.getElementById("product-image-url").value.trim()
    };
    try {
        await apiFetch(id ? `/products/${id}` : "/products", {
            method: id ? "PUT" : "POST", body: JSON.stringify(product)
        });
        showToast(id ? "Product updated" : "Product added");
        clearProductForm();
        await refreshAll();
    } catch (error) {
        showToast(error.message || "Product save nahi ho paaya");
    }
});

document.getElementById("products-table-body").addEventListener("click", async event => {
    const edit = event.target.closest("[data-edit-product]");
    if (edit) { editProduct(edit.dataset.editProduct); return; }
    const remove = event.target.closest("[data-delete-product]");
    if (!remove || !confirm("Delete this product?")) return;
    try {
        await apiFetch(`/products/${remove.dataset.deleteProduct}`, { method: "DELETE" });
        showToast("Product deleted");
        await refreshAll();
    } catch (error) {
        showToast(error.message || "Product delete nahi ho paaya");
    }
});

initialiseDashboard();
