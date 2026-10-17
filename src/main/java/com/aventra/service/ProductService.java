package com.aventra.service;

import com.aventra.entity.Product;
import com.aventra.repository.WishlistItemRepository;
import com.aventra.repository.CartItemRepository;
import com.aventra.repository.OrderItemRepository;
import org.springframework.transaction.annotation.Transactional;
import com.aventra.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import java.util.List;
import org.springframework.data.domain.Sort;

@Service
public class ProductService {

    @Autowired
    private ProductRepository productRepository;
        @Autowired
    private WishlistItemRepository wishlistItemRepository;

    @Autowired
    private CartItemRepository cartItemRepository;

    @Autowired
    private OrderItemRepository orderItemRepository;

    // Get All Products
    public List<Product> getAllProducts() {
        return productRepository.findAll();
    }

    // Add Product
    public Product saveProduct(Product product) {
        return productRepository.save(product);
    }

    // Update Product
    public Product updateProduct(Long id, Product updatedProduct) {

        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found"));

        product.setName(updatedProduct.getName());
        product.setCategory(updatedProduct.getCategory());
        product.setPrice(updatedProduct.getPrice());
        product.setQuantity(updatedProduct.getQuantity());
        product.setImageUrl(updatedProduct.getImageUrl());

        return productRepository.save(product);
    }
    // Delete Product
    @Transactional
    public void deleteProduct(Long id) {

        if (orderItemRepository.existsByProduct_Id(id)) {
            throw new RuntimeException(
                "This product is part of existing orders and cannot be deleted. "
                + "Set its quantity to 0 instead."
            );
        }

        wishlistItemRepository.deleteByProduct_Id(id);
        cartItemRepository.deleteByProduct_Id(id);
        productRepository.deleteById(id);
    }

    // Search Products
    public List<Product> searchProducts(String keyword) {
        return productRepository.findByNameContainingIgnoreCase(keyword);
    }

    // Get Products by Category
    public List<Product> getProductsByCategory(String category) {
        return productRepository.findByCategoryIgnoreCase(category);
    }
    public Page<Product> getProductsWithPagination(int page, int size) {

        Pageable pageable = PageRequest.of(page, size);

        return productRepository.findAll(pageable);
    }
    public List<Product> getAllProductsSorted(String field) {
        return productRepository.findAll(Sort.by(field));
    }
    public Page<Product> getProductsWithPaginationAndSorting(
            int page,
            int size,
            String field,
            String direction) {

        Sort sort = direction.equalsIgnoreCase("asc")
                ? Sort.by(field).ascending()
                : Sort.by(field).descending();

        Pageable pageable = PageRequest.of(page, size, sort);

        return productRepository.findAll(pageable);
    }
}
