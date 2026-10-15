package com.aventra.controller;

import com.aventra.entity.WishlistItem;
import com.aventra.service.WishlistService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/wishlist")
@CrossOrigin(origins = "*")
public class WishlistController {

    @Autowired
    private WishlistService wishlistService;

    @PostMapping("/add")
    public WishlistItem addToWishlist(@RequestParam Long productId) {
        return wishlistService.addToWishlist(productId);
    }

    @GetMapping
    public List<WishlistItem> getUserWishlist() {
        return wishlistService.getUserWishlist();
    }

    @DeleteMapping("/{wishlistItemId}")
    public String removeFromWishlist(@PathVariable Long wishlistItemId) {
        wishlistService.removeFromWishlist(wishlistItemId);
        return "Removed from wishlist";
    }
}