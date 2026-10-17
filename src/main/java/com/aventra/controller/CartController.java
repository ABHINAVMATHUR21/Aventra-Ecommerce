package com.aventra.controller;

import com.aventra.entity.CartItem;
import com.aventra.service.CartService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/cart")
@CrossOrigin(origins = "*")
public class CartController {

    @Autowired
    private CartService cartService;

    @PostMapping("/add")
    public CartItem addToCart(
            @RequestParam Long productId,
            @RequestParam(defaultValue = "1") Integer quantity
    ) {
        return cartService.addToCart(productId, quantity);
    }

    @GetMapping
    public List<CartItem> getUserCart() {
        return cartService.getUserCart();
    }

    @PutMapping("/{cartItemId}")
    public CartItem updateCartQuantity(
            @PathVariable Long cartItemId,
            @RequestParam Integer quantity
    ) {
        return cartService.updateCartQuantity(cartItemId, quantity);
    }

    @DeleteMapping("/{cartItemId}")
    public String removeCartItem(@PathVariable Long cartItemId) {

        cartService.removeCartItem(cartItemId);

        return "Product removed from cart";
    }
}