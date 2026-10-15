package com.aventra.service;

import com.aventra.entity.Product;
import com.aventra.entity.User;
import com.aventra.entity.WishlistItem;
import com.aventra.repository.ProductRepository;
import com.aventra.repository.WishlistItemRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class WishlistService {

    private final WishlistItemRepository wishlistItemRepository;
    private final ProductRepository productRepository;
    private final CurrentUserService currentUserService;

    public WishlistService(
            WishlistItemRepository wishlistItemRepository,
            ProductRepository productRepository,
            CurrentUserService currentUserService
    ) {
        this.wishlistItemRepository = wishlistItemRepository;
        this.productRepository = productRepository;
        this.currentUserService = currentUserService;
    }

    public WishlistItem addToWishlist(Long productId) {

        User user = currentUserService.getCurrentUser();

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found"));

        WishlistItem wishlistItem = wishlistItemRepository
                .findByUser_IdAndProduct_Id(user.getId(), productId)
                .orElse(new WishlistItem());

        wishlistItem.setUser(user);
        wishlistItem.setProduct(product);

        return wishlistItemRepository.save(wishlistItem);
    }

    public List<WishlistItem> getUserWishlist() {

        User user = currentUserService.getCurrentUser();

        return wishlistItemRepository.findByUser_Id(user.getId());
    }

    public void removeFromWishlist(Long wishlistItemId) {

        User user = currentUserService.getCurrentUser();

        WishlistItem wishlistItem = wishlistItemRepository
                .findById(wishlistItemId)
                .orElseThrow(() ->
                        new RuntimeException("Wishlist item not found")
                );

        if (!wishlistItem.getUser().getId().equals(user.getId())) {
            throw new RuntimeException(
                    "You cannot remove another user's wishlist item"
            );
        }

        wishlistItemRepository.delete(wishlistItem);
    }
}
