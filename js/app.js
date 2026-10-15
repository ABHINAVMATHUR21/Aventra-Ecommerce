// ===============================
// Aventra - app.js
// Common utilities (sab pages me use hote hain) + Home page logic
// ===============================

const API_BASE = "http://localhost:8080";

// ---------- TOAST ----------

function showToast(message) {
    let toast = document.getElementById("toast");

    if (!toast) {
        toast = document.createElement("div");
        toast.id = "toast";
        document.body.appendChild(toast);
    }

    toast.innerText = message;
    toast.className = "show-toast";

    setTimeout(() => {
        toast.className = "";
    }, 2500);
}

// ---------- AUTH HELPERS ----------

function getToken() {
    return localStorage.getItem("token");
}

function isLoggedIn() {
    return !!getToken();
}

function logout() {
    localStorage.removeItem("token");
    localStorage.removeItem("userName");
    window.location.href = "login.html";
}

// Protected pages (cart/checkout/orders/profile) is helper ko call karein.
function requireLogin() {
    if (!isLoggedIn()) {
        showToast("Please login first");
        setTimeout(() => (window.location.href = "login.html"), 800);
        return false;
    }
    return true;
}

// Backend ko authenticated request bhejne ke liye wrapper.
async function apiFetch(path, options = {}) {
    const headers = {
        "Content-Type": "application/json",
        ...(options.headers || {})
    };

    const token = getToken();
    if (token) {
        headers["Authorization"] = `Bearer ${token}`;
    }

    const response = await fetch(`${API_BASE}${path}`, { ...options, headers });
    const data = await response.json().catch(() => ({}));

    if (!response.ok) {
        throw new Error(data.message || "Something went wrong");
    }

    return data;
}

// ---------- CART HELPERS (backend-based via CartController) ----------
// NOTE: CartController uses @RequestParam (query params), NOT JSON body.
// GET /cart -> List<CartItem>. Rendering assumes each CartItem has
// { id, quantity, product: { id, name, price, ... } } — verify against
// your actual CartItem.java once you share it.

async function addToCartServer(productId, quantity = 1) {
    if (!requireLogin()) return;

    try {
        await apiFetch(`/cart/add?productId=${productId}&quantity=${quantity}`, {
            method: "POST"
        });
        showToast("Cart me add ho gaya!");
        updateCartCount();
    } catch (error) {
        showToast(error.message || "Cart me add nahi ho paaya");
    }
}

async function updateCartCount() {
    const counters = document.querySelectorAll(".cart-count");

    if (!isLoggedIn()) {
        counters.forEach((el) => (el.innerText = "0"));
        return;
    }

    try {
        const cart = await apiFetch("/cart");
        const totalQty = cart.reduce((sum, item) => sum + (item.quantity || 0), 0);
        counters.forEach((el) => (el.innerText = totalQty));
    } catch (error) {
        counters.forEach((el) => (el.innerText = "0"));
    }
}

async function addToWishlistServer(productId) {
    if (!requireLogin()) return;

    try {
        await apiFetch(`/wishlist/add?productId=${productId}`, {
            method: "POST"
        });
        showToast("Wishlist me add ho gaya!");
    } catch (error) {
        showToast(error.message || "Wishlist me add nahi ho paaya");
    }
}

// ---------- Product image fallback ----------

function getProductImage(product) {
    const images = {
        apple: "images/Apple.avif",
        milk: "images/Amul MIlk.avif",
        rice: "images/Fresh Vegetable.avif",
        aalo: "images/Fresh Vegetable.avif"
    };

    return (
        product.imageUrl ||
        product.image ||
        images[(product.name || "").toLowerCase()] ||
        "images/Fresh Vegetable.avif"
    );
}

// ---------- Update header (user icon + cart count) on every page ----------

document.addEventListener("DOMContentLoaded", () => {
    updateCartCount();

    const userIconLink = document.getElementById("user-icon-link");
    if (userIconLink && !isLoggedIn()) {
        userIconLink.setAttribute("href", "login.html");
    }

    addAdminNavigation();
});

