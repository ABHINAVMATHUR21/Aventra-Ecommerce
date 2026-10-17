// ===============================
// Aventra - cart.js (cart.html)
// ===============================
// Backend-based cart (CartController). Assumes each CartItem looks like:
// { id, quantity, product: { id, name, price, image? } }
// Verify field names against your CartItem.java / Product.java.

if (!requireLogin()) {
    // requireLogin already redirect kar dega login.html par
}

const cartContainer = document.getElementById("cart-container");
const DELIVERY_FEE = 25;

async function loadCart() {
    try {
        const cart = await apiFetch("/cart");
        renderCart(cart);
    } catch (error) {
        cartContainer.innerHTML = "<p>Cart load nahi ho paaya. Backend check karein.</p>";
        console.error(error);
    }
}

function renderCart(cart) {
    if (!cart.length) {
        cartContainer.innerHTML = `
            <div class="empty-cart">
                <p>Aapka cart khaali hai.</p>
                <a href="product.html">Shop Now &rarr;</a>
            </div>
        `;
        return;
    }

    const rows = cart.map((item) => {
        const product = item.product || {};
        const canIncrease = item.quantity < product.quantity;
        return `
            <tr data-cart-item-id="${item.id}">
                <td>
                    <div class="cart-item-name">
                        <img src="${getProductImage(product)}" alt="${product.name}">
                        <span>${product.name}</span>
                    </div>
                </td>
                <td>₹${product.price}</td>
                <td>
                    <div class="qty-control">
                        <button class="qty-btn" data-action="decrease">-</button>
                        <span>${item.quantity}</span>
                        <button class="qty-btn" data-action="increase" ${canIncrease ? "" : "disabled"}>+</button>
                    </div>
                    <small style="display:block;margin-top:5px;color:#667085;">${product.quantity} available</small>
                </td>
                <td>₹${product.price * item.quantity}</td>
                <td><button class="remove-btn" title="Remove"><i class="fa-solid fa-trash"></i></button></td>
            </tr>
        `;
    }).join("");

    const subtotal = cart.reduce((sum, item) => sum + (item.product?.price || 0) * item.quantity, 0);
    const total = subtotal + (subtotal > 0 ? DELIVERY_FEE : 0);

    cartContainer.innerHTML = `
        <table class="cart-table">
            <thead>
                <tr>
                    <th>Product</th>
                    <th>Price</th>
                    <th>Quantity</th>
                    <th>Total</th>
                    <th></th>
                </tr>
            </thead>
            <tbody>${rows}</tbody>
        </table>

        <div class="cart-summary">
            <div class="summary-row"><span>Subtotal</span><span>₹${subtotal}</span></div>
            <div class="summary-row"><span>Delivery Fee</span><span>₹${DELIVERY_FEE}</span></div>
            <div class="summary-row total"><span>Total</span><span>₹${total}</span></div>
            <button class="checkout-btn" id="checkout-btn">Proceed to Checkout</button>
        </div>
    `;

    document.getElementById("checkout-btn").addEventListener("click", () => {
        window.location.href = "checkout.html";
    });
}

loadCart();

cartContainer.addEventListener("click", async (e) => {
    const row = e.target.closest("tr[data-cart-item-id]");
    if (!row) return;

    const cartItemId = row.dataset.cartItemId;

    if (e.target.closest(".qty-btn")) {
        const action = e.target.closest(".qty-btn").dataset.action;
        const qtySpan = row.querySelector(".qty-control span");
        let qty = parseInt(qtySpan.innerText, 10);

        qty = action === "increase" ? qty + 1 : Math.max(1, qty - 1);

        try {
            await apiFetch(`/cart/${cartItemId}?quantity=${qty}`, { method: "PUT" });
            await loadCart();
            updateCartCount();
        } catch (error) {
            showToast(error.message || "Quantity update nahi ho payi");
        }
    }

    if (e.target.closest(".remove-btn")) {
        try {
            await apiFetch(`/cart/${cartItemId}`, { method: "DELETE" });
            showToast("Item cart se remove ho gaya");
            await loadCart();
            updateCartCount();
        } catch (error) {
            showToast(error.message || "Remove nahi ho paaya");
        }
    }
});
