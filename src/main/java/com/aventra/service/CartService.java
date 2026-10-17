package com.aventra.service;

import com.aventra.entity.CartItem;
import com.aventra.entity.Product;
import com.aventra.entity.User;
import com.aventra.repository.CartItemRepository;
import com.aventra.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CartService {

    @Autowired
    private CartItemRepository cartItemRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CurrentUserService currentUserService;

    public CartItem addToCart(Long productId, Integer quantity) {

        if (quantity == null || quantity <= 0) {
            throw new RuntimeException("Quantity must be greater than 0");
        }

        User user = currentUserService.getCurrentUser();

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found"));

        CartItem cartItem = cartItemRepository
                .findByUser_IdAndProduct_Id(user.getId(), productId)
                .orElse(new CartItem());

        int currentQuantity = cartItem.getQuantity() == null
                ? 0
                : cartItem.getQuantity();

        int newQuantity = currentQuantity + quantity;

        if (newQuantity > product.getQuantity()) {
            throw new RuntimeException(
                    "Requested quantity is not available in stock"
            );
        }

        cartItem.setUser(user);
        cartItem.setProduct(product);
        cartItem.setQuantity(newQuantity);

        return cartItemRepository.save(cartItem);
    }

    public List<CartItem> getUserCart() {

        User user = currentUserService.getCurrentUser();

        return cartItemRepository.findByUser_Id(user.getId());
    }

    public CartItem updateCartQuantity(
            Long cartItemId,
            Integer quantity
    ) {
        if (quantity == null || quantity <= 0) {
            throw new RuntimeException("Quantity must be greater than 0");
        }

        User user = currentUserService.getCurrentUser();

        CartItem cartItem = cartItemRepository.findById(cartItemId)
                .orElseThrow(() -> new RuntimeException("Cart item not found"));

        verifyCartOwnership(cartItem, user);

        if (quantity > cartItem.getProduct().getQuantity()) {
            throw new RuntimeException(
                    "Requested quantity is not available in stock"
            );
        }

        cartItem.setQuantity(quantity);

        return cartItemRepository.save(cartItem);
    }

    public void removeCartItem(Long cartItemId) {

        User user = currentUserService.getCurrentUser();

        CartItem cartItem = cartItemRepository.findById(cartItemId)
                .orElseThrow(() -> new RuntimeException("Cart item not found"));

        verifyCartOwnership(cartItem, user);

        cartItemRepository.delete(cartItem);
    }

    private void verifyCartOwnership(
            CartItem cartItem,
            User user
    ) {
        if (!cartItem.getUser().getId().equals(user.getId())) {
            throw new RuntimeException(
                    "You cannot access another user's cart"
            );
        }
    }
}