// Administrator ko hi dashboard link dikhaya jata hai. Backend endpoint bhi
// ADMIN role se protected hai, isliye link ko manually open karna access nahi deta.
async function addAdminNavigation() {
    const navigation = document.querySelector(".nav-links");
    if (!navigation || !isLoggedIn() || navigation.querySelector("[data-admin-link]")) {
        return;
    }

    try {
        const profile = await apiFetch("/users/profile");
        if ((profile.role || "").toUpperCase() !== "ADMIN") return;

        const item = document.createElement("li");
        item.dataset.adminLink = "true";
        item.innerHTML = '<a href="admin.html">Admin</a>';
        navigation.appendChild(item);
    } catch (error) {
        // A normal customer page should continue working if profile is unavailable.
        console.warn("Admin navigation unavailable", error);
    }
}

// ===============================
// HOME PAGE ONLY (index.html)
// ===============================

const productGrid = document.getElementById("featured-product-grid");

async function loadFeaturedProducts() {
    if (!productGrid) return;

    try {
        const products = await apiFetch("/products");

        productGrid.innerHTML = products.slice(0, 8).map((product) => {
            const inStock = product.inStock && product.quantity > 0;
            return `
            <div class="product-card">
                <img src="${getProductImage(product)}" alt="${product.name}">
                <h3>${product.name}</h3>
                <p>₹${product.price}</p>
                <small style="display:block;margin:-10px 0 12px;color:${inStock ? "#15803d" : "#b91c1c"};">${inStock ? `${product.quantity} in stock` : "Out of stock"}</small>
                <button class="add-to-cart-btn" data-id="${product.id}" ${inStock ? "" : "disabled"}>${inStock ? "Add To Cart" : "Unavailable"}</button>
            </div>
        `;
        }).join("");

        productGrid.dataset.products = JSON.stringify(products);

    } catch (error) {
        productGrid.innerHTML = "<p>Products load nahi ho paaye. Backend check karein.</p>";
        console.error(error);
    }
}

if (productGrid) {
    loadFeaturedProducts();

    productGrid.addEventListener("click", (event) => {
        if (!event.target.classList.contains("add-to-cart-btn")) return;

        const id = event.target.dataset.id;
        addToCartServer(id, 1);
    });
}

// ---------- Navbar Search (redirects to product.html) ----------

const navSearchInput = document.querySelector(".search-box input");
const navSearchBtn = document.querySelector(".search-box button");

function runNavSearch() {
    const query = navSearchInput.value.trim();
    if (query) {
        window.location.href = `product.html?search=${encodeURIComponent(query)}`;
    } else {
        window.location.href = "product.html";
    }
}

if (navSearchBtn) {
    navSearchBtn.addEventListener("click", runNavSearch);
}

if (navSearchInput) {
    navSearchInput.addEventListener("keyup", (e) => {
        if (e.key === "Enter") runNavSearch();
    });
}

// ---------- Newsletter Subscription ----------

const subscribeButton = document.querySelector(".newsletter button");
const emailInput = document.querySelector(".newsletter input");

if (subscribeButton) {
    subscribeButton.addEventListener("click", () => {
        const email = emailInput.value.trim();

        if (email === "") {
            showToast("Please enter your email.");
            return;
        }

        showToast("Thank you for subscribing!");
        emailInput.value = "";
    });
}

// ---------- Smooth Scroll ----------

document.querySelectorAll('a[href^="#"]').forEach((anchor) => {
    anchor.addEventListener("click", function (e) {
        const target = document.querySelector(this.getAttribute("href"));
        if (target) {
            e.preventDefault();
            target.scrollIntoView({ behavior: "smooth" });
        }
    });
});

// ---------- Sticky Navbar Shadow ----------

window.addEventListener("scroll", () => {
    const header = document.querySelector("header");
    if (!header) return;

    header.style.boxShadow =
        window.scrollY > 20
            ? "0 5px 20px rgba(0,0,0,0.15)"
            : "0 2px 10px rgba(0,0,0,0.08)";
});

// ---------- Reveal Animation ----------

const observer = new IntersectionObserver(
    (entries) => {
        entries.forEach((entry) => {
            if (entry.isIntersecting) entry.target.classList.add("show");
        });
    },
    { threshold: 0.2 }
);

document.querySelectorAll(".category-card, .product-card, .feature-box").forEach((item) => {
    item.classList.add("hidden");
    observer.observe(item);
});

// ---------- Hero Button ----------

const shopButton = document.querySelector(".shop-btn");

if (shopButton) {
    shopButton.addEventListener("click", () => {
        window.location.href = "product.html";
    });
}

console.log("Aventra Loaded Successfully");
