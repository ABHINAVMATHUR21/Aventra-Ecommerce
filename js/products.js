// ===============================
// Aventra - products.js  (product.html)
// ===============================
// Confirmed from ProductController: GET /products (all), GET /products/category/{category},
// GET /products/search?keyword=xxx

const grid = document.getElementById("product-grid");
const filterBar = document.getElementById("filter-bar");
const searchInput = document.getElementById("search-input");
const searchBtn = document.getElementById("search-btn");

let allProducts = [];
let activeCategory = "all";

function renderProducts(products) {
    if (!products.length) {
        grid.innerHTML = "<p>Koi product nahi mila.</p>";
        return;
    }

    grid.innerHTML = products.map((product) => {
        const inStock = product.inStock && product.quantity > 0;
        return `
        <div class="product-card" style="position:relative;">
            <button class="wishlist-btn" data-id="${product.id}" title="Add to wishlist"
                style="position:absolute;top:14px;right:14px;background:#fff;border:none;width:34px;height:34px;border-radius:50%;box-shadow:var(--shadow);cursor:pointer;">
                <i class="fa-regular fa-heart"></i>
            </button>
            <img src="${getProductImage(product)}" alt="${product.name}">
            <h3>${product.name}</h3>
            <p>₹${product.price}${product.unit ? " / " + product.unit : ""}</p>
            <small style="display:block;margin:-10px 0 12px;color:${inStock ? "#15803d" : "#b91c1c"};">${inStock ? `${product.quantity} in stock` : "Out of stock"}</small>
                        ${inStock ? `
            <div class="card-qty" data-max="${product.quantity}">
                <button type="button" class="card-qty-btn" data-action="minus">-</button>
                <span class="card-qty-value">1</span>
                <button type="button" class="card-qty-btn" data-action="plus">+</button>
            </div>` : ""}
            <button class="add-to-cart-btn" data-id="${product.id}" ${inStock ? "" : "disabled"}>${inStock ? "Add To Cart" : "Unavailable"}</button>
        </div>
    `;
    }).join("");
}

function applyFilters() {
    const query = searchInput.value.trim().toLowerCase();

    let filtered = allProducts;

    if (activeCategory !== "all") {
        filtered = filtered.filter(
            (p) => (p.category || "").toLowerCase() === activeCategory
        );
    }

    if (query) {
        filtered = filtered.filter((p) => p.name.toLowerCase().includes(query));
    }

    renderProducts(filtered);
}

async function loadAllProducts() {
    try {
        allProducts = await apiFetch("/products");

        const params = new URLSearchParams(window.location.search);
        const searchQuery = params.get("search");
        const categoryQuery = params.get("category");

        if (searchQuery) {
            searchInput.value = searchQuery;
        }

        if (categoryQuery) {
            const matchingChip = filterBar.querySelector(`.filter-chip[data-category="${categoryQuery}"]`);
            if (matchingChip) {
                filterBar.querySelectorAll(".filter-chip").forEach((c) => c.classList.remove("active"));
                matchingChip.classList.add("active");
                activeCategory = categoryQuery;
            }
        }

        applyFilters();
    } catch (error) {
        grid.innerHTML = "<p>Products load nahi ho paaye. Backend check karein.</p>";
        console.error(error);
    }
}

loadAllProducts();

// ---------- Category filter chips ----------

filterBar.addEventListener("click", (e) => {
    const chip = e.target.closest(".filter-chip");
    if (!chip) return;

    filterBar.querySelectorAll(".filter-chip").forEach((c) => c.classList.remove("active"));
    chip.classList.add("active");

    activeCategory = chip.dataset.category;
    applyFilters();
});

// ---------- Search ----------

searchBtn.addEventListener("click", applyFilters);
searchInput.addEventListener("keyup", (e) => {
    if (e.key === "Enter") applyFilters();
});

// ---------- Add to cart ----------

grid.addEventListener("click", (event) => {
    if (event.target.closest(".add-to-cart-btn")) {
        const id = event.target.closest(".add-to-cart-btn").dataset.id;
        addToCartServer(id, 1);
        return;
    }

    if (event.target.closest(".wishlist-btn")) {
        const id = event.target.closest(".wishlist-btn").dataset.id;
        addToWishlistServer(id);
    }
});
