package com.aventra.repository;

import com.aventra.entity.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CartItemRepository extends JpaRepository<CartItem, Long> {

    // Same user ne same product pehle se cart me add kiya hai ya nahi
    Optional<CartItem> findByUser_IdAndProduct_Id(
            Long userId,
            Long productId
    );

    // Ek particular user ke saare cart items
    List<CartItem> findByUser_Id(Long userId);
        void deleteByProduct_Id(Long productId);
}
