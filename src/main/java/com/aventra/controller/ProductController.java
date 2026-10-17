package com.aventra.controller;
import org.springframework.data.domain.Page;
import com.aventra.entity.Product;
import com.aventra.service.ProductService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import org.springframework.web.bind.annotation.RequestParam;

@RestController
@CrossOrigin(origins = "*")
public class ProductController {

    @Autowired
    private ProductService productService;

    // Get All Products
    @GetMapping("/products")
    public List<Product> getAllProducts() {
        return productService.getAllProducts();
    }

    // Add Product
    @PostMapping("/products")
    public Product addProduct(@Valid @RequestBody Product product) {
        return productService.saveProduct(product);
    }

    // Update Product
    @PutMapping("/products/{id}")
    public Product updateProduct(@PathVariable Long id,
                                 @Valid @RequestBody Product product) {
        return productService.updateProduct(id, product);
    }

    // Delete Product
    @DeleteMapping("/products/{id}")
    public String deleteProduct(@PathVariable Long id) {
        productService.deleteProduct(id);
        return "Product deleted successfully!";
    }

    // Search Product by Name
    @GetMapping("/products/search")
    public List<Product> searchProducts(@RequestParam String keyword) {
        return productService.searchProducts(keyword);
    }

    // Get Products by Category
    @GetMapping("/products/category/{category}")
    public List<Product> getProductsByCategory(@PathVariable String category) {
        return productService.getProductsByCategory(category);
    }
    @GetMapping("/products/page")
    public Page<Product> getProductsWithPagination(
            @RequestParam int page,
            @RequestParam int size) {

        return productService.getProductsWithPagination(page, size);
    }
    // Pagination + Sorting
    @GetMapping("/products/filter")
    public Page<Product> getProducts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int size,
            @RequestParam(defaultValue = "id") String field,
            @RequestParam(defaultValue = "asc") String direction) {

        return productService.getProductsWithPaginationAndSorting(
                page, size, field, direction);
    }
